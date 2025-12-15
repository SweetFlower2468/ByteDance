package com.edu.neu.finalhomework.activity.main.adapter.holder;

import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.domain.entity.Message;
import com.edu.neu.finalhomework.domain.entity.Attachment;
import com.edu.neu.finalhomework.activity.main.adapter.ChatAdapter;
import com.edu.neu.finalhomework.activity.main.adapter.AttachmentAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户消息 ViewHolder
 * 对应 item_msg_user.xml
 */
public class UserMsgHolder extends RecyclerView.ViewHolder {
    
    private TextView tvContent;
    private View quoteFileWrapper;
    private View quoteTextWrapper;
    private RecyclerView recyclerAttachments;
    private TextView tvQuoteContent;
    
    private ChatAdapter.OnMessageActionListener listener;

    public UserMsgHolder(View itemView, ChatAdapter.OnMessageActionListener listener) {
        super(itemView);
        this.listener = listener;
        tvContent = itemView.findViewById(R.id.tv_content);
        quoteFileWrapper = itemView.findViewById(R.id.include_quote_file);
        quoteTextWrapper = itemView.findViewById(R.id.include_quote_text);
        
        if (quoteFileWrapper != null) {
            recyclerAttachments = quoteFileWrapper.findViewById(R.id.recycler_attachments);
            if (recyclerAttachments != null) {
                recyclerAttachments.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            }
            // 隐藏关闭按钮
            View btnClose = quoteFileWrapper.findViewById(R.id.btn_close);
            if (btnClose != null) btnClose.setVisibility(View.GONE);
        }
        
        if (quoteTextWrapper != null) {
            tvQuoteContent = quoteTextWrapper.findViewById(R.id.tv_quote_content);
            // 隐藏关闭按钮
            View btnClose = quoteTextWrapper.findViewById(R.id.btn_close);
            if (btnClose != null) btnClose.setVisibility(View.GONE);
        }
    }
    
    public void bind(Message message) {
        if (message != null) {
            if (tvContent != null) tvContent.setText(message.content);
            
            // 处理引用显示
            String quoteToShow = null;
            boolean hasQuote = (message.quotedMessageId != null) || (message.quotedContent != null && !message.quotedContent.isEmpty());
            if (hasQuote) {
                // 优先实时查 DB 的被引用消息内容
                if (message.quotedMessageId != null) {
                    com.edu.neu.finalhomework.domain.entity.Message ref =
                            com.edu.neu.finalhomework.App.getInstance()
                                    .getDatabase()
                                    .messageDao()
                                    .getMessageById(message.quotedMessageId);
                    if (ref != null) {
                        if (ref.content != null && !ref.content.isEmpty()) {
                            quoteToShow = ref.content;
                        } else if (ref.deepThink != null && !ref.deepThink.isEmpty()) {
                            quoteToShow = ref.deepThink;
                        }
                    }
                }
                // 退回已存的 quotedContent
                if (quoteToShow == null || quoteToShow.isEmpty()) {
                    quoteToShow = message.quotedContent;
                }
                if (quoteToShow != null) {
                    quoteToShow = quoteToShow.replaceAll("\\s+", " ").trim();
                }
                if (quoteToShow == null || quoteToShow.isEmpty()) quoteToShow = "(引用为空)";

                if (quoteTextWrapper != null) {
                    quoteTextWrapper.setVisibility(View.VISIBLE);
                    if (tvQuoteContent != null) tvQuoteContent.setText(quoteToShow);
                }
            } else {
                if (quoteTextWrapper != null) quoteTextWrapper.setVisibility(View.GONE);
            }
            
            // 处理附件
            if (message.attachments != null && !message.attachments.isEmpty()) {
                if (quoteFileWrapper != null) {
                    quoteFileWrapper.setVisibility(View.VISIBLE);
                    setupAttachments(message.attachments);
                }
            } else {
                if (quoteFileWrapper != null) quoteFileWrapper.setVisibility(View.GONE);
            }
            
            // 长按弹出操作菜单
            if (tvContent != null) {
                tvContent.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onLongClick(v, message);
                        return true;
                    }
                    return false;
                });
            }
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongClick(v, message);
                    return true;
                }
                return false;
            });
        }
    }
    
    private void setupAttachments(List<Attachment> attachments) {
        if (recyclerAttachments == null) return;
        
        // 将领域层附件转换为适配器对象
        List<AttachmentAdapter.Attachment> adapterItems = new ArrayList<>();
        for (Attachment att : attachments) {
            Uri uri = Uri.parse(att.filePath);
            adapterItems.add(new AttachmentAdapter.Attachment(
                    uri,
                    att.type,
                    att.fileName,
                    att.displaySize,
                    att.fileSize,
                    att.tokenCount,
                    null));
        }
        
        // 只读模式的附件列表
        AttachmentAdapter adapter = new AttachmentAdapter(adapterItems, null, true);
        recyclerAttachments.setAdapter(adapter);
    }
}
