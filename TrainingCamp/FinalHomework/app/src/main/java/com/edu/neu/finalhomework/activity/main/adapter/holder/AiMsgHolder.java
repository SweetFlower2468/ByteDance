package com.edu.neu.finalhomework.activity.main.adapter.holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.main.adapter.ChatAdapter;
import com.edu.neu.finalhomework.domain.entity.Message;

/**
 * AI 消息 ViewHolder
 * 对应 item_msg_ai.xml
 */
public class AiMsgHolder extends RecyclerView.ViewHolder {
    
    private TextView tvContent;
    private View viewDeepThink;
    private TextView tvDtContent;
    private TextView tvDtDuration;
    private ImageView ivDtArrow;
    private View layoutActions;
    
    // Action Buttons
    private View btnCopy;
    private View btnTts;
    private View btnFavorite;
    private View btnRegenerate;
    private View btnDelete;

    private ChatAdapter.OnMessageActionListener listener;
    private boolean isDeepThinkExpanded = true;
    private boolean isReadOnly = false;

    public AiMsgHolder(View itemView, ChatAdapter.OnMessageActionListener listener) {
        super(itemView);
        this.listener = listener;
        
        tvContent = itemView.findViewById(R.id.tv_content);
        viewDeepThink = itemView.findViewById(R.id.view_deep_think);
        
        if (viewDeepThink != null) {
            tvDtContent = viewDeepThink.findViewById(R.id.tv_dt_content);
            tvDtDuration = viewDeepThink.findViewById(R.id.tv_dt_duration);
            ivDtArrow = viewDeepThink.findViewById(R.id.iv_dt_arrow);
            
            // Set toggle listener on the whole header or just arrow? 
            // User: "Ai回复时，可以点击深度思考的右上角小符号折叠深度思考过程中的信息" -> arrow or icon
            // Let's make the whole deep think view clickable for better UX, or just arrow.
            // I'll attach to viewDeepThink which is the included layout container.
            // Wait, viewDeepThink is the included root. I should click the header part.
            // But I don't have separate ID for header in include, let's just click the arrow or use the root.
            viewDeepThink.setOnClickListener(v -> toggleDeepThink());
        }
        
        layoutActions = itemView.findViewById(R.id.layout_actions);
        if (layoutActions != null) {
            btnCopy = layoutActions.findViewById(R.id.btn_copy);
            btnTts = layoutActions.findViewById(R.id.btn_tts);
            btnFavorite = layoutActions.findViewById(R.id.btn_favorite);
            btnRegenerate = layoutActions.findViewById(R.id.btn_regenerate);
            btnDelete = layoutActions.findViewById(R.id.btn_delete);
        }
    }
    
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        if (layoutActions != null) {
            layoutActions.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        }
    }

    private void toggleDeepThink() {
        if (tvDtContent == null) return;
        
        isDeepThinkExpanded = !isDeepThinkExpanded;
        tvDtContent.setVisibility(isDeepThinkExpanded ? View.VISIBLE : View.GONE);
        if (tvDtDuration != null) {
            tvDtDuration.setVisibility(isDeepThinkExpanded ? View.VISIBLE : View.GONE);
        }
        
        if (ivDtArrow != null) {
            ivDtArrow.animate().rotation(isDeepThinkExpanded ? 0 : -90).setDuration(200).start();
        }
    }
    
    public void bind(Message message) {
        if (message == null) return;

        // Bind Content
        if (tvContent != null) {
            tvContent.setText(message.content);
        }

        // Bind Deep Think
        if (viewDeepThink != null) {
            boolean hasDeepThink = message.deepThink != null && !message.deepThink.isEmpty();
            
            if (hasDeepThink) {
                viewDeepThink.setVisibility(View.VISIBLE);
                if (tvDtContent != null) {
                    tvDtContent.setText(message.deepThink);
                    tvDtContent.setVisibility(isDeepThinkExpanded ? View.VISIBLE : View.GONE);
                }
                if (tvDtDuration != null) {
                     // Mock duration or use data
                    tvDtDuration.setVisibility(isDeepThinkExpanded ? View.VISIBLE : View.GONE);
                }
            } else {
                viewDeepThink.setVisibility(View.GONE);
            }
        }
        
        // Bind Actions
        if (listener != null) {
            if (btnCopy != null) btnCopy.setOnClickListener(v -> listener.onCopy(message));
            if (btnTts != null) btnTts.setOnClickListener(v -> listener.onTts(message));
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> listener.onFavorite(message));
                // Update favorite icon state
                if (btnFavorite instanceof ImageView) {
                    ImageView iv = (ImageView) btnFavorite;
                    if (message.isFavorite) {
                        iv.setImageResource(R.drawable.ic_star_filled);
                        iv.setColorFilter(iv.getContext().getResources().getColor(R.color.brand_primary));
                    } else {
                        iv.setImageResource(R.drawable.ic_star);
                        iv.setColorFilter(iv.getContext().getResources().getColor(R.color.text_secondary));
                    }
                }
            }
            if (btnRegenerate != null) btnRegenerate.setOnClickListener(v -> listener.onRegenerate(message));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(message));
        }
        
        // Long click for popup
        if (itemView != null) {
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongClick(v, message);
                    return true;
                }
                return false;
            });
            // Also add to content view
            if (tvContent != null) {
                tvContent.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onLongClick(v, message);
                        return true;
                    }
                    return false;
                });
            }
        }
    }
}
