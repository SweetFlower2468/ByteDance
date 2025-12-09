package com.edu.neu.finalhomework.activity.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

import androidx.transition.TransitionManager;
import com.edu.neu.finalhomework.utils.SPUtils;

/**
 * 帮助与反馈 Activity
 * 对应 activity_help_feedback.xml
 */
public class HelpActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_feedback);
        
        initViews();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getTitle() != null && item.getTitle().toString().contains("历史")) {
                 navigateToFeedbackHistory();
                 return true;
            }
            String resName = getResources().getResourceEntryName(item.getItemId());
            if (resName != null && resName.contains("history")) {
                navigateToFeedbackHistory();
                return true;
            }
            return false;
        });
        
        TextView tvGreeting = findViewById(R.id.tv_greeting);
        String nickname = SPUtils.getString("user_nickname", "User");
        tvGreeting.setText("你好 " + nickname + "\n有什么可以帮助你的吗？");
        
        RecyclerView recyclerFaq = findViewById(R.id.recycler_faq);
        recyclerFaq.setLayoutManager(new LinearLayoutManager(this));
        recyclerFaq.setAdapter(new FAQAdapter(getFaqs()));
        
        findViewById(R.id.btn_feedback).setOnClickListener(v -> navigateToFeedbackSubmit());
    }
    
    private List<FAQ> getFaqs() {
        List<FAQ> list = new ArrayList<>();
        list.add(new FAQ("如何下载本地模型？", "在首页点击“模型管理”进入，选择喜欢的模型点击下载即可。支持断点续传。"));
        list.add(new FAQ("什么是深度思考模式？", "开启深度思考后，AI 会在回复前进行多步骤推理，展示思考过程，适合复杂逻辑问题。"));
        list.add(new FAQ("如何备份聊天记录？", "聊天记录目前存储在本地数据库中，暂不支持云端备份，请勿清除应用数据。"));
        list.add(new FAQ("支持图片识别吗？", "支持。点击输入框左侧的“+”号，选择相册或拍照上传图片，AI 可以识别图片内容。"));
        list.add(new FAQ("如何切换夜间模式？", "在“个人中心” -> “背景设置”中可以选择浅色、深色或跟随系统模式。"));
        return list;
    }
    
    private void navigateToFeedbackSubmit() {
        Intent intent = new Intent(this, FeedbackSubmitActivity.class);
        startActivity(intent);
    }
    
    private void navigateToFeedbackHistory() {
        Intent intent = new Intent(this, FeedbackHistoryActivity.class);
        startActivity(intent);
    }
    
    static class FAQ {
        String question;
        String answer;
        boolean expanded;
        
        public FAQ(String question, String answer) {
            this.question = question;
            this.answer = answer;
            this.expanded = false;
        }
    }
    
    static class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.ViewHolder> {
        List<FAQ> items;
        
        public FAQAdapter(List<FAQ> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FAQ item = items.get(position);
            holder.tvQuestion.setText(item.question);
            holder.tvAnswer.setText(item.answer);
            
            boolean expanded = item.expanded;
            holder.tvAnswer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            holder.ivArrow.setRotation(expanded ? 90 : 0);
            
            holder.itemView.setOnClickListener(v -> {
                androidx.transition.AutoTransition transition = new androidx.transition.AutoTransition();
                transition.excludeTarget(holder.ivArrow, true);
                transition.setDuration(200);
                TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView.getParent(), transition);
                
                item.expanded = !item.expanded;
                holder.tvAnswer.setVisibility(item.expanded ? View.VISIBLE : View.GONE);
                holder.ivArrow.animate().rotation(item.expanded ? 90 : 0).setDuration(200).start();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuestion, tvAnswer;
            ImageView ivArrow;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tv_question);
                tvAnswer = itemView.findViewById(R.id.tv_answer);
                ivArrow = itemView.findViewById(R.id.iv_arrow);
            }
        }
    }
}