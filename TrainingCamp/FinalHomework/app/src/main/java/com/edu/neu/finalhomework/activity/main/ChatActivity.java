package com.edu.neu.finalhomework.activity.main;

import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.database.Cursor;
import android.provider.OpenableColumns;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.main.adapter.ChatAdapter;
import com.edu.neu.finalhomework.activity.main.adapter.AttachmentAdapter;
import com.edu.neu.finalhomework.activity.main.network.ArkClient;
import com.edu.neu.finalhomework.activity.main.widget.ChatHeaderView;
import com.edu.neu.finalhomework.activity.main.widget.ChatInputPanel;
import com.edu.neu.finalhomework.activity.main.widget.MessageActionPopup;
import com.edu.neu.finalhomework.domain.entity.Message;
import com.edu.neu.finalhomework.domain.entity.Favorite;
import com.edu.neu.finalhomework.domain.entity.Attachment;
import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.domain.dao.MessageDao;
import com.edu.neu.finalhomework.domain.dao.SessionDao;
import com.edu.neu.finalhomework.domain.entity.Session;
import com.edu.neu.finalhomework.config.LimitConfig;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

import java.util.ArrayList;
import java.util.List;

import com.edu.neu.finalhomework.service.LlamaService;
import com.edu.neu.finalhomework.service.callback.StreamCallback;
import com.edu.neu.finalhomework.utils.MarkdownUtils;
import com.edu.neu.finalhomework.utils.PdfUtils;
import com.edu.neu.finalhomework.utils.ToastUtils;
import com.edu.neu.finalhomework.utils.TokenUtils;
import com.edu.neu.finalhomework.utils.DeviceSpecUtil;
import android.util.Log;

/**
 * 聊天主界面 Activity
 * 对应 activity_chat.xml (主控制器)
 */
public class ChatActivity extends BaseActivity {
    
    private ChatHeaderView chatHeader;
    private RecyclerView recyclerChat;
    private ChatInputPanel chatInputPanel;
    private ChatAdapter chatAdapter;
    private List<Message> messageList = new ArrayList<>();
    
    private com.edu.neu.finalhomework.ml.HandRecognitionManager handManager;
    
    private MessageDao messageDao;
    private SessionDao sessionDao;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final long SESSION_PENDING_NEW = -2;
    private long sessionId = -1;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private long currentPlayingMessageId = -1;
    private com.edu.neu.finalhomework.domain.entity.LocalModel currentActiveModel; // Added field
    private com.edu.neu.finalhomework.activity.main.controller.ChatController chatController;
    
    // Pagination
    private boolean isLoading = false;
    private boolean isLastPage = false;
    
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> fileLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 动态调整本地推理 batch（分片）大小，提升生成速度
        com.edu.neu.finalhomework.config.ModelConfig.nBatch = DeviceSpecUtil.recommendBatch(getApplicationContext());
        
        // Init DB
        messageDao = App.getInstance().getDatabase().messageDao();
        sessionDao = App.getInstance().getDatabase().sessionDao();
        sessionId = getIntent().getLongExtra("sessionId", -1);
        
        initLaunchers();
        initViews();
        
        // Initialize Hand Recognition
        handManager = new com.edu.neu.finalhomework.ml.HandRecognitionManager(this);
        handManager.setListener(isLeftHand -> {
            runOnUiThread(() -> {
                if (chatInputPanel != null) {
                    chatInputPanel.setHandMode(isLeftHand);
                }
            });
        });
        
        initData();
        initListeners();
        initTTS();
        
        chatController = new com.edu.neu.finalhomework.activity.main.controller.ChatController();

        // Load active model info once on create (no message reload)
        refreshActiveModel(false);
    }
    
    private void initTTS() {
        ttsReady = false;
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Try English as fallback
                    result = tts.setLanguage(Locale.US);
                }
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("ChatActivity", "TTS language not supported");
                    ttsReady = false;
                    return;
                }
                ttsReady = true;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        // Handled in onTts click
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            currentPlayingMessageId = -1;
                            if (chatAdapter != null) {
                                chatAdapter.setPlayingMessageId(-1);
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            currentPlayingMessageId = -1;
                            if (chatAdapter != null) {
                                chatAdapter.setPlayingMessageId(-1);
                            }
                        });
                    }
                });
            } else {
                Log.e("ChatActivity", "TTS init failed, status=" + status);
                ttsReady = false;
            }
        });
    }
    
    @Override
    protected void onNewIntent(Intent intent) {        super.onNewIntent(intent);
        setIntent(intent);
        long newSessionId = intent.getLongExtra("sessionId", -1);
        if (newSessionId != -1 && newSessionId != this.sessionId) {
            this.sessionId = newSessionId;
            messageList.clear();
            chatAdapter.notifyDataSetChanged();
            isLoading = false;
            isLastPage = false;
            chatInputPanel.setGenerating(false);
            chatInputPanel.clearQuote();
            loadMoreMessages();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (handManager != null) {
            handManager.start();
        }
        // 保持当前模型/会话，不自动切回历史，只校验会话是否被删除
        refreshActiveModel(false);
        executor.execute(() -> {
            if (sessionId != -1) {
                Session currentSession = sessionDao.getSessionById(sessionId);
                if (currentSession == null) {
                    runOnUiThread(() -> {
                        sessionId = -1;
                        messageList.clear();
                        chatAdapter.notifyDataSetChanged();
                        isLastPage = false;
                        isLoading = false;
                        chatInputPanel.clearQuote();
                        ToastUtils.show(this, "当前会话已被删除");
                    });
                    return;
                }
            }
            // 同步收藏状态
            if (messageDao != null && !messageList.isEmpty()) {
                List<Long> favIds = messageDao.getFavoriteMessageIds();
                runOnUiThread(() -> {
                    boolean changed = false;
                    for (Message m : messageList) {
                        boolean isFav = favIds.contains(m.id);
                        if (m.isFavorite != isFav) {
                            m.isFavorite = isFav;
                            changed = true;
                        }
                    }
                    if (changed) chatAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handManager != null) {
            handManager.stop();
        }
        if (tts != null) {
            tts.stop();
            currentPlayingMessageId = -1;
            if (chatAdapter != null) {
                chatAdapter.setPlayingMessageId(-1);
            }
            // 不弹窗，静默
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handManager != null) {
            handManager.close();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        if (chatInputPanel.isPanelOpen()) {
            chatInputPanel.setPanelVisibility(false);
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackPressTime > 2000) {
            ToastUtils.show(this, "再按一次退出应用");
            lastBackPressTime = currentTime;
        } else {
            super.onBackPressed();
        }
    }
    
    private long lastBackPressTime = 0;

    private void initLaunchers() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                     ToastUtils.show(this, "照片已拍摄");
                     // Logic to handle captured image (bitmap or uri)
                }
            }
        );

        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    chatInputPanel.addAttachment(uri, "image/jpeg", "Image", "Unknown");
                }
            }
        );

        fileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        // Try to persist permission when possible (ACTION_OPEN_DOCUMENT)
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    String[] info = getFileInfo(uri);
                    chatInputPanel.addAttachment(uri, "application/octet-stream", info[0], info[1]);
                }
            }
        );
    }

    private void initViews() {
        chatHeader = findViewById(R.id.chat_header);
        recyclerChat = findViewById(R.id.recycler_chat);
        chatInputPanel = findViewById(R.id.chat_input_panel);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        // Disable simple item animator to prevent height change animations (twitching)
        recyclerChat.setItemAnimator(null);
        recyclerChat.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messageList);
        // 禁用焦点，避免点击/长按触发跳动
        recyclerChat.setFocusable(false);
        recyclerChat.setFocusableInTouchMode(false);
        recyclerChat.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        
        chatAdapter.setOnMessageActionListener(new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onCopy(Message message) {
                copyText(message.content);
            }

            @Override
            public void onTts(Message message) {
                if (tts == null || !ttsReady) {
                    initTTS(); // Try to re-init if null/not ready
                    ToastUtils.show(ChatActivity.this, "TTS不可用，请稍后再试");
                    return;
                }
                
                if (message == null || message.content == null) return;
                
                // If clicking the same message that is currently playing
                if (currentPlayingMessageId == message.id && tts.isSpeaking()) {
                    tts.stop();
                    currentPlayingMessageId = -1;
                    chatAdapter.setPlayingMessageId(-1);
                    return;
                }
                
                // Stop any previous speech
                tts.stop();
                
                // Clean markdown
                String textToSpeak = MarkdownUtils.cleanMarkdown(message.content);
                
                // Speak
                int result = tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, String.valueOf(message.id));
                if (result == TextToSpeech.ERROR) {
                    ToastUtils.show(ChatActivity.this, "朗读失败");
                    ttsReady = false;
                    initTTS();
                    currentPlayingMessageId = -1;
                    chatAdapter.setPlayingMessageId(-1);
                } else {
                    currentPlayingMessageId = message.id;
                    chatAdapter.setPlayingMessageId(message.id);
                }
            }

            @Override
            public void onFavorite(Message message) {
                toggleFavorite(message);
            }

            @Override
            public void onRegenerate(Message message) {
                // Find user message before this AI message
                executor.execute(() -> {
                    Message userMsg = null;
                    int index = messageList.indexOf(message);
                    if (index > 0) {
                         // Simple check: previous message
                         Message prev = messageList.get(index - 1);
                         if ("user".equals(prev.type)) {
                             userMsg = prev;
                         }
                    }
                    
                    // If not found in list (e.g. pagination?), query DB
                    if (userMsg == null) {
                         // Logic to find query from DB if needed, but for now assuming list context
                    }

                    if (userMsg != null) {
                        // Rebuild API prompt including quotes and files
                        String apiPrompt = buildApiPrompt(userMsg);
                        boolean isDeepThink = (message.deepThink != null);
                        if (currentActiveModel != null && currentActiveModel.isLocal) {
                            isDeepThink = false;
                        }
                        final boolean finalDeepThink = isDeepThink;
                        
                        runOnUiThread(() -> {
                            ToastUtils.show(ChatActivity.this, "正在重新生成...");
                            
                            // Reset current AI message
                            message.content = "";
                            message.deepThink = finalDeepThink ? "" : null;
                            message.isGenerating = true;
                            message.isFailed = false;
                            // 重置点赞/点踩/收藏
                            message.isLiked = false;
                            message.isDisliked = false;
                            message.isFavorite = false;
                            chatInputPanel.setGenerating(true);
                            setRecyclerFocusDuringGeneration(true);
                            chatAdapter.notifyItemChanged(index);
                        });
                        
                        // Update DB
                        messageDao.updateMessage(message);
                        
                        // Resend
                        sendChatRequest(apiPrompt, finalDeepThink, message, userMsg);
                    } else {
                        runOnUiThread(() -> ToastUtils.show(ChatActivity.this, "无法找到对应的用户提问"));
                    }
                });
            }

            @Override
            public void onDelete(Message message) {
                deleteMessage(message);
            }
            
            @Override
            public void onLongClick(View view, Message message) {
                showMessagePopup(view, message);
            }
        });
        
        recyclerChat.setAdapter(chatAdapter);
        
        recyclerChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                userDragging = (newState == RecyclerView.SCROLL_STATE_DRAGGING);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    forceStickBottom = false;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(-1) && !isLoading && !isLastPage) {
                    loadMoreMessages();
                }
            }
        });
    }
    
    private void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("chat_content", text);
        clipboard.setPrimaryClip(clip);
        ToastUtils.show(ChatActivity.this, "已复制到剪贴板");
    }
    
    private void toggleFavorite(Message message) {
        boolean newState = !message.isFavorite;
        message.isFavorite = newState;
        // Strip stray think tokens in content when toggling favorite
        if (message.content != null && (message.content.contains("<think>") || message.content.contains("</think>"))) {
            message.content = message.content.replace("<think>", "").replace("</think>", "");
        }
        executor.execute(() -> {
            // 更新消息自身状态
            messageDao.updateMessage(message);
            if (newState) {
                // 新增独立收藏记录（快照）
                Favorite fav = new Favorite();
                fav.messageId = message.id;
                fav.aiMessageId = "ai".equals(message.type) ? message.id : null;
                fav.userMessageId = "user".equals(message.type) ? message.id : null;
                // 关联问答对
                if ("ai".equals(message.type)) {
                    Message q = messageDao.getPreviousMessage(message.sessionId, message.timestamp);
                    if (q != null && "user".equals(q.type)) {
                        fav.userMessageId = q.id;
                        fav.userContent = q.content;
                        fav.userAttachments = q.attachments;
                        fav.userQuotedContent = q.quotedContent;
                        fav.userQuotedMessageId = q.quotedMessageId;
                    }
                    fav.aiContent = message.content;
                } else if ("user".equals(message.type)) {
                    Message a = messageDao.getNextMessage(message.sessionId, message.timestamp);
                    if (a != null && "ai".equals(a.type)) {
                        fav.aiMessageId = a.id;
                        fav.aiContent = a.content;
                    }
                    fav.userContent = message.content;
                    fav.userAttachments = message.attachments;
                    fav.userQuotedContent = message.quotedContent;
                    fav.userQuotedMessageId = message.quotedMessageId;
                } else {
                    fav.aiContent = message.content;
                }
                fav.createdAt = System.currentTimeMillis();
                App.getInstance().getDatabase().favoriteDao().insert(fav);
            } else {
                // 取消收藏时仅移除最近一次该消息的收藏记录
                App.getInstance().getDatabase().favoriteDao().deleteLatestByMessageId(message.id);
            }
            runOnUiThread(() -> {
                chatAdapter.notifyDataSetChanged();
                ToastUtils.show(ChatActivity.this, newState ? "已收藏" : "已取消收藏");
            });
        });
    }
    
    private void deleteMessage(Message message) {
        if (chatInputPanel.getQuotedMessage() == message) {
            chatInputPanel.clearQuote();
        }
        executor.execute(() -> {
            messageDao.deleteMessage(message);
            runOnUiThread(() -> {
                messageList.remove(message);
                chatAdapter.notifyDataSetChanged();
                ToastUtils.show(ChatActivity.this, "已删除");
            });
        });
    }
    
    private void showMessagePopup(View anchor, Message message) {
        new MessageActionPopup(this, (actionId, msg) -> {
            switch (actionId) {
                case MessageActionPopup.ACTION_COPY:
                    copyText(msg.content);
                    break;
                case MessageActionPopup.ACTION_QUOTE:
                    chatInputPanel.setQuotedMessage(msg);
                    break;
                case MessageActionPopup.ACTION_FAVORITE:
                    toggleFavorite(msg);
                    break;
                case MessageActionPopup.ACTION_LIKE:
                    msg.isLiked = !msg.isLiked;
                    if (msg.isLiked) msg.isDisliked = false;
                    executor.execute(() -> messageDao.updateMessage(msg));
                    chatAdapter.notifyDataSetChanged();
                    ToastUtils.show(ChatActivity.this, msg.isLiked ? "已点赞" : "已取消点赞");
                    break;
                case MessageActionPopup.ACTION_DISLIKE:
                    msg.isDisliked = !msg.isDisliked;
                    if (msg.isDisliked) msg.isLiked = false;
                    executor.execute(() -> messageDao.updateMessage(msg));
                    chatAdapter.notifyDataSetChanged();
                    ToastUtils.show(ChatActivity.this, msg.isDisliked ? "已点踩" : "已取消点踩");
                    break;
                case MessageActionPopup.ACTION_DELETE:
                    deleteMessage(msg);
                    break;
                case MessageActionPopup.ACTION_TTS:
                    if (tts == null) {
                        initTTS(); // Try to re-init
                        ToastUtils.show(ChatActivity.this, "正在初始化TTS，请稍后重试");
                    } else if (message != null && message.content != null) {
                        // Reuse onTts logic logic to ensure consistent state
                        // Stop any previous speech
                        tts.stop();
                        
                        String textToSpeak = MarkdownUtils.cleanMarkdown(message.content);
                        int result = tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, String.valueOf(message.id));
                        
                        if (result == TextToSpeech.ERROR) {
                             ToastUtils.show(ChatActivity.this, "朗读失败");
                        } else {
                            currentPlayingMessageId = message.id;
                            if (chatAdapter != null) {
                                chatAdapter.setPlayingMessageId(message.id);
                            }
                        }
                    }
                    break;
                case MessageActionPopup.ACTION_REGENERATE:
                     executor.execute(() -> {
                        Message userMsg = null;
                        int index = messageList.indexOf(message);
                        if (index > 0) {
                             Message prev = messageList.get(index - 1);
                             if ("user".equals(prev.type)) userMsg = prev;
                        }
                        if (userMsg != null) {
                            String apiPrompt = buildApiPrompt(userMsg);
                            boolean isDeepThink = (message.deepThink != null);
                            final boolean finalDeepThink = isDeepThink;
                            runOnUiThread(() -> {
                                ToastUtils.show(ChatActivity.this, "正在重新生成...");
                                message.content = "";
                                message.deepThink = finalDeepThink ? "" : null;
                                message.isGenerating = true;
                                message.isFailed = false;
                                chatInputPanel.setGenerating(true);
                                chatAdapter.notifyItemChanged(index);
                            });
                            messageDao.updateMessage(message);
                            sendChatRequest(apiPrompt, finalDeepThink, message, userMsg);
                        }
                    });
                    break;
            }
        }).show(anchor, message);
    }
    

    private void initData() {
        if (sessionId != -1 && sessionId != SESSION_PENDING_NEW) {
            loadMoreMessages();
        } else {
            // Try to load latest session
            executor.execute(() -> {
                Session latest = sessionDao.getLatestSession();
                if (latest != null) {
                    sessionId = latest.id;
                    runOnUiThread(this::loadMoreMessages);
                }
            });
        }
    }

    private void loadMoreMessages() {
        if (sessionId == SESSION_PENDING_NEW) return;
        if (isLoading || isLastPage) return;
        isLoading = true;

        long lastTimestamp = Long.MAX_VALUE;
        if (!messageList.isEmpty()) {
            lastTimestamp = messageList.get(0).timestamp;
        }

        long finalLastTimestamp = lastTimestamp;
        executor.execute(() -> {
            List<Message> newMessages;
            if (finalLastTimestamp == Long.MAX_VALUE) {
                newMessages = messageDao.getLatestMessagesBySession(sessionId, LimitConfig.PAGE_SIZE);
            } else {
                newMessages = messageDao.getMessagesBySessionBefore(sessionId, finalLastTimestamp, LimitConfig.PAGE_SIZE);
            }
            
            runOnUiThread(() -> {
                if (newMessages == null || newMessages.isEmpty()) {
                    isLastPage = true;
                    if (messageList.isEmpty()) {
                        chatAdapter.notifyDataSetChanged();
                    }
                } else {
                    messageList.addAll(0, newMessages);
                    chatAdapter.notifyItemRangeInserted(0, newMessages.size());
                    
                    if (finalLastTimestamp == Long.MAX_VALUE) {
                        recyclerChat.scrollToPosition(messageList.size() - 1);
                    }
                    
                    if (newMessages.size() < LimitConfig.PAGE_SIZE) {
                        isLastPage = true;
                    }
                }
                isLoading = false;
            });
        });
    }

    private void sendChatRequest(String apiPrompt, boolean isDeepThink, Message aiMsg, Message userMsg) {
        chatController.sendChatRequest(apiPrompt, isDeepThink, aiMsg, userMsg, currentActiveModel, new com.edu.neu.finalhomework.activity.main.controller.ChatController.ChatGenerationListener() {
            @Override
            public void onThinking(String delta) {
                 updateMessageThinking(aiMsg, delta);
            }

            @Override
            public void onContent(String delta) {
                 updateMessageContent(aiMsg, delta);
            }

            @Override
            public void onComplete() {
                 handleGenerationComplete(aiMsg);
            }

            @Override
            public void onError(String error) {
                 handleGenerationError(aiMsg, error);
            }
        });
    }

    private void updateMessageThinking(Message aiMsg, String delta) {
        if (aiMsg.isGenerating) {
            aiMsg.isGenerating = false; 
        }
        
        // Accumulate
        aiMsg.deepThink = (aiMsg.deepThink == null ? "" : aiMsg.deepThink) + delta;
        
        // Update UI
        runOnUiThread(() -> {
            int index = messageList.indexOf(aiMsg);
            if (index != -1) {
                chatAdapter.notifyItemChanged(index, "partial_update");
                scrollToBottomIfNeeded();
            }
        });
    }

    private void updateMessageContent(Message aiMsg, String delta) {
        // Once we receive the first chunk, stop showing the loading dots
        if (aiMsg.isGenerating) {
            aiMsg.isGenerating = false;
        }

        // Accumulate
        aiMsg.content += delta;
        
        // Update UI
        runOnUiThread(() -> {
            int index = messageList.indexOf(aiMsg);
            if (index != -1) {
                chatAdapter.notifyItemChanged(index, "partial_update");
                scrollToBottomIfNeeded();
            }
        });
    }

    private void handleGenerationComplete(Message aiMsg) {
        aiMsg.isGenerating = false;
        executor.execute(() -> {
            messageDao.updateMessage(aiMsg);
            updateSessionLastMessage(aiMsg.content);
        });
        runOnUiThread(() -> {
            chatInputPanel.setGenerating(false);
            int index = messageList.indexOf(aiMsg);
            if (index != -1) chatAdapter.notifyItemChanged(index);
            forceStickBottom = false;
            setRecyclerFocusDuringGeneration(false);
        });
        StringBuilder full = new StringBuilder();
        if (aiMsg.deepThink != null && !aiMsg.deepThink.isEmpty()) {
            full.append("<think>").append(aiMsg.deepThink).append("</think>");
        }
        if (aiMsg.content != null) {
            full.append(aiMsg.content);
        }
        Log.i("ChatActivity", "AI complete | id=" + aiMsg.id + " | full=" + full);
    }

    private void handleGenerationError(Message aiMsg, String error) {
        aiMsg.isGenerating = false;
        aiMsg.content += "\n[Error: " + error + "]";
        executor.execute(() -> messageDao.updateMessage(aiMsg));
        runOnUiThread(() -> {
            chatInputPanel.setGenerating(false);
            int index = messageList.indexOf(aiMsg);
            if (index != -1) chatAdapter.notifyItemChanged(index);
            forceStickBottom = false;
            setRecyclerFocusDuringGeneration(false);
        });
        Log.e("ChatActivity", "AI error | id=" + aiMsg.id + " | error=" + error + " | partial=" + (aiMsg.content == null ? "" : aiMsg.content));
    }

    private void updateSessionLastMessage(String content) {
        Session session = sessionDao.getSessionById(sessionId);
        if (session != null) {
            session.lastMessage = content;
            session.updateTimestamp = System.currentTimeMillis();
            sessionDao.updateSession(session);
        }
    }

    private void setRecyclerFocusDuringGeneration(boolean generating) {
        runOnUiThread(() -> {
            if (recyclerChat == null) return;
            // 始终禁用焦点，防止点击/长按触发跳动
            recyclerChat.setFocusable(false);
            recyclerChat.setFocusableInTouchMode(false);
            recyclerChat.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            recyclerChat.clearFocus();
        });
    }
    
    private long lastAutoScrollTs = 0;
    private static final long AUTO_SCROLL_INTERVAL_MS = 800;
    private boolean forceStickBottom = false;
    private boolean userDragging = false;

    private void scrollToBottomIfNeeded() {
        if (!forceStickBottom) return; // 仅在强制贴底阶段滚动，避免回弹
        if (userDragging) return;
        long now = System.currentTimeMillis();
        if (now - lastAutoScrollTs < AUTO_SCROLL_INTERVAL_MS) return;
        lastAutoScrollTs = now;
        smoothScrollToBottom();
    }

    private void smoothScrollToBottom() {
        int last = messageList.size() - 1;
        if (last < 0) return;
        RecyclerView.LayoutManager lm = recyclerChat.getLayoutManager();
        if (lm instanceof LinearLayoutManager) {
            LinearLayoutManager llm = (LinearLayoutManager) lm;
            // 仅当列表末项可见或强制贴底时才滚动，避免跳回固定位置
            int lastVisible = llm.findLastVisibleItemPosition();
            if (forceStickBottom || lastVisible >= last - 2) {
                llm.scrollToPositionWithOffset(last, 0);
            }
        } else {
            recyclerChat.scrollToPosition(last);
        }
    }

    /**
     * 根据模型类型重新从数据库加载最近的消息列表，并刷新UI。
     * 不删除历史，只是限制当前列表长度以控制上下文。
     */
    private void reloadMessagesForModel(com.edu.neu.finalhomework.domain.entity.LocalModel model) {
        if (sessionId == -1 || model == null) return;
        final int keep = model.isLocal ? 60 : 150; // UI列表保留条数，历史仍在DB
        executor.execute(() -> {
            List<Message> latest = messageDao.getLatestMessagesBySession(sessionId, keep);
            if (latest != null) {
                runOnUiThread(() -> {
                    messageList.clear();
                    messageList.addAll(latest);
                    chatAdapter.notifyDataSetChanged();
                    smoothScrollToBottom();
                });
            }
        });
    }

    /**
     * 刷新当前激活模型信息，可选择是否重新加载消息
     */
    private void refreshActiveModel(boolean reloadMessages) {
        executor.execute(() -> {
            com.edu.neu.finalhomework.domain.entity.LocalModel activeModel =
                    App.getInstance().getDatabase().modelDao().getActiveModel(com.edu.neu.finalhomework.domain.entity.LocalModel.Status.ACTIVE);
            currentActiveModel = activeModel;
            runOnUiThread(() -> {
                chatHeader.setModelName(activeModel != null ? activeModel.name : "Select Model");
                chatInputPanel.setDeepThinkSupported(activeModel != null && !activeModel.isLocal && activeModel.isDeepThink);
                if (reloadMessages && activeModel != null && sessionId != -1 && sessionId != SESSION_PENDING_NEW) {
                    reloadMessagesForModel(activeModel);
                }
            });
        });
    }

    private void initListeners() {
        // Header Listeners
        chatHeader.setOnMenuClickListener(v -> startActivity(new Intent(this, com.edu.neu.finalhomework.activity.model.ModelManagerActivity.class)));
        chatHeader.setOnModelSelectClickListener(v -> showModelPopup(v));
        chatHeader.setOnNewSessionClickListener(v -> {
            sessionId = -1;
            messageList.clear();
            chatAdapter.notifyDataSetChanged();
            isLastPage = false;
            isLoading = false;
            chatInputPanel.clearQuote();
            ToastUtils.show(this, "已新建会话");
        });
        chatHeader.setOnHistoryClickListener(v -> startActivity(new Intent(this, com.edu.neu.finalhomework.activity.history.HistoryActivity.class)));
        chatHeader.setOnProfileClickListener(v -> startActivity(new Intent(this, com.edu.neu.finalhomework.activity.profile.ProfileActivity.class)));

        // Input Panel Listeners
        chatInputPanel.setOnSendListener(new ChatInputPanel.OnSendListener() {
            @Override
            public void onSend(String text, boolean isDeepThink, List<AttachmentAdapter.Attachment> attachments, Message quotedMessage) {
                boolean deepThinkAllowed = currentActiveModel != null && !currentActiveModel.isLocal && currentActiveModel.isDeepThink;
                handleSend(text, deepThinkAllowed && isDeepThink, attachments, quotedMessage);
            }
            
            @Override
            public void onStop() {
                stopGeneration();
            }
        });
        
        chatInputPanel.setOnActionClickListener(new ChatInputPanel.OnActionClickListener() {
            @Override
            public void onCameraClick() {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    cameraLauncher.launch(intent);
                } else {
                    ToastUtils.show(ChatActivity.this, "未找到相机应用");
                }
            }

            @Override
            public void onGalleryClick() {
                galleryLauncher.launch("image/*");
            }

            @Override
            public void onFileClick() {
                fileLauncher.launch("application/pdf");
            }

            @Override
            public void onDeepThinkToggle(boolean enabled) {
                ToastUtils.show(ChatActivity.this, "深度思考: " + (enabled ? "已开启" : "已关闭"));
            }
        });
    }

    private String buildApiPrompt(Message userMsg) {
        StringBuilder sb = new StringBuilder(userMsg.content);
        
        // Append Quote if exists
        if (userMsg.quotedContent != null && !userMsg.quotedContent.isEmpty()) {
            sb.append("\n\n引用内容：“").append(userMsg.quotedContent).append("”");
        }
        
        // Append PDF content if exists
        // Note: This relies on re-reading the file URI. If URI is invalid, we skip content.
        // We only persist file path (URI string), so we try to re-read.
        if (userMsg.attachments != null && !userMsg.attachments.isEmpty()) {
            for (Attachment att : userMsg.attachments) {
                if (att.type != null && (att.type.equals("application/pdf") || att.fileName.toLowerCase().endsWith(".pdf"))) {
                    try {
                        Uri uri = Uri.parse(att.filePath);
                        String pdfContent = PdfUtils.extractTextFromPdf(ChatActivity.this, uri);
                        if (pdfContent != null) {
                            sb.append("\n\n文件名称：“").append(att.fileName).append("”\n文件内容：“").append(pdfContent).append("”");
                        } else {
                            sb.append("\n\n文件名称：“").append(att.fileName).append("”\n(无法重新读取文件内容)");
                        }
                    } catch (Exception e) {
                        // Ignore errors during regeneration prompt build
                    }
                }
            }
        }
        return sb.toString();
    }
    
    private void handleSend(String text, boolean isDeepThink, List<AttachmentAdapter.Attachment> attachments, Message quotedMessage) {
        // Ensure active model is present
        if (currentActiveModel == null) {
            refreshActiveModel(false);
            if (currentActiveModel == null) {
                runOnUiThread(() -> ToastUtils.show(this, "请先选择模型"));
                chatInputPanel.setGenerating(false);
                return;
            }
        }
        final boolean finalDeepThink = isDeepThink;
        // Set generating state immediately
        runOnUiThread(() -> {
            chatInputPanel.setGenerating(true);
            forceStickBottom = true;
            setRecyclerFocusDuringGeneration(true);
        });

        executor.execute(() -> {
            // 1. Ensure Session
            if (sessionId == -1 || sessionId == SESSION_PENDING_NEW) {
                Session session = new Session();
                // Use Time + Snippet as title to ensure uniqueness and clarity
                String timeStr = com.edu.neu.finalhomework.utils.TimeUtils.formatDateTime(System.currentTimeMillis());
                String snippet = text.length() > 10 ? text.substring(0, 10) + "..." : text;
                session.title = timeStr + " " + snippet;
                
                session.lastMessage = text;
                session.updateTimestamp = System.currentTimeMillis();
                sessionId = sessionDao.insertSession(session);
            }

            // 2. Create User Message
            Message userMsg = new Message();
            userMsg.sessionId = sessionId;
            userMsg.type = "user";
            userMsg.content = text; // Only display text in UI
            userMsg.timestamp = System.currentTimeMillis();
            
            // Handle Quote (Metadata for UI display)
            if (quotedMessage != null) {
                userMsg.quotedMessageId = quotedMessage.id;
                if (quotedMessage.content != null && !quotedMessage.content.isEmpty()) {
                    userMsg.quotedContent = quotedMessage.content;
                } else if (quotedMessage.deepThink != null && !quotedMessage.deepThink.isEmpty()) {
                    userMsg.quotedContent = quotedMessage.deepThink;
                } else {
                    userMsg.quotedContent = "(引用为空)";
                }
            }
            
            // Handle Attachments (Metadata for UI display)
            if (attachments != null && !attachments.isEmpty()) {
                userMsg.attachments = new ArrayList<>();
                for (AttachmentAdapter.Attachment item : attachments) {
                    Attachment att = new Attachment();
                    att.filePath = item.uri.toString();
                    att.type = item.type;
                    att.fileName = item.name;
                    att.displaySize = item.size;
                    att.fileSize = item.sizeBytes > 0 ? item.sizeBytes : 0;
                    att.tokenCount = item.tokenCount;
                    userMsg.attachments.add(att);
                }
            }
            
            userMsg.id = messageDao.insertMessage(userMsg);
            
            runOnUiThread(() -> {
                messageList.add(userMsg);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerChat.smoothScrollToPosition(messageList.size() - 1);
            });

            // 3. Create Placeholder AI Response
            Message aiMsg = new Message();
            aiMsg.sessionId = sessionId;
            aiMsg.type = "ai";
            aiMsg.content = ""; // Start empty
            aiMsg.isGenerating = true; // Set generating state
            aiMsg.timestamp = System.currentTimeMillis();
            if (finalDeepThink) {
                aiMsg.deepThink = ""; // Placeholder for streaming thinking
            }
            aiMsg.id = messageDao.insertMessage(aiMsg);
            
            runOnUiThread(() -> {
                messageList.add(aiMsg);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerChat.smoothScrollToPosition(messageList.size() - 1);
            });

            // 4. Send Request via ArkClient
            // Only send text (content) + attachments/quote prompt to API, but userMsg in UI/DB only has text.
            // We construct the full prompt here.
            
            StringBuilder apiPromptBuilder = new StringBuilder(text);

            // Handle Quote
            if (quotedMessage != null && quotedMessage.content != null) {
                apiPromptBuilder.append("\n\n引用内容：“").append(quotedMessage.content).append("”");
            }

            // Handle PDF Attachments Reading (reuse cached text if available)
            if (attachments != null && !attachments.isEmpty()) {
                for (AttachmentAdapter.Attachment item : attachments) {
                    if (item.type != null && (item.type.equals("application/pdf") || item.name.toLowerCase().endsWith(".pdf"))) {
                        String pdfContent = item.extractedText;
                        if (pdfContent != null && !pdfContent.isEmpty()) {
                            apiPromptBuilder.append("\n\n文件名称：“").append(item.name).append("”\n文件内容：“").append(pdfContent).append("”");
                            int pdfTokens = item.tokenCount > 0 ? item.tokenCount : TokenUtils.estimateTokens(pdfContent);
                            item.tokenCount = pdfTokens;
                            if (chatInputPanel != null) {
                                chatInputPanel.updateAttachmentMeta(item.uri, pdfTokens, pdfContent);
                            }
                            Log.i("ChatActivity", "PDF parsed tokens=" + pdfTokens + " name=" + item.name);
                        } else {
                            item.tokenCount = 0;
                            if (chatInputPanel != null) {
                                chatInputPanel.updateAttachmentMeta(item.uri, 0, null);
                            }
                            Log.w("ChatActivity", "PDF content missing, skip re-read name=" + item.name);
                            apiPromptBuilder.append("\n\n文件名称：“").append(item.name).append("”\n(无法读取文件内容)");
                        }
                    }
                }
            }
            
            String apiPrompt = apiPromptBuilder.toString();
            
            sendChatRequest(apiPrompt, finalDeepThink, aiMsg, userMsg);
        });
    }

    private void stopGeneration() {
        ToastUtils.show(this, "正在停止生成...");
        if (chatController != null) {
            chatController.stopGeneration();
        }
        chatInputPanel.setGenerating(false);
        setRecyclerFocusDuringGeneration(false);
        // Reset latest generating message if exists
        int last = messageList.size() - 1;
        if (last >= 0) {
            Message m = messageList.get(last);
            if (m != null && m.isGenerating) {
                m.isGenerating = false;
                int idx = messageList.indexOf(m);
                if (idx != -1) {
                    chatAdapter.notifyItemChanged(idx);
                }
                executor.execute(() -> {
                    messageDao.updateMessage(m);
                    updateSessionLastMessage(m.content);
                });
            }
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (handManager != null) {
            handManager.processTouchEvent(ev);
        }
        
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = chatInputPanel;
            if (v != null) {
                int[] l = {0, 0};
                v.getLocationInWindow(l);
                int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left + v.getWidth();
                // If touch is OUTSIDE chatInputPanel
                if (ev.getX() < left || ev.getX() > right || ev.getY() < top || ev.getY() > bottom) {
                    hideKeyboard();
                    if (chatInputPanel.isPanelOpen()) {
                        chatInputPanel.setPanelVisibility(false, false);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view == null) view = chatInputPanel;
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    private void showModelPopup(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        
        executor.execute(() -> {
            List<com.edu.neu.finalhomework.domain.entity.LocalModel> models = App.getInstance().getDatabase().modelDao().getAllModels();
            runOnUiThread(() -> {
                for (com.edu.neu.finalhomework.domain.entity.LocalModel m : models) {
                    // Only show READY or ACTIVE models
                    if (m.status == com.edu.neu.finalhomework.domain.entity.LocalModel.Status.READY || 
                        m.status == com.edu.neu.finalhomework.domain.entity.LocalModel.Status.ACTIVE) {
                        popup.getMenu().add(0, (int)m.id, 0, m.name);
                    }
                }
                
                if (popup.getMenu().size() == 0) {
                     popup.getMenu().add("No Models Available");
                }

                chatHeader.animateArrow(true);
                
                popup.setOnMenuItemClickListener(item -> {
                    long id = item.getItemId();
                    if (id == 0) return true; // No models item
                    
                    executor.execute(() -> {
                        // Deactivate currently active models
                        App.getInstance().getDatabase().modelDao().changeStatus(com.edu.neu.finalhomework.domain.entity.LocalModel.Status.ACTIVE, com.edu.neu.finalhomework.domain.entity.LocalModel.Status.READY);
                        
                        // Activate new model
                        com.edu.neu.finalhomework.domain.entity.LocalModel target = App.getInstance().getDatabase().modelDao().getModelById(id);
                        if (target != null) {
                            target.status = com.edu.neu.finalhomework.domain.entity.LocalModel.Status.ACTIVE;
                            App.getInstance().getDatabase().modelDao().updateModel(target);
                            
                            runOnUiThread(() -> {
                                chatHeader.setModelName(target.name);
                                    chatInputPanel.setDeepThinkSupported(!target.isLocal && target.isDeepThink);
                                    reloadMessagesForModel(target);
                                ToastUtils.show(ChatActivity.this, "已切换模型: " + target.name);
                            });
                        }
                    });
                    return true;
                });
                
                popup.setOnDismissListener(menu -> chatHeader.animateArrow(false));
                
                popup.show();
            });
        });
    }

    private String[] getFileInfo(Uri uri) {
        String name = "Unknown";
        String size = "Unknown";
        
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    
                    if (nameIndex != -1) name = cursor.getString(nameIndex);
                    if (sizeIndex != -1) {
                        long sizeBytes = cursor.getLong(sizeIndex);
                        if (sizeBytes < 1024) size = sizeBytes + " B";
                        else if (sizeBytes < 1024 * 1024) size = (sizeBytes / 1024) + " KB";
                        else size = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return new String[]{name, size};
    }
}
