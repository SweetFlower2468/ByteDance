package com.edu.neu.finalhomework.activity.feedback.adapter;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.domain.entity.Feedback;
import java.util.List;

/**
 * 反馈列表适配器
 * 对应 item_feedback_*.xml
 */
public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder> {
    
    private List<Feedback> feedbacks;
    
    public FeedbackAdapter(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }
    
    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TODO: 根据 viewType 创建对应的 ViewHolder
        return null;
    }
    
    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        // TODO: 绑定数据
    }
    
    @Override
    public int getItemCount() {
        return feedbacks != null ? feedbacks.size() : 0;
    }
    
    public void updateFeedbacks(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
        notifyDataSetChanged();
    }
    
    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        public FeedbackViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
        }
    }
}
