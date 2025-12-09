package com.edu.neu.finalhomework.activity.profile;

import android.os.Bundle;
import android.widget.TextView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import io.noties.markwon.Markwon;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        TextView tvContent = findViewById(R.id.tv_content);
        String md = "# Final Homework\n\n" +
                    "这是一个基于 Android 的本地大模型对话应用。\n\n" +
                    "## 主要功能\n" +
                    "- **本地模型推理**: 支持 Llama 等模型的本地运行。\n" +
                    "- **深度思考**: 展示 AI 的思考过程。\n" +
                    "- **聊天记录**: 本地持久化存储。\n" +
                    "- **多媒体支持**: 发送文件、图片。\n\n" +
                    "Designed & Developed for ByteDance Training Camp.";
                    
        try {
            // Check if Markwon is available at runtime
            Markwon.create(this).setMarkdown(tvContent, md);
        } catch (Throwable e) {
             tvContent.setText(md);
        }
    }
}