package com.edu.neu.finalhomework.activity.main.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.main.adapter.holder.AiMsgHolder;
import com.edu.neu.finalhomework.activity.main.adapter.holder.UserMsgHolder;
import com.edu.neu.finalhomework.domain.entity.Message;
import java.util.List;

/**
 * 消息列表适配器
 * 处理 User/AI/System 三种消息类型
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;
    private static final int TYPE_SYSTEM = 3;
    private static final int TYPE_FILE = 4;
    
    private List<Message> messages;
    private OnMessageActionListener actionListener;
    private boolean isReadOnly = false;

    public interface OnMessageActionListener {
        void onCopy(Message message);
        void onTts(Message message);
        void onFavorite(Message message);
        void onRegenerate(Message message);
        void onDelete(Message message);
        void onLongClick(View view, Message message);
    }
    
    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }
    
    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message == null) return TYPE_SYSTEM;

        String type = message.type;
        if ("user".equals(type)) {
            return TYPE_USER;
        } else if ("ai".equals(type)) {
            if (message.attachments != null && !message.attachments.isEmpty()) {
                return TYPE_FILE; // Not implemented yet, fallback to AI or special
            }
            return TYPE_AI;
        } else {
            return TYPE_SYSTEM;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_msg_user, parent, false);
            return new UserMsgHolder(view, actionListener);
        } else if (viewType == TYPE_AI) {
            View view = inflater.inflate(R.layout.item_msg_ai, parent, false);
            return new AiMsgHolder(view, actionListener);
        }
        // Fallback for System/File/Unknown
        View view = new View(parent.getContext());
        return new RecyclerView.ViewHolder(view) {};
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof UserMsgHolder) {
            ((UserMsgHolder) holder).bind(message);
        } else if (holder instanceof AiMsgHolder) {
            AiMsgHolder aiHolder = (AiMsgHolder) holder;
            aiHolder.setReadOnly(isReadOnly);
            aiHolder.bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }
    
    public void updateMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
}
