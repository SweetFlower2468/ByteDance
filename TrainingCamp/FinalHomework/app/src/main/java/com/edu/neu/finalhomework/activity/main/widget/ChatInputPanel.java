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
    private ImageButton btnMoreFeatures; // Deep Think toggle in input row
    private View actionPanel; // Bottom tool panel
    
    // Preview containers
    private View layoutPreviewContainer;
    private View viewQuoteText;
    private View viewQuoteFileWrapper; 
    private RecyclerView recyclerAttachments;
    private AttachmentAdapter attachmentAdapter;

    // Action Panel Items
    private View actionCamera;
    private View actionGallery;
    private View actionFile;
    private View actionDeepThink;

    // State Machine
    private enum State {
        IDLE,       // Input empty, panel closed -> Show ADD icon
        TYPING,     // Input has text -> Show SEND icon
        PANEL_OPEN  // Input empty, panel open -> Show CLOSE icon
    }

    private State currentState = State.IDLE;
    private boolean isDeepThinkEnabled = false;
    private Message quotedMessage;

    private OnSendListener onSendListener;
    private OnActionClickListener onActionClickListener;

    public interface OnSendListener {
        void onSend(String text, boolean isDeepThink, List<AttachmentAdapter.Attachment> attachments, Message quotedMessage);
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

        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnMoreFeatures = findViewById(R.id.btn_more_features);
        actionPanel = findViewById(R.id.action_panel);
        
        layoutPreviewContainer = findViewById(R.id.layout_preview_container);
        viewQuoteText = findViewById(R.id.view_quote_text);
        viewQuoteFileWrapper = findViewById(R.id.view_quote_file); // Points to view_quote_file_preview
        recyclerAttachments = viewQuoteFileWrapper.findViewById(R.id.recycler_attachments);
        View btnCloseAttachments = viewQuoteFileWrapper.findViewById(R.id.btn_close);
        if (btnCloseAttachments != null) {
            btnCloseAttachments.setOnClickListener(v -> clearQuote());
        }
        
        // Initialize Action Panel items
        actionCamera = findViewById(R.id.action_camera);
        actionGallery = findViewById(R.id.action_gallery);
        actionFile = findViewById(R.id.action_file);
        actionDeepThink = findViewById(R.id.action_deep_think);
        
        initAttachmentAdapter();
        setupActionButtons();
        setupListeners();
        updateState();
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
        // Configure Action Buttons (Icon + Text)
        setActionButton(actionCamera, R.drawable.ic_camera, "拍照");
        setActionButton(actionGallery, R.drawable.ic_photos, "相册");
        setActionButton(actionFile, R.drawable.ic_file_upload, "文件");
        setActionButton(actionDeepThink, R.drawable.ic_brain_gray, "深度思考");

        // Ensure button starts with correct selector
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
        // Text Watcher for State Machine
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

        // Send/Action Button Click
        btnSend.setOnClickListener(v -> handleSendButtonClick());

        // Brain Toggle (Input Row)
        btnMoreFeatures.setOnClickListener(v -> toggleDeepThink());

        // Action Panel Clicks
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
        
        // Editor Action (Enter key)
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (currentState == State.TYPING) {
                    handleSendButtonClick();
                    return true;
                }
            }
            return false;
        });

        // Focus Listener to close panel on keyboard open
        etInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // If input focused (keyboard likely showing), hide tool panel WITHOUT animation to prevent glitches
                if (isPanelOpen()) {
                    setPanelVisibility(false, false);
                }
            }
        });
    }

    private void handleSendButtonClick() {
        switch (currentState) {
            case TYPING:
                String text = etInput.getText().toString().trim();
                if (!text.isEmpty() && onSendListener != null) {
                    onSendListener.onSend(text, isDeepThinkEnabled, new ArrayList<>(attachmentAdapter.getItems()), quotedMessage);
                    etInput.setText(""); // Clear input
                    clearQuote(); // Clear attachments after send
                }
                break;
            case IDLE:
                // Expand Panel
                setPanelVisibility(true);
                // Clear focus to close keyboard
                etInput.clearFocus();
                hideKeyboard(etInput);
                break;
            case PANEL_OPEN:
                // Close Panel
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
        String text = etInput.getText().toString().trim();
        boolean hasText = !text.isEmpty();
        boolean isPanelVisible = actionPanel.getVisibility() == View.VISIBLE;

        if (hasText) {
            currentState = State.TYPING;
            btnSend.setImageResource(R.drawable.ic_send); 
             btnSend.setBackgroundResource(R.drawable.bg_circle_button); // Blue circle
             btnSend.setImageTintList(getResources().getColorStateList(android.R.color.white));
        } else {
            // Restore background
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
            // End previous transitions only if we are starting a new one
            TransitionManager.endTransitions(this);
            
            TransitionSet transition = new TransitionSet()
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds())
                    .setDuration(200);
            
            TransitionManager.beginDelayedTransition(this, transition);
        } else {
            // If NOT animating, just clean up running transitions.
            // DO NOT call beginDelayedTransition(this, null) as it enables AutoTransition
            // which causes ghosting/pop-up effects during layout changes (like keyboard open).
            TransitionManager.endTransitions(this);
        }
        
        actionPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        
        // Force layout update to ensure empty space is removed immediately
        if (!visible) {
            actionPanel.post(this::requestLayout);
        }
        
        updateState();
    }
    
    public boolean isPanelOpen() {
        return actionPanel.getVisibility() == View.VISIBLE;
    }

    private void toggleDeepThink() {
        isDeepThinkEnabled = !isDeepThinkEnabled;
        updateDeepThinkUI();
        if (onActionClickListener != null) {
            onActionClickListener.onDeepThinkToggle(isDeepThinkEnabled);
        }
    }

    private void updateDeepThinkUI() {
        // Use state_selected as defined in selector_brain_toggle.xml
        btnMoreFeatures.setSelected(isDeepThinkEnabled);
        
        if (isDeepThinkEnabled) {
            btnMoreFeatures.setImageResource(R.drawable.ic_brain_blue);
            btnMoreFeatures.setBackgroundResource(R.drawable.bg_white_circle); 
             btnMoreFeatures.setImageResource(R.drawable.selector_brain_toggle);
             btnMoreFeatures.setImageTintList(null); 
        } else {
             btnMoreFeatures.setImageResource(R.drawable.selector_brain_toggle);
             btnMoreFeatures.setImageTintList(null);
        }
        
        // Also update the icon in the panel if visible
        if (actionDeepThink != null) {
             ImageView iv = actionDeepThink.findViewById(R.id.iv_icon);
             TextView tv = actionDeepThink.findViewById(R.id.tv_label);
             if (iv != null) iv.setImageResource(isDeepThinkEnabled ? R.drawable.ic_brain_blue : R.drawable.ic_brain_gray);
             if (tv != null) tv.setText(isDeepThinkEnabled ? "深度思考(开)" : "深度思考");
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
        // Show Text Preview
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
     * Set Attachment (File or Image)
     */
    public void addAttachment(Uri uri, String type, String name, String size) {
        layoutPreviewContainer.setVisibility(View.VISIBLE);
        viewQuoteText.setVisibility(View.GONE);
        viewQuoteFileWrapper.setVisibility(View.VISIBLE);

        AttachmentAdapter.Attachment attachment = new AttachmentAdapter.Attachment(uri, type, name, size);
        attachmentAdapter.add(attachment);
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
}