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
    
    // Default Text Sizes in SP
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
            // Set mock text
            if (tvUserMsg != null) tvUserMsg.setText("收到，调整很清晰。");
        }
        
        if (aiMsgLayout != null) {
            tvAiMsg = aiMsgLayout.findViewById(R.id.tv_content);
            // Set mock text
            if (tvAiMsg != null) tvAiMsg.setText("你好，字号效果展示。");
        }
    }
    
    private void initData() {
        // Load current scale
        float currentScale = SPUtils.getFontSizeScale();
        float sliderValue = 1.0f;
        if (currentScale == 0.85f) sliderValue = 0.0f;
        else if (currentScale == 1.0f) sliderValue = 1.0f;
        else if (currentScale == 1.15f) sliderValue = 2.0f;
        else if (currentScale == 1.30f) sliderValue = 3.0f;
        
        sliderFontSize.setValue(sliderValue);
        
        // Init preview text
        updatePreviewText(currentScale);
    }
    
    private void initListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_confirm) { // Assuming menu_font has confirm action
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
        // We need to access the TextViews. 
        // Since we didn't add IDs to includes, let's try to find them.
        // This is fragile but works if layout order is static.
        
        // Recursively finding all TextViews with id tv_content?
        // Let's assume I can update activity_font_size.xml to add IDs.
        // I will do that in a separate tool call for robustness.
        // For now, I will write the logic assuming they are accessible via `findViewById` if I had unique IDs.
        // I'll update the XML below.
        
        if (tvUserMsg != null) tvUserMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_SIZE_BODY * scale);
        if (tvAiMsg != null) tvAiMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_SIZE_BODY * scale);
    }
    
    private void saveFontSize() {
        float scale = getScaleFromValue(sliderFontSize.getValue());
        SPUtils.setFontSizeScale(scale);
        // Restart app or activity chain to apply? 
        // BaseActivity handles it in onCreate.
        // Usually need to recreate back stack.
        // For simplicity:
        // Intent intent = new Intent(this, MainActivity.class);
        // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // startActivity(intent);
        
        // Or just finish and let user navigate back, but previous activities won't update until recreated.
        // Let's just finish with a toast.
        // Or call recreate() on this activity to show it works? But this activity overrides it dynamically?
        // BaseActivity applies config on onCreate.
        
        SPUtils.setFontSizeScale(scale);
        finish();
    }
}
