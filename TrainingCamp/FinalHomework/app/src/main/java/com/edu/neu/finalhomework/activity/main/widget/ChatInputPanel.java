package com.edu.neu.finalhomework.activity.main.widget;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.domain.entity.Message;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.activity.main.adapter.AttachmentAdapter;
import com.edu.neu.finalhomework.utils.TokenUtils;
import com.edu.neu.finalhomework.utils.PdfUtils;
import com.edu.neu.finalhomework.utils.ToastUtils;
import java.util.ArrayList;
import java.util.List;
import androidx.transition.TransitionManager;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionSet;
import android.view.ViewGroup;

/**
 * 聊天输入面板
 * 对应 view_input_panel.xml (处理输入、引用、状态机)
 */
public class ChatInputPanel extends ConstraintLayout {

    private EditText etInput;
    private ImageButton btnSend;
    private ImageButton btnMoreFeatures; // 输入行的深度思考开关
    private View actionPanel; // 底部工具面板
    private ConstraintLayout innerLayout;
    
    // 预览容器
    private View layoutPreviewContainer;
    private View viewQuoteText;
    private View viewQuoteFileWrapper; 
    private RecyclerView recyclerAttachments;
    private AttachmentAdapter attachmentAdapter;

    // 底部操作区
    private View actionCamera;
    private View actionGallery;
    private View actionFile;
    private View actionDeepThink;

    // 状态机
    private enum State {
        IDLE,       // 输入为空且面板关闭 -> 显示“添加”图标
        TYPING,     // 输入有文本 -> 显示“发送”图标
        PANEL_OPEN, // 输入为空且面板展开 -> 显示“关闭”图标
        GENERATING  // 正在生成 -> 显示“停止”图标
    }

    private State currentState = State.IDLE;
    private boolean isDeepThinkEnabled = false;
    private boolean isGenerating = false;
    private Message quotedMessage;

    private OnSendListener onSendListener;
    private OnActionClickListener onActionClickListener;

    public interface OnSendListener {
        void onSend(String text, boolean isDeepThink, List<AttachmentAdapter.Attachment> attachments, Message quotedMessage);
        default void onStop() {}
    }

    public interface OnActionClickListener {
        void onCameraClick();
        void onGalleryClick();
        void onFileClick();
        void onDeepThinkToggle(boolean enabled);
    }
    
    public ChatInputPanel(Context context) {
        super(context);
        init();
    }
    
    public ChatInputPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ChatInputPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_input_panel, this, true);

        innerLayout = findViewById(R.id.root_input_panel);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnMoreFeatures = findViewById(R.id.btn_more_features);
        actionPanel = findViewById(R.id.action_panel);
        
        layoutPreviewContainer = findViewById(R.id.layout_preview_container);
        viewQuoteText = findViewById(R.id.view_quote_text);
        viewQuoteFileWrapper = findViewById(R.id.view_quote_file); // 对应 view_quote_file_preview
        recyclerAttachments = viewQuoteFileWrapper.findViewById(R.id.recycler_attachments);
        View btnCloseAttachments = viewQuoteFileWrapper.findViewById(R.id.btn_close);
        if (btnCloseAttachments != null) {
            btnCloseAttachments.setOnClickListener(v -> clearQuote());
        }
        
        // 初始化底部操作区
        actionCamera = findViewById(R.id.action_camera);
        actionGallery = findViewById(R.id.action_gallery);
        actionFile = findViewById(R.id.action_file);
        actionDeepThink = findViewById(R.id.action_deep_think);
        
        initAttachmentAdapter();
        setupActionButtons();
        setupListeners();
        updateState();
        
        // 恢复左右手模式
        boolean isLeftHand = com.edu.neu.finalhomework.utils.SPUtils.getBoolean("input_hand_mode_left", false);
        setHandMode(isLeftHand);
    }

    private void initAttachmentAdapter() {
        attachmentAdapter = new AttachmentAdapter(new ArrayList<>(), this::removeAttachment);
        recyclerAttachments.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerAttachments.setAdapter(attachmentAdapter);
    }
    
    private void removeAttachment(AttachmentAdapter.Attachment attachment) {
        attachmentAdapter.remove(attachment);
        if (attachmentAdapter.getItemCount() == 0) {
            clearQuote();
        }
    }

    private void setupActionButtons() {
        // 配置操作按钮（图标+文字）
        setActionButton(actionCamera, R.drawable.ic_camera, "拍照");
        setActionButton(actionGallery, R.drawable.ic_photos, "相册");
        setActionButton(actionFile, R.drawable.ic_file_upload, "文件");
        setActionButton(actionDeepThink, R.drawable.ic_brain_inactive, "深度思考");

        // 确保按钮初始状态与 selector 对齐
        btnMoreFeatures.setImageResource(R.drawable.selector_brain_toggle);
    }

    private void setActionButton(View view, int iconRes, String label) {
        if (view == null) return;
        ImageView iv = view.findViewById(R.id.iv_icon);
        TextView tv = view.findViewById(R.id.tv_label);
        if (iv != null) iv.setImageResource(iconRes);
        if (tv != null) tv.setText(label);
    }

    private void setupListeners() {
        // 文本监听驱动状态机
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 发送/停止按钮点击
        btnSend.setOnClickListener(v -> handleSendButtonClick());

        // 输入行的深度思考开关
        btnMoreFeatures.setOnClickListener(v -> toggleDeepThink());

        // 底部操作区点击事件
        if (actionCamera != null) actionCamera.setOnClickListener(v -> {
            if (onActionClickListener != null) onActionClickListener.onCameraClick();
        });
        if (actionGallery != null) actionGallery.setOnClickListener(v -> {
            if (onActionClickListener != null) onActionClickListener.onGalleryClick();
        });
        if (actionFile != null) actionFile.setOnClickListener(v -> {
            if (onActionClickListener != null) onActionClickListener.onFileClick();
        });
        if (actionDeepThink != null) actionDeepThink.setOnClickListener(v -> toggleDeepThink());
        
        // 输入法回车事件
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (currentState == State.TYPING) {
                    handleSendButtonClick();
                    return true;
                }
            }
            return false;
        });

        // 焦点监听：键盘弹出时收起面板
        etInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 若输入框获得焦点（键盘弹出），无动画收起工具面板避免跳动
                if (isPanelOpen()) {
                    setPanelVisibility(false, false);
                }
            }
        });
    }

    public void setGenerating(boolean generating) {
        this.isGenerating = generating;
        updateState();
    }

    private void handleSendButtonClick() {
        switch (currentState) {
            case GENERATING:
                if (onSendListener != null) {
                    onSendListener.onStop();
                }
                break;
            case TYPING:
                String text = etInput.getText().toString().trim();
                if (!text.isEmpty() && onSendListener != null) {
                    Message currentQuote = (layoutPreviewContainer.getVisibility() == View.VISIBLE) ? quotedMessage : null;
                    onSendListener.onSend(text, isDeepThinkEnabled, new ArrayList<>(attachmentAdapter.getItems()), currentQuote);
                    etInput.setText(""); // 清空输入
                    clearQuote(); // 发送后清理引用/附件
                }
                break;
            case IDLE:
                // 展开面板
                setPanelVisibility(true);
                // 取消焦点以收起键盘
                etInput.clearFocus();
                hideKeyboard(etInput);
                break;
            case PANEL_OPEN:
                // 收起面板
                setPanelVisibility(false);
                break;
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void updateState() {
        if (isGenerating) {
            currentState = State.GENERATING;
            btnSend.setImageResource(R.drawable.ic_stop_circle); // 使用停止图标
            btnSend.setBackgroundResource(R.drawable.bg_transparent_circle); // 无背景
            btnSend.setImageTintList(getResources().getColorStateList(R.color.text_primary)); // 使用主文本色
            return;
        }

        String text = etInput.getText().toString().trim();
        boolean hasText = !text.isEmpty();
        boolean isPanelVisible = actionPanel.getVisibility() == View.VISIBLE;

        if (hasText) {
            currentState = State.TYPING;
            btnSend.setImageResource(R.drawable.ic_send); 
             btnSend.setBackgroundResource(R.drawable.bg_circle_button); // 蓝色圆形背景
             btnSend.setImageTintList(getResources().getColorStateList(android.R.color.white));
        } else {
            // 恢复默认背景
            btnSend.setBackgroundResource(android.R.color.transparent);
            btnSend.setImageTintList(null); 

            if (isPanelVisible) {
                currentState = State.PANEL_OPEN;
                btnSend.setImageResource(R.drawable.ic_close_circle); 
            } else {
                currentState = State.IDLE;
                btnSend.setImageResource(R.drawable.ic_add_circle);
            }
        }
    }

    public void setPanelVisibility(boolean visible) {
        setPanelVisibility(visible, true);
    }

    public void setPanelVisibility(boolean visible, boolean animate) {
        if (actionPanel.getVisibility() == (visible ? View.VISIBLE : View.GONE)) return;

        if (animate) {
            TransitionManager.endTransitions(this);
            
            TransitionSet transition = new TransitionSet()
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds())
                    .setDuration(200);
            
            TransitionManager.beginDelayedTransition(this, transition);
        }
        // 若不需要动画，避免调用 TransitionManager 以免与键盘布局冲突
        
        actionPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        updateState();
    }
    
    public boolean isPanelOpen() {
        return actionPanel.getVisibility() == View.VISIBLE;
    }

    private void toggleDeepThink() {
        if (!btnMoreFeatures.isEnabled()) {
            ToastUtils.show(getContext(), "本地模型不支持深度思考");
            return;
        }
        isDeepThinkEnabled = !isDeepThinkEnabled;
        updateDeepThinkUI();
        if (onActionClickListener != null) {
            onActionClickListener.onDeepThinkToggle(isDeepThinkEnabled);
        }
    }

    private void updateDeepThinkUI() {
        // 使用 selector_brain_toggle.xml 中的 state_selected 状态
        btnMoreFeatures.setSelected(isDeepThinkEnabled);
        
        if (isDeepThinkEnabled) {
            btnMoreFeatures.setImageResource(R.drawable.ic_brain_active);
            btnMoreFeatures.setBackgroundResource(R.drawable.bg_white_circle); 
             btnMoreFeatures.setImageResource(R.drawable.selector_brain_toggle);
             btnMoreFeatures.setImageTintList(null); 
        } else {
             btnMoreFeatures.setImageResource(R.drawable.selector_brain_toggle);
             btnMoreFeatures.setImageTintList(null);
        }
        
        // 同步更新面板中的图标与文案
        if (actionDeepThink != null) {
             ImageView iv = actionDeepThink.findViewById(R.id.iv_icon);
             TextView tv = actionDeepThink.findViewById(R.id.tv_label);
             if (iv != null) iv.setImageResource(isDeepThinkEnabled ? R.drawable.ic_brain_active : R.drawable.ic_brain_inactive);
             if (tv != null) tv.setText(isDeepThinkEnabled ? "深度思考(开)" : "深度思考");
        }
    }

    public void setDeepThinkSupported(boolean supported) {
        btnMoreFeatures.setEnabled(supported);
        btnMoreFeatures.setAlpha(supported ? 1.0f : 0.5f);
        if (!supported && isDeepThinkEnabled) {
             toggleDeepThink(); // 若不支持则强制关闭
        }
    }

    public void setOnSendListener(OnSendListener listener) {
        this.onSendListener = listener;
    }

    public void setOnActionClickListener(OnActionClickListener listener) {
        this.onActionClickListener = listener;
    }
    
    /**
     * 设置引用消息
     */
    public void setQuotedMessage(Message message) {
        if (message == null) return;
        this.quotedMessage = message;
        layoutPreviewContainer.setVisibility(View.VISIBLE);
        // 展示文本引用预览
        viewQuoteText.setVisibility(View.VISIBLE);
        viewQuoteFileWrapper.setVisibility(View.GONE);
        
        TextView tvQuote = viewQuoteText.findViewById(R.id.tv_quote_content);
        if (tvQuote != null) {
            tvQuote.setText(message.content);
        }
        
        View btnClose = viewQuoteText.findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> clearQuote());
        }
    }
    
    /**
     * 添加附件（文件或图片）
     */
    public void addAttachment(Uri uri, String type, String name, String size) {
        layoutPreviewContainer.setVisibility(View.VISIBLE);
        viewQuoteText.setVisibility(View.GONE);
        viewQuoteFileWrapper.setVisibility(View.VISIBLE);

        AttachmentAdapter.Attachment attachment = new AttachmentAdapter.Attachment(uri, type, name, size);
        int insertIndex = attachmentAdapter.getItems().size();
        attachmentAdapter.add(attachment);

        // 异步计算附件元信息，避免阻塞 UI
        new Thread(() -> {
            // 尝试持久化读权限
            try {
                getContext().getContentResolver().takePersistableUriPermission(uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            long sizeBytes = queryFileSize(uri);
            String baseDisplay = (sizeBytes > 0) ? formatSize(sizeBytes) : size;
            int tokenCount = 0;
            String extractedText = null;

            boolean isPdf = false;
            if (type != null && type.toLowerCase().contains("pdf")) isPdf = true; // 检查类型是否包含pdf
            if (name != null && name.toLowerCase().endsWith(".pdf")) isPdf = true; // 检查名称是否以.pdf结尾
            if (uri != null && uri.toString().toLowerCase().endsWith(".pdf")) isPdf = true; // 检查URI是否以.pdf结尾

            if (isPdf) {
                try {
                    extractedText = PdfUtils.extractTextFromPdf(getContext(), uri);
                    if (extractedText != null && !extractedText.isEmpty()) {
                        tokenCount = TokenUtils.estimateTokens(extractedText);
                    } else {
                        tokenCount = 0; // 解析为空时显式置为0
                    }
                } catch (Exception ignored) {
                    tokenCount = 0;
                }
            }

            attachment.sizeBytes = sizeBytes;
            attachment.tokenCount = tokenCount;
            attachment.extractedText = extractedText;
            attachment.size = baseDisplay; // 仅显示尺寸，不在这里重复显示token

            android.util.Log.i("AttachmentToken", "name=" + name + " bytes=" + sizeBytes + " tokens=" + attachment.tokenCount);

            post(() -> attachmentAdapter.notifyItemChanged(insertIndex));
        }).start();
    }

    /**
     * 在发送阶段重新解析出 token 时，回写到附件预览，避免 UI 仍显示 0
     */
    public void updateAttachmentMeta(Uri uri, int tokenCount, String extractedText) {
        List<AttachmentAdapter.Attachment> list = attachmentAdapter.getItems();
        for (int i = 0; i < list.size(); i++) {
            AttachmentAdapter.Attachment a = list.get(i);
            if (a.uri != null && a.uri.equals(uri)) {
                a.tokenCount = tokenCount;
                if (extractedText != null) a.extractedText = extractedText;
                attachmentAdapter.notifyItemChanged(i);
                break;
            }
        }
    }
    
    /**
     * 清除引用
     */
    public void clearQuote() {
        this.quotedMessage = null;
        layoutPreviewContainer.setVisibility(View.GONE);
        viewQuoteText.setVisibility(View.GONE);
        viewQuoteFileWrapper.setVisibility(View.GONE);
        attachmentAdapter.clear();
    }
    
    public Message getQuotedMessage() {
        return quotedMessage;
    }
    
    public String getInputText() {
        return etInput.getText().toString();
    }

    /**
     * 切换左右手布局
     * @param isLeftHand true: 左手模式（发送按钮在左），false: 右手模式（发送按钮在右）
     */
    public void setHandMode(boolean isLeftHand) {
        if (innerLayout == null) return;
        
        androidx.constraintlayout.widget.ConstraintSet constraintSet = new androidx.constraintlayout.widget.ConstraintSet();
        constraintSet.clone(innerLayout);

        if (isLeftHand) {
            // 左手模式：按钮在左，输入框在右
            // 清理已有约束
            constraintSet.clear(R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.END);
            constraintSet.clear(R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.START);
            constraintSet.clear(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.START);
            constraintSet.clear(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.END);

            // 按钮连接到父布局起始侧
            constraintSet.connect(R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START, dpToPx(8));
            
            // 输入框连接到按钮末端及父布局末端
            constraintSet.connect(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.START, R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.END, dpToPx(8));
            constraintSet.connect(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END, dpToPx(12));
            
        } else {
            // 右手模式（默认）：按钮在右，输入框在左
            // 清理已有约束
            constraintSet.clear(R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.END);
            constraintSet.clear(R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.START);
            constraintSet.clear(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.START);
            constraintSet.clear(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.END);

            // 按钮连接到父布局末端
            constraintSet.connect(R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END, dpToPx(8));

            // 输入框连接到父布局起始侧，并连接按钮
            constraintSet.connect(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START, dpToPx(12));
            constraintSet.connect(R.id.input_wrapper, androidx.constraintlayout.widget.ConstraintSet.END, R.id.btn_send, androidx.constraintlayout.widget.ConstraintSet.START, dpToPx(8));
        }
        
        androidx.transition.TransitionManager.beginDelayedTransition(innerLayout);
        constraintSet.applyTo(innerLayout);
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private long queryFileSize(Uri uri) {
        try (android.database.Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format(java.util.Locale.US, "%.2f GB", gb);
    }
}
