package com.edu.neu.finalhomework.activity.profile.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.utils.SPUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.slider.Slider;

/**
 * 字体大小设置 Activity
 * 对应 activity_font_size.xml
 */
public class FontSizeActivity extends BaseActivity {
    
    private MaterialToolbar toolbar;
    private Slider sliderFontSize;
    private TextView tvUserMsg, tvAiMsg;
    
    // 预览用默认字号（SP）
    private static final float DEFAULT_SIZE_BODY = 16f; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_size);
        
        initViews();
        initData();
        initListeners();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        sliderFontSize = findViewById(R.id.slider_font_size);
        
        View userMsgLayout = findViewById(R.id.preview_msg_user);
        View aiMsgLayout = findViewById(R.id.preview_msg_ai);
        
        if (userMsgLayout != null) {
            tvUserMsg = userMsgLayout.findViewById(R.id.tv_content);
            // 设置模拟文案
            if (tvUserMsg != null) tvUserMsg.setText("收到，调整很清晰！");
        }
        
        if (aiMsgLayout != null) {
            tvAiMsg = aiMsgLayout.findViewById(R.id.tv_content);
            // 设置模拟文案
            if (tvAiMsg != null) tvAiMsg.setText("你好，字号效果展示～");
        }
    }
    
    private void initData() {
        // 读取当前字号缩放
        float currentScale = SPUtils.getFontSizeScale();
        float sliderValue = 1.0f;
        if (currentScale == 0.85f) sliderValue = 0.0f;
        else if (currentScale == 1.0f) sliderValue = 1.0f;
        else if (currentScale == 1.15f) sliderValue = 2.0f;
        else if (currentScale == 1.30f) sliderValue = 3.0f;
        
        sliderFontSize.setValue(sliderValue);
        
        // 初始化预览文本显示
        updatePreviewText(currentScale);
    }
    
    private void initListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_confirm) { // 假定菜单含确认动作
                saveFontSize();
                return true;
            }
            return false;
        });
        
        sliderFontSize.addOnChangeListener((slider, value, fromUser) -> {
            float scale = getScaleFromValue(value);
            updatePreviewText(scale);
        });
    }
    
    private float getScaleFromValue(float value) {
        if (value == 0.0f) return 0.85f;
        if (value == 1.0f) return 1.0f;
        if (value == 2.0f) return 1.15f;
        if (value == 3.0f) return 1.30f;
        return 1.0f;
    }
    
    private void updatePreviewText(float scale) {
        // 需要获取预览 TextView。布局中未分配独立 ID，当前通过包含视图查找，依赖布局顺序（较脆弱）。
        
        if (tvUserMsg != null) tvUserMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_SIZE_BODY * scale);
        if (tvAiMsg != null) tvAiMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_SIZE_BODY * scale);
    }
    
    private void saveFontSize() {
        float scale = getScaleFromValue(sliderFontSize.getValue());
        SPUtils.setFontSizeScale(scale);
        // 是否需要重启应用/任务栈以生效？BaseActivity 在 onCreate 中会应用配置。
        // 通常需重建回退栈，这里简化处理：
        // （可选方案）启动主界面并清空栈：
        // Intent intent = new Intent(this, MainActivity.class);
        // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // startActivity(intent);
        //
        // 也可仅 finish 让用户返回，之前界面需重建才会更新；或调用 recreate() 强制刷新当前页。
        // 现采用简化方案：保存后直接关闭。
        
        SPUtils.setFontSizeScale(scale);
        finish();
    }
}
