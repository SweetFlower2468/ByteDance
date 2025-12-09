package com.edu.neu.finalhomework.activity.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.edu.neu.finalhomework.R;

/**
 * 聊天头部视图
 * 对应 view_chat_header.xml
 */
public class ChatHeaderView extends ConstraintLayout {

    private View btnNavModel;
    private View layoutModelSpinner;
    private TextView tvCurrentModel;
    private View btnNewSession;
    private View btnHistory;
    private View btnProfile;
    private View ivModelArrow;
    
    public ChatHeaderView(Context context) {
        super(context);
        init();
    }
    
    public ChatHeaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ChatHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_chat_header, this, true);
        
        btnNavModel = findViewById(R.id.btn_nav_model);
        layoutModelSpinner = findViewById(R.id.layout_model_spinner);
        tvCurrentModel = findViewById(R.id.tv_current_model);
        btnNewSession = findViewById(R.id.btn_new_session);
        btnHistory = findViewById(R.id.btn_history);
        btnProfile = findViewById(R.id.btn_profile);
        ivModelArrow = findViewById(R.id.iv_model_arrow);
    }

    public void animateArrow(boolean isExpanded) {
        if (ivModelArrow != null) {
            ivModelArrow.animate().rotation(isExpanded ? -90f : 0f).setDuration(200).start();
        }
    }
    
    /**
     * 设置当前模型名称
     */
    public void setModelName(String name) {
        if (tvCurrentModel != null) {
            tvCurrentModel.setText(name);
        }
    }

    public void setOnMenuClickListener(OnClickListener listener) {
        if (btnNavModel != null) btnNavModel.setOnClickListener(listener);
    }

    public void setOnModelSelectClickListener(OnClickListener listener) {
        if (layoutModelSpinner != null) layoutModelSpinner.setOnClickListener(listener);
    }

    public void setOnNewSessionClickListener(OnClickListener listener) {
        if (btnNewSession != null) btnNewSession.setOnClickListener(listener);
    }

    public void setOnHistoryClickListener(OnClickListener listener) {
        if (btnHistory != null) btnHistory.setOnClickListener(listener);
    }

    public void setOnProfileClickListener(OnClickListener listener) {
        if (btnProfile != null) btnProfile.setOnClickListener(listener);
    }
}
