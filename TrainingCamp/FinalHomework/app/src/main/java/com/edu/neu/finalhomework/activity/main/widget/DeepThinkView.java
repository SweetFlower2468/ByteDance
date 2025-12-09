package com.edu.neu.finalhomework.activity.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * 深度思考视图
 * 对应 view_deep_think.xml
 */
public class DeepThinkView extends LinearLayout {
    
    public DeepThinkView(Context context) {
        super(context);
        init();
    }
    
    public DeepThinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public DeepThinkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // TODO: 初始化视图
    }
    
    /**
     * 设置深度思考内容
     */
    public void setDeepThinkContent(String content) {
        // TODO: 实现内容设置
    }
    
    /**
     * 显示/隐藏视图
     */
    public void setVisible(boolean visible) {
        setVisibility(visible ? VISIBLE : GONE);
    }
}
