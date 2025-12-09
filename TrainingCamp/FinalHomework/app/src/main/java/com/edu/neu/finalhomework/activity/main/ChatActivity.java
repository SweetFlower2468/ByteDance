package com.edu.neu.finalhomework.activity.main;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
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
import com.edu.neu.finalhomework.activity.main.widget.ChatHeaderView;
import com.edu.neu.finalhomework.activity.main.widget.ChatInputPanel;
import com.edu.neu.finalhomework.activity.main.widget.MessageActionPopup;
import com.edu.neu.finalhomework.domain.entity.Message;
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

import java.util.ArrayList;
import java.util.List;

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
    
    private MessageDao messageDao;
    private SessionDao sessionDao;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private long sessionId = -1;
    
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
        
        // Init DB
        messageDao = App.getInstance().getDatabase().messageDao();
        sessionDao = App.getInstance().getDatabase().sessionDao();
        sessionId = getIntent().getLongExtra("sessionId", -1);
        
        initLaunchers();
        initViews();
        initData();
        initListeners();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        long newSessionId = intent.getLongExtra("sessionId", -1);
        if (newSessionId != -1 && newSessionId != this.sessionId) {
            this.sessionId = newSessionId;
            messageList.clear();
            chatAdapter.notifyDataSetChanged();
            isLoading = false;
            isLastPage = false;
            loadMoreMessages();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        executor.execute(() -> {
            // 0. Update Active Model Name
            com.edu.neu.finalhomework.domain.entity.LocalModel activeModel = App.getInstance().getDatabase().modelDao().getActiveModel(com.edu.neu.finalhomework.domain.entity.LocalModel.Status.ACTIVE);
            String modelName = (activeModel != null) ? activeModel.name : "Select Model";
            runOnUiThread(() -> chatHeader.setModelName(modelName));

            // Check if current session still exists (might be deleted in HistoryActivity)
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
                        Toast.makeText(this, "当前会话已被删除", Toast.LENGTH_SHORT).show();
                    });
                    return; // Stop processing favorites if session is gone
                }
            }

            // Sync favorites status
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

    private void initLaunchers() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                     Toast.makeText(this, "照片已拍摄", Toast.LENGTH_SHORT).show();
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
        recyclerChat.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messageList);
        
        chatAdapter.setOnMessageActionListener(new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onCopy(Message message) {
                copyText(message.content);
            }

            @Override
            public void onTts(Message message) {
                Toast.makeText(ChatActivity.this, "开始朗读...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFavorite(Message message) {
                toggleFavorite(message);
            }

            @Override
            public void onRegenerate(Message message) {
                Toast.makeText(ChatActivity.this, "正在重新生成...", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(ChatActivity.this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleFavorite(Message message) {
        message.isFavorite = !message.isFavorite;
        executor.execute(() -> {
            messageDao.updateMessage(message);
            runOnUiThread(() -> {
                chatAdapter.notifyDataSetChanged();
                Toast.makeText(ChatActivity.this, message.isFavorite ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(ChatActivity.this, "已删除", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ChatActivity.this, msg.isLiked ? "已点赞" : "已取消点赞", Toast.LENGTH_SHORT).show();
                    break;
                case MessageActionPopup.ACTION_DISLIKE:
                    msg.isDisliked = !msg.isDisliked;
                    if (msg.isDisliked) msg.isLiked = false;
                    executor.execute(() -> messageDao.updateMessage(msg));
                    chatAdapter.notifyDataSetChanged();
                    Toast.makeText(ChatActivity.this, msg.isDisliked ? "已点踩" : "已取消点踩", Toast.LENGTH_SHORT).show();
                    break;
                case MessageActionPopup.ACTION_DELETE:
                    deleteMessage(msg);
                    break;
                case MessageActionPopup.ACTION_TTS:
                    Toast.makeText(ChatActivity.this, "开始朗读...", Toast.LENGTH_SHORT).show();
                    break;
                case MessageActionPopup.ACTION_REGENERATE:
                    Toast.makeText(ChatActivity.this, "正在重新生成...", Toast.LENGTH_SHORT).show();
                    break;
            }
        }).show(anchor, message);
    }
    

    private void initData() {
        // Setup Header (Initial placeholder, will be updated in onResume)
        // chatHeader.setModelName("Llama 3.2 (3B)"); // Removed hardcode
        
        if (sessionId != -1) {
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
            Toast.makeText(this, "已新建会话", Toast.LENGTH_SHORT).show();
        });
        chatHeader.setOnHistoryClickListener(v -> startActivity(new Intent(this, com.edu.neu.finalhomework.activity.history.HistoryActivity.class)));
        chatHeader.setOnProfileClickListener(v -> startActivity(new Intent(this, com.edu.neu.finalhomework.activity.profile.ProfileActivity.class)));

        // Input Panel Listeners
        chatInputPanel.setOnSendListener((text, isDeepThink, attachments, quotedMessage) -> {
            executor.execute(() -> {
                // 1. Ensure Session
                if (sessionId == -1) {
                    Session session = new Session();
                    session.title = text.length() > 20 ? text.substring(0, 20) + "..." : text;
                    session.lastMessage = text;
                    session.updateTimestamp = System.currentTimeMillis();
                    sessionId = sessionDao.insertSession(session);
                }

                // 2. Create User Message
                Message msg = new Message();
                msg.sessionId = sessionId;
                msg.type = "user";
                msg.content = text;
                msg.timestamp = System.currentTimeMillis();
                
                // Handle Quote
                if (quotedMessage != null) {
                    msg.quotedMessageId = quotedMessage.id;
                    msg.quotedContent = quotedMessage.content;
                }
                
                // Handle Attachments
                if (attachments != null && !attachments.isEmpty()) {
                    msg.attachments = new ArrayList<>();
                    for (AttachmentAdapter.Attachment item : attachments) {
                        Attachment att = new Attachment();
                        att.filePath = item.uri.toString();
                        att.type = item.type;
                        att.fileName = item.name;
                        att.displaySize = item.size;
                        att.fileSize = 0;
                        msg.attachments.add(att);
                    }
                }
                msg.id = messageDao.insertMessage(msg);
                
                // 3. Mock AI Response
                Message resp = new Message();
                resp.sessionId = sessionId;
                resp.type = "ai";
                resp.content = "收到消息：" + text + (isDeepThink ? " (Deep Think Enabled)" : "");
                resp.timestamp = System.currentTimeMillis() + 10;
                if (isDeepThink) {
                    resp.deepThink = "Thinking about: " + text;
                }
                resp.id = messageDao.insertMessage(resp);
                
                // 4. Update Session
                Session session = sessionDao.getSessionById(sessionId);
                if (session != null) {
                    session.lastMessage = resp.content;
                    session.updateTimestamp = resp.timestamp;
                    sessionDao.updateSession(session);
                }
                
                // 5. Update UI
                runOnUiThread(() -> {
                    messageList.add(msg);
                    messageList.add(resp);
                    chatAdapter.notifyDataSetChanged();
                    recyclerChat.smoothScrollToPosition(messageList.size() - 1);
                });
            });
        });

        chatInputPanel.setOnActionClickListener(new ChatInputPanel.OnActionClickListener() {
            @Override
            public void onCameraClick() {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    cameraLauncher.launch(intent);
                } else {
                    Toast.makeText(ChatActivity.this, "未找到相机应用", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onGalleryClick() {
                galleryLauncher.launch("image/*");
            }

            @Override
            public void onFileClick() {
                fileLauncher.launch("*/*");
            }

            @Override
            public void onDeepThinkToggle(boolean enabled) {
                Toast.makeText(ChatActivity.this, "深度思考: " + (enabled ? "已开启" : "已关闭"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
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
                                Toast.makeText(this, "已切换模型: " + target.name, Toast.LENGTH_SHORT).show();
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
