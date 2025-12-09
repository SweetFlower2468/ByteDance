package com.edu.neu.finalhomework.activity.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.edu.neu.finalhomework.domain.entity.Message;

/**
 * 引用预览视图
 * 对应 view_quote_*.xml
 */
public class QuotePreviewView extends LinearLayout {
    
    public QuotePreviewView(Context context) {
        super(context);
        init();
    }
    
    public QuotePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public QuotePreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // TODO: 初始化视图
    }
    
    /**
     * 设置引用的消息
     */
    public void setQuotedMessage(Message message) {
        // TODO: 实现引用消息设置
    }
    
    /**
     * 设置关闭按钮点击监听
     */
    public void setOnCloseClickListener(OnClickListener listener) {
        // TODO: 实现关闭按钮监听
    }
}
