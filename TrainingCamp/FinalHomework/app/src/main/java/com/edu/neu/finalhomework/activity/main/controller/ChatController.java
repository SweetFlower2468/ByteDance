package com.edu.neu.finalhomework.activity.main.controller;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.activity.main.network.ArkClient;
import com.edu.neu.finalhomework.activity.main.network.Cancellable;
import com.edu.neu.finalhomework.config.ChatConfig;
import com.edu.neu.finalhomework.config.ModelConfig;
import com.edu.neu.finalhomework.domain.dao.MessageDao;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.edu.neu.finalhomework.domain.entity.Message;
import com.edu.neu.finalhomework.service.LlamaService;
import com.edu.neu.finalhomework.service.callback.SimpleCallback;
import com.edu.neu.finalhomework.service.callback.StreamCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天生成控制器（中间层）
 * 负责分发到本地 LlamaService 或网络 ArkClient，封装 Prompt 构建、上下文获取与响应解析。
 */
public class ChatController {

    private static final String TAG = "ChatController";
    private final MessageDao messageDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private Cancellable activeNetworkCancellable;
    private boolean isGenerating = false;

    // UI 更新回调
    public interface ChatGenerationListener {
        void onThinking(String delta);
        void onContent(String delta);
        void onComplete();
        void onError(String error);
    }

    public ChatController() {
        this.messageDao = App.getInstance().getDatabase().messageDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /** 停止当前生成 */
    public void stopGeneration() {
        if (!isGenerating) return;

        // 停止本地推理
        LlamaService.getInstance().cancelInference();

        // 停止网络请求
        if (activeNetworkCancellable != null) {
            activeNetworkCancellable.cancel();
            activeNetworkCancellable = null;
        }

        isGenerating = false;
    }

    /**
     * 发送聊天请求
     * @param apiPrompt 组装好的 Prompt（文本 + 附件描述）
     * @param isDeepThink 是否开启深度思考
     * @param aiMsg AI 占位消息
     * @param userMsg 用户消息（用于上下文过滤）
     * @param activeModel 当前模型
     * @param listener UI 回调
     */
    public void sendChatRequest(String apiPrompt, boolean isDeepThink, Message aiMsg, Message userMsg, LocalModel activeModel, ChatGenerationListener listener) {
        if (activeModel == null) {
            listener.onError("No active model selected");
            return;
        }

        isGenerating = true;

        if (activeModel.isLocal) {
            handleLocalRequest(apiPrompt, aiMsg, userMsg, activeModel, listener);
        } else {
            handleNetworkRequest(apiPrompt, isDeepThink, aiMsg, userMsg, listener);
        }
    }

    private void handleLocalRequest(String prompt, Message aiMsg, Message userMsg, LocalModel model, ChatGenerationListener listener) {
        LlamaService service = LlamaService.getInstance();

        // 为本地模型裁剪上下文
        List<Message> contextHistory = new ArrayList<>();
        List<Message> dbMessages = messageDao.getLatestMessagesBySession(aiMsg.sessionId, ChatConfig.MAX_LOCAL_CONTEXT_MESSAGES + 2);
        if (dbMessages != null) {
            Collections.sort(dbMessages, (m1, m2) -> Long.compare(m1.timestamp, m2.timestamp));
            for (Message m : dbMessages) {
                if (m.id == aiMsg.id) continue;
                if (userMsg != null && m.id == userMsg.id) continue;
                if ("user".equals(m.type) || "ai".equals(m.type)) {
                    contextHistory.add(m);
                }
            }
            // 仅保留最近 N 条消息
            if (contextHistory.size() > ChatConfig.MAX_LOCAL_CONTEXT_MESSAGES) {
                contextHistory = contextHistory.subList(contextHistory.size() - ChatConfig.MAX_LOCAL_CONTEXT_MESSAGES, contextHistory.size());
            }
        }

        // 基于上下文与最新提问构建 ChatML Prompt
        String chatPrompt = buildLocalChatPrompt(contextHistory, prompt, ChatConfig.MAX_LOCAL_PROMPT_CHARS);

        // 确保模型已加载
        if (!service.isModelLoaded() || (service.getCurrentModel() != null && service.getCurrentModel().id != model.id)) {
            // 可选：通知“加载中”
            service.loadModel(model, new SimpleCallback() {
                @Override
                public void onSuccess() {
                    doLocalInference(service, chatPrompt, aiMsg, listener);
                }

                @Override
                public void onFailure(String error) {
                    isGenerating = false;
                    listener.onError("模型加载失败: " + error);
                }
            });
        } else {
            doLocalInference(service, chatPrompt, aiMsg, listener);
        }
    }

    private void doLocalInference(LlamaService service, String chatPrompt, Message aiMsg, ChatGenerationListener listener) {
        // 深度思考：部分模型会自动输出 <think>，若需强制关闭可考虑负向提示；当前依赖解析处理

        service.infer(chatPrompt, new StreamCallback() {
            private boolean hasReceivedContent = false;
            private StringBuilder buffer = new StringBuilder();
            private boolean isThinking = false;
            private int generatedChars = 0;
            private boolean outputLimitReached = false;
            private boolean hardStoppedByMarker = false;

            @Override
            public void onDeepThink(String delta) {
                appendThinking(delta, listener);
            }

            @Override
            public void onChunk(String delta) {
                if (!hasReceivedContent) {
                    hasReceivedContent = true;
                }

                buffer.append(delta);
                String currentBuffer = buffer.toString();

                // 停止词过滤
                if (containsStopToken(currentBuffer)) {
                    int stopIndex = getStopTokenIndex(currentBuffer);
                    if (stopIndex != -1) {
                        service.cancelInference();
                        String validContent = currentBuffer.substring(0, stopIndex);
                        processContent(validContent, listener, true);
                        
                        // 若服务端保证 cancel 后触发 onComplete，则此处不重复调用；LlamaService 会在 finally 中回调
                        return; 
                    }
                }

                // 解析 <think> 标签
                String processed = processBufferForThinking(currentBuffer, listener);
                buffer.setLength(0);
                buffer.append(processed);

                // 达到输出上限则提前停止
                if (outputLimitReached) {
                    service.cancelInference();
                    return;
                }
            }

            @Override
            public void onComplete() {
                // 刷新剩余缓存
                if (buffer.length() > 0) {
                     processContent(buffer.toString(), listener, true);
                }
                isGenerating = false;
                listener.onComplete();
            }

            @Override
            public void onError(String message, Throwable t) {
                isGenerating = false;
                listener.onError(message);
            }

            // --- 深度思考解析辅助方法（来自 ChatActivity） ---

            private boolean containsStopToken(String s) {
                 return s.contains("<|im_end|>") || s.contains("<|im_start|>") || s.contains("<|endoftext|>");
            }
            
            private int getStopTokenIndex(String s) {
                if (s.contains("<|im_end|>")) return s.indexOf("<|im_end|>");
                if (s.contains("<|im_start|>")) return s.indexOf("<|im_start|>");
                if (s.contains("<|endoftext|>")) return s.indexOf("<|endoftext|>");
                return -1;
            }

            private String processBufferForThinking(String text, ChatGenerationListener listener) {
                int index = 0;
                while (index < text.length()) {
                    if (!isThinking) {
                        int startTagIndex = text.indexOf("<think>", index);
                        if (startTagIndex != -1) {
                            String contentPart = text.substring(index, startTagIndex);
                            appendContent(contentPart, listener);
                            isThinking = true;
                            index = startTagIndex + 7;
                        } else {
                            String remaining = text.substring(index);
                            if (isPartialTag(remaining)) return remaining;
                            appendContent(remaining, listener);
                            return "";
                        }
                    } else {
                        int endTagIndex = text.indexOf("</think>", index);
                        if (endTagIndex != -1) {
                            String thinkPart = text.substring(index, endTagIndex);
                            appendThinking(thinkPart, listener);
                            isThinking = false;
                            index = endTagIndex + 8;
                        } else {
                            String remaining = text.substring(index);
                            if (isPartialTag(remaining)) return remaining;
                            appendThinking(remaining, listener);
                            return "";
                        }
                    }
                }
                return "";
            }

            private boolean isPartialTag(String s) {
                if (s.isEmpty()) return false;
                if ("<think>".startsWith(s)) return true;
                if ("</think>".startsWith(s)) return true;
                if (s.startsWith("<|")) {
                     if ("<|im_end|>".startsWith(s)) return true;
                     if ("<|im_start|>".startsWith(s)) return true;
                     if ("<|endoftext|>".startsWith(s)) return true;
                     return true; 
                }
                if (s.equals("<")) return true;
                return false;
            }

            private void processContent(String text, ChatGenerationListener listener, boolean force) {
                if (outputLimitReached) return;

                if (isThinking) appendThinking(text, listener);
                else appendContent(text, listener);
            }

            private void appendContent(String text, ChatGenerationListener listener) {
                if (outputLimitReached || text.isEmpty()) return;
                String cleaned = stripStopMarkers(text);
                if (cleaned == null) {
                    outputLimitReached = true;
                    hardStoppedByMarker = true;
                    service.cancelInference();
                    return;
                }
                int remaining = ChatConfig.MAX_LOCAL_OUTPUT_CHARS - generatedChars;
                if (remaining <= 0) {
                    outputLimitReached = true;
                    service.cancelInference();
                    return;
                }
                String emit = cleaned.length() > remaining ? cleaned.substring(0, remaining) : cleaned;
                if (!emit.isEmpty()) {
                    listener.onContent(emit);
                    generatedChars += emit.length();
                }
                if (cleaned.length() > remaining) {
                    outputLimitReached = true;
                    service.cancelInference();
                }
            }

            private void appendThinking(String text, ChatGenerationListener listener) {
                if (outputLimitReached || text.isEmpty()) return;
                String cleaned = stripStopMarkers(text);
                if (cleaned == null) {
                    outputLimitReached = true;
                    hardStoppedByMarker = true;
                    service.cancelInference();
                    return;
                }
                int remaining = ChatConfig.MAX_LOCAL_OUTPUT_CHARS - generatedChars;
                if (remaining <= 0) {
                    outputLimitReached = true;
                    service.cancelInference();
                    return;
                }
                String emit = cleaned.length() > remaining ? cleaned.substring(0, remaining) : cleaned;
                if (!emit.isEmpty()) {
                    listener.onThinking(emit);
                    generatedChars += emit.length();
                }
                if (cleaned.length() > remaining) {
                    outputLimitReached = true;
                    service.cancelInference();
                }
            }

            // 若生成中出现 ChatML 控制标记则移除；若标记出现在开头则返回 null 表示需要停止
            private String stripStopMarkers(String text) {
                if (text == null || text.isEmpty()) return text;
                int idx = text.indexOf("<|im_start|>");
                if (idx == -1) idx = text.indexOf("<|im_end|>");
                if (idx == -1) idx = text.indexOf("<|endoftext|>");
                // 兼容形如 "<im_start" 的异常片段
                if (idx == -1) {
                    int loose = text.toLowerCase().indexOf("im_start");
                    if (loose != -1) idx = loose;
                }
                if (idx == -1) {
                    int looseEnd = text.toLowerCase().indexOf("im_end");
                    if (looseEnd != -1) idx = looseEnd;
                }
                if (idx == -1) return text;
                if (idx == 0) {
                    return null; // 整个分片即为标记或以标记开头，直接停止
                }
                return text.substring(0, idx);
            }
        });
    }

    private String buildLocalChatPrompt(List<Message> history, String currentPrompt, int maxChars) {
        StringBuilder sb = new StringBuilder();
        // 不再注入预设角色，直接从对话历史开始
        if (history != null) {
            for (Message m : history) {
                if (m == null || m.content == null) continue;
                String role = "user".equals(m.type) ? "user" : "assistant";
                String block = "<|im_start|>" + role + "\n" + m.content + "<|im_end|>\n";
                if (sb.length() + block.length() > maxChars) {
                    // 超出上限时跳过更早的消息
                    continue;
                }
                sb.append(block);
            }
        }

        String userBlockPrefix = "<|im_start|>user\n";
        String userBlockSuffix = "<|im_end|>\n<|im_start|>assistant\n";
        int overhead = userBlockPrefix.length() + userBlockSuffix.length();
        int remaining = maxChars - sb.length() - overhead;
        String safePrompt = currentPrompt == null ? "" : currentPrompt;
        if (remaining < safePrompt.length()) {
            // 截取 Prompt 末尾，保留最近意图
            safePrompt = safePrompt.substring(safePrompt.length() - Math.max(0, remaining));
        }

        sb.append(userBlockPrefix)
                .append(safePrompt)
                .append(userBlockSuffix);

        return sb.toString();
    }

    private void handleNetworkRequest(String apiPrompt, boolean isDeepThink, Message aiMsg, Message userMsg, ChatGenerationListener listener) {
        long currentSessionId = aiMsg.sessionId;
        List<ArkClient.Msg> contextMessages = new ArrayList<>();

        // 拉取上下文（使用线程池查询 DB，避免主线程阻塞）
        
        executor.execute(() -> {
            int fetchLimit = ChatConfig.MAX_CONTEXT_MESSAGES + 5;
            List<Message> dbMessages = messageDao.getLatestMessagesBySession(currentSessionId, fetchLimit);
            
            if (dbMessages != null) {
                Collections.sort(dbMessages, (m1, m2) -> Long.compare(m1.timestamp, m2.timestamp));
                List<Message> validHistory = new ArrayList<>();
                for (Message m : dbMessages) {
                    if (m.id == aiMsg.id) continue;
                    if (userMsg != null && m.id == userMsg.id) continue;
                    if ("user".equals(m.type) || "ai".equals(m.type)) {
                        validHistory.add(m);
                    }
                }
                if (validHistory.size() > ChatConfig.MAX_CONTEXT_MESSAGES) {
                    validHistory = validHistory.subList(validHistory.size() - ChatConfig.MAX_CONTEXT_MESSAGES, validHistory.size());
                }
                for (Message m : validHistory) {
                    String role = "user".equals(m.type) ? "user" : "assistant";
                    contextMessages.add(new ArkClient.Msg(role, m.content));
                }
            }
            
            contextMessages.add(new ArkClient.Msg("user", apiPrompt));

            // 网络调用需异步进行
            activeNetworkCancellable = new ArkClient().sendChat(contextMessages, isDeepThink, new ArkClient.StreamListener() {
                @Override
                public void onReasoning(String delta) {
                    listener.onThinking(delta);
                }

                @Override
                public void onChunk(String delta) {
                    listener.onContent(delta);
                }

                @Override
                public void onComplete() {
                    isGenerating = false;
                    activeNetworkCancellable = null;
                    listener.onComplete();
                }

                @Override
                public void onError(String message, Throwable t) {
                    isGenerating = false;
                    activeNetworkCancellable = null;
                    listener.onError(message + (t != null ? " " + t.getMessage() : ""));
                }
            });
        });
    }
}

