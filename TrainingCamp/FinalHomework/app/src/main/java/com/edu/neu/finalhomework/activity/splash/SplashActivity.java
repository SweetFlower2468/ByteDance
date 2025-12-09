package com.edu.neu.finalhomework.activity.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.main.ChatActivity;

/**
 * 启动页 Activity
 * 对应 activity_splash.xml
 */
public class SplashActivity extends BaseActivity {
    
    private static final long SPLASH_DELAY = 2000; // 2秒延迟
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_splash);
        
        // 延迟跳转到主界面
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, ChatActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
