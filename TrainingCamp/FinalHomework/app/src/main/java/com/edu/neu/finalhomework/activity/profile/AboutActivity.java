package com.edu.neu.finalhomework.activity.profile;

import android.os.Bundle;
import android.widget.TextView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import io.noties.markwon.Markwon;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvContent = findViewById(R.id.tv_content);
        String md = readAssetMarkdown("about_app.md");
        if (md == null || md.isEmpty()) {
            md = "# 关于本应用\n\n" +
                 "这是一个基于 Android 的本地/远端大模型聊天与模型管理应用。";
        }

        try {
            // 运行时检查 Markwon 是否可用
            Markwon.create(this).setMarkdown(tvContent, md);
        } catch (Throwable e) {
             tvContent.setText(md);
        }
    }

    private String readAssetMarkdown(String filename) {
        try (InputStream is = getAssets().open(filename);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return null;
        }
    }
}
