package com.edu.neu.finalhomework.activity.history.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.domain.entity.Session;
import com.edu.neu.finalhomework.utils.TimeUtils;

public class HistoryAdapter extends ListAdapter<Session, HistoryAdapter.ViewHolder> {

    private final OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(Session session);
    }

    public HistoryAdapter(OnSessionClickListener listener) {
        super(new DiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Session session = getItem(position);
        holder.bind(session, listener);
    }
    
    public Session getSessionAt(int position) {
        return getItem(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvTitle;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvTokens;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_session_avatar);
            ivAvatar.setImageResource(R.drawable.ic_quote); // Set fixed icon
            tvTitle = itemView.findViewById(R.id.tv_session_title);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvTokens = itemView.findViewById(R.id.tv_token_badge);
        }

        public void bind(Session session, OnSessionClickListener listener) {
            tvTitle.setText(session.title != null ? session.title : "新会话");
            tvLastMessage.setText(session.lastMessage != null ? session.lastMessage : "暂无消息");
            
            // Time format logic
            tvTime.setText(TimeUtils.getFriendlyTimeSpanByNow(session.updateTimestamp));
            
            // Token display
            String tokenText = formatTokenCount(session.totalTokens);
            tvTokens.setText(tokenText);
            tvTokens.setVisibility(session.totalTokens > 0 ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSessionClick(session);
            });
        }
        
        private String formatTokenCount(int tokens) {
            if (tokens < 1000) return tokens + " tokens";
            return String.format("%.1fk tokens", tokens / 1000.0);
        }
    }

    static class DiffCallback extends DiffUtil.ItemCallback<Session> {
        @Override
        public boolean areItemsTheSame(@NonNull Session oldItem, @NonNull Session newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Session oldItem, @NonNull Session newItem) {
            return oldItem.updateTimestamp == newItem.updateTimestamp &&
                   oldItem.totalTokens == newItem.totalTokens &&
                   (oldItem.lastMessage == null ? newItem.lastMessage == null : oldItem.lastMessage.equals(newItem.lastMessage));
        }
    }
}
