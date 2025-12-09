package com.edu.neu.finalhomework.activity.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * 聊天操作面板
 * 对应 view_chat_action_panel.xml (工具栏)
 */
public class ChatActionPanel extends LinearLayout {
    
    public ChatActionPanel(Context context) {
        super(context);
        init();
    }
    
    public ChatActionPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ChatActionPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // TODO: 初始化视图
    }
    
    /**
     * 设置按钮点击监听
     */
    public void setOnActionClickListener(String action, OnClickListener listener) {
        // TODO: 实现按钮点击监听
    }
}
