package com.edu.neu.finalhomework.activity.base;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.utils.SPUtils;

/**
 * Activity 基类
 * 处理沉浸式、字体大小缩放 (FontScale)
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 应用字体大小缩放
        applyFontScale();
        
        // 设置沉浸式状态栏
        setupImmersiveStatusBar();
    }
    
    /**
     * 应用字体大小缩放
     * 注意：updateConfiguration 在 API 17+ 已废弃，但为了兼容性仍使用
     * 更好的方式是使用 Activity 的 recreate() 或通过系统设置
     */
    @SuppressWarnings("deprecation")
    private void applyFontScale() {
        float scale = SPUtils.getFontSizeScale();
        if (scale != 1.0f) {
            android.content.res.Configuration config = getResources().getConfiguration();
            config.fontScale = scale;
            getResources().updateConfiguration(
                    config,
                    getResources().getDisplayMetrics()
            );
        }
    }
    
    /**
     * 设置沉浸式状态栏
     */
    private void setupImmersiveStatusBar() {
        // 1. 设置状态栏图标颜色（亮色/深色）
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
                getWindow(), decorView
        );
        controller.setAppearanceLightStatusBars(true); // 状态栏图标变黑（假设背景是浅色）

        // 2. 边缘到边缘 (Edge-to-Edge) 设置
        // 这会让内容延伸到状态栏和导航栏后面，实现沉浸式
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 3. 手动处理 Insets，避免内容被遮挡
        // 为根布局添加 Padding，避开状态栏和导航栏
        View content = findViewById(android.R.id.content);
        if (content != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.systemBars() |
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout() |
                        androidx.core.view.WindowInsetsCompat.Type.ime()
                );
                
                // Apply padding to avoid overlap
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                
                // Return CONSUMED to stop propagation if we handled it fully, 
                // or return windowInsets to let children see them (usually better to return windowInsets)
                return androidx.core.view.WindowInsetsCompat.CONSUMED;
            });
        }
    }
    
    /**
     * 获取 Application 实例
     */
    protected App getApp() {
        return (App) getApplication();
    }
}
