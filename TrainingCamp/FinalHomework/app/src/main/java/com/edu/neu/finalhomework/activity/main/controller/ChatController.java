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
 * Chat Generation Controller (Middle Layer)
 * Encapsulates logic for dispatching requests to Local LlamaService or Network ArkClient.
 * Handles prompt construction, context retrieval, and response parsing.
 */
public class ChatController {

    private static final String TAG = "ChatController";
    private final MessageDao messageDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private Cancellable activeNetworkCancellable;
    private boolean isGenerating = false;

    // Listener for UI updates
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

    /**
     * Stop any ongoing generation
     */
    public void stopGeneration() {
        if (!isGenerating) return;

        // Stop Local
        LlamaService.getInstance().cancelInference();

        // Stop Network
        if (activeNetworkCancellable != null) {
            activeNetworkCancellable.cancel();
            activeNetworkCancellable = null;
        }

        isGenerating = false;
    }

    /**
     * Send a chat request
     * @param apiPrompt The prepared prompt (text + attachments description)
     * @param isDeepThink Whether deep thinking is enabled
     * @param aiMsg The placeholder AI message to update
     * @param userMsg The user message (for context filtering)
     * @param activeModel The model to use
     * @param listener Callback for UI updates
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

        // Build shorter context specifically for local models
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
            // Keep only the last N messages for local context
            if (contextHistory.size() > ChatConfig.MAX_LOCAL_CONTEXT_MESSAGES) {
                contextHistory = contextHistory.subList(contextHistory.size() - ChatConfig.MAX_LOCAL_CONTEXT_MESSAGES, contextHistory.size());
            }
        }

        // Build ChatML prompt with context + latest user query
        String chatPrompt = buildLocalChatPrompt(contextHistory, prompt, ChatConfig.MAX_LOCAL_PROMPT_CHARS);

        // Ensure model is loaded
        if (!service.isModelLoaded() || (service.getCurrentModel() != null && service.getCurrentModel().id != model.id)) {
            // Notify loading? (Maybe listener.onStatus("Loading..."))
            service.loadModel(model, new SimpleCallback() {
                @Override
                public void onSuccess() {
                    doLocalInference(service, chatPrompt, aiMsg, listener);
                }

                @Override
                public void onFailure(String error) {
                    isGenerating = false;
                    listener.onError("Model load failed: " + error);
                }
            });
        } else {
            doLocalInference(service, chatPrompt, aiMsg, listener);
        }
    }

    private void doLocalInference(LlamaService service, String chatPrompt, Message aiMsg, ChatGenerationListener listener) {
        // If Deep Thinking is requested, we might want to prompt for it,
        // but typically R1 models output <think> automatically.
        // If the user wants to DISABLE it, we might need a negative prompt, but for now we rely on parsing.

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

                // Stop Token Filtering
                if (containsStopToken(currentBuffer)) {
                    int stopIndex = getStopTokenIndex(currentBuffer);
                    if (stopIndex != -1) {
                        service.cancelInference();
                        String validContent = currentBuffer.substring(0, stopIndex);
                        processContent(validContent, listener, true);
                        
                        // We do NOT call onComplete here directly if the service guarantees calling onComplete after cancel.
                        // However, LlamaService implementation calls onComplete in finally block.
                        // So we just return.
                        return; 
                    }
                }

                // Process <think> tags
                String processed = processBufferForThinking(currentBuffer, listener);
                buffer.setLength(0);
                buffer.append(processed);

                // If we already hit the output cap, stop early
                if (outputLimitReached) {
                    service.cancelInference();
                    return;
                }
            }

            @Override
            public void onComplete() {
                // Flush remaining buffer
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

            // --- Helper Methods for Thinking Parsing (Ported from ChatActivity) ---

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

            // Remove ChatML control markers if they appear in generation. 
            // If marker is found, return null to indicate we should stop.
            private String stripStopMarkers(String text) {
                if (text == null || text.isEmpty()) return text;
                int idx = text.indexOf("<|im_start|>");
                if (idx == -1) idx = text.indexOf("<|im_end|>");
                if (idx == -1) idx = text.indexOf("<|endoftext|>");
                // Also guard against malformed variants like "<im_start"
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
                    return null; // Entire chunk is marker or starts with it -> stop
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
                    // Skip older messages if exceeding limit
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
            // Keep the tail of the prompt to preserve the most recent intent
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

        // Fetch Context
        // Note: Running DB query on main thread if called from main? 
        // ChatController should handle threading or assume caller handles it.
        // For safety, we use executor to fetch DB, then call ArkClient.
        
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

            // Network call must be async
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

