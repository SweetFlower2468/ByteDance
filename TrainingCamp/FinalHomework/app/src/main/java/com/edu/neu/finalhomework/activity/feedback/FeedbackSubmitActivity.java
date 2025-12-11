package com.edu.neu.finalhomework.activity.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.domain.entity.Feedback;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.concurrent.Executors;

import com.edu.neu.finalhomework.utils.ToastUtils;

/**
 * 反馈提交 Activity
 * 对应 activity_feedback_submit.xml
 */
public class FeedbackSubmitActivity extends BaseActivity {
    
    private EditText etContent;
    private TextView tvCounter;
    private Button btnSubmit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_submit);
        
        initViews();
        initListeners();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getTitle() != null && item.getTitle().toString().contains("历史")) {
                 Intent intent = new Intent(this, FeedbackHistoryActivity.class);
                 startActivity(intent);
                 return true;
            }
            // Fallback
            String resName = getResources().getResourceEntryName(item.getItemId());
            if (resName != null && resName.contains("history")) {
                Intent intent = new Intent(this, FeedbackHistoryActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        
        etContent = findViewById(R.id.et_feedback_content);
        tvCounter = findViewById(R.id.tv_counter);
        btnSubmit = findViewById(R.id.btn_submit);
    }
    
    private void initListeners() {
        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCounter.setText(s.length() + "/300");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }
    
    private void submitFeedback() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            ToastUtils.show(this, "请输入反馈内容");
            return;
        }
        
        Feedback feedback = new Feedback();
        feedback.content = content;
        feedback.title = "用户反馈";
        feedback.type = "suggestion";
        feedback.status = "pending";

        Executors.newSingleThreadExecutor().execute(() -> {
            App.getInstance().getDatabase().feedbackDao().insert(feedback);
            runOnUiThread(() -> {
                 ToastUtils.show(this, "反馈提交成功");
                 finish();
            });
        });
    }
}