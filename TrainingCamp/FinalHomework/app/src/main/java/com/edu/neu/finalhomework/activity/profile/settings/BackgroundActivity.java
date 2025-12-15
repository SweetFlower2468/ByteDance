package com.edu.neu.finalhomework.activity.profile.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatDelegate;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.utils.SPUtils;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * 背景设置 Activity
 * 对应 activity_background_settings.xml
 */
public class BackgroundActivity extends BaseActivity {
    
    private ImageView ivCheckSystem;
    private ImageView ivCheckLight;
    private ImageView ivCheckDark;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_settings);
        
        initViews();
        initData();
        initListeners();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        ivCheckSystem = findViewById(R.id.iv_check_system);
        ivCheckLight = findViewById(R.id.iv_check_light);
        ivCheckDark = findViewById(R.id.iv_check_dark);
    }
    
    private void initData() {
        int mode = SPUtils.getInt("background_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        updateCheckState(mode);
    }
    
    private void initListeners() {
        findViewById(R.id.layout_system).setOnClickListener(v -> setMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        findViewById(R.id.layout_light).setOnClickListener(v -> setMode(AppCompatDelegate.MODE_NIGHT_NO));
        findViewById(R.id.layout_dark).setOnClickListener(v -> setMode(AppCompatDelegate.MODE_NIGHT_YES));
    }
    
    private void setMode(int mode) {
        SPUtils.putInt("background_mode", mode);
        String themeName = "默认";
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) themeName = "浅色";
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES) themeName = "深色";
        SPUtils.putString("background_theme", themeName);
        
        updateCheckState(mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    
    private void updateCheckState(int mode) {
        ivCheckSystem.setVisibility(View.GONE);
        ivCheckLight.setVisibility(View.GONE);
        ivCheckDark.setVisibility(View.GONE);
        
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            ivCheckLight.setVisibility(View.VISIBLE);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            ivCheckDark.setVisibility(View.VISIBLE);
        } else {
            ivCheckSystem.setVisibility(View.VISIBLE);
        }
    }
}
