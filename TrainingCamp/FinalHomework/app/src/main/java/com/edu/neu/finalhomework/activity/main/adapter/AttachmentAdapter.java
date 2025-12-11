package com.edu.neu.finalhomework.activity.main.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.edu.neu.finalhomework.R;
import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FILE = 0;
    private static final int TYPE_IMAGE = 1;

    private List<Attachment> items;
    private OnRemoveListener listener;
    private boolean isReadOnly = false;

    public interface OnRemoveListener {
        void onRemove(Attachment attachment);
    }

    public static class Attachment {
        public Uri uri;
        public String type;
        public String name;
        public String size;       // display size string (may include tokens)
        public long sizeBytes;    // raw size in bytes
        public int tokenCount;    // estimated tokens (if parsed)
        public String extractedText; // optional cached text for reuse

        public Attachment(Uri uri, String type, String name, String size) {
            this(uri, type, name, size, -1, 0, null);
        }

        public Attachment(Uri uri, String type, String name, String size, long sizeBytes, int tokenCount, String extractedText) {
            this.uri = uri;
            this.type = type;
            this.name = name;
            this.size = size;
            this.sizeBytes = sizeBytes;
            this.tokenCount = tokenCount;
            this.extractedText = extractedText;
        }
    }

    public AttachmentAdapter(List<Attachment> items, OnRemoveListener listener) {
        this(items, listener, false);
    }

    public AttachmentAdapter(List<Attachment> items, OnRemoveListener listener, boolean isReadOnly) {
        this.items = items;
        this.listener = listener;
        this.isReadOnly = isReadOnly;
    }
    
    public List<Attachment> getItems() {
        return items;
    }

    @Override
    public int getItemViewType(int position) {
        String type = items.get(position).type;
        if (type != null && type.startsWith("image/")) {
            return TYPE_IMAGE;
        }
        return TYPE_FILE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_IMAGE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_attachment_image, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_attachment_file, parent, false);
            return new FileViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Attachment item = items.get(position);
        if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind(item, listener, isReadOnly);
        } else {
            ((FileViewHolder) holder).bind(item, listener, isReadOnly);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public void add(Attachment attachment) {
        items.add(attachment);
        notifyItemInserted(items.size() - 1);
    }
    
    public void remove(Attachment attachment) {
        int index = items.indexOf(attachment);
        if (index != -1) {
            items.remove(index);
            notifyItemRemoved(index);
        }
    }
    
    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        View btnRemove;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.image_view_attachment);
            btnRemove = itemView.findViewById(R.id.btn_remove_image);
        }

        public void bind(Attachment item, OnRemoveListener listener, boolean isReadOnly) {
            if (ivImage != null) {
                Glide.with(itemView).load(item.uri).into(ivImage);
            }
            if (btnRemove != null) {
                btnRemove.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
                btnRemove.setOnClickListener(v -> {
                    if (!isReadOnly && listener != null) listener.onRemove(item);
                });
            }
        }
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvSize;
        TextView tvToken;
        View btnRemove;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            tvName = itemView.findViewById(R.id.tv_file_name);
            tvSize = itemView.findViewById(R.id.tv_file_size);
            tvToken = itemView.findViewById(R.id.tv_token_count);
            btnRemove = itemView.findViewById(R.id.btn_remove_file);
        }

        public void bind(Attachment item, OnRemoveListener listener, boolean isReadOnly) {
            tvName.setText(item.name);
            tvSize.setText(item.size);
            if (tvToken != null) {
                tvToken.setVisibility(View.VISIBLE);
                tvToken.setText(formatTokens(item.tokenCount));
            }
            
            // Icon Logic
            int iconRes = R.drawable.ic_file_other;
            String nameLower = item.name.toLowerCase();
            if (nameLower.endsWith(".pdf")) iconRes = R.drawable.ic_file_pdf;
            else if (nameLower.endsWith(".doc") || nameLower.endsWith(".docx")) iconRes = R.drawable.ic_file_word;
            else if (nameLower.endsWith(".xls") || nameLower.endsWith(".xlsx")) iconRes = R.drawable.ic_file_excel;
            else if (nameLower.endsWith(".ppt") || nameLower.endsWith(".pptx")) iconRes = R.drawable.ic_file_ppt;
            else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".png") || nameLower.endsWith(".jpeg")) iconRes = R.drawable.ic_file_image;
            
            ivIcon.setImageResource(iconRes);
            // Remove tint for colored icons
            ivIcon.setImageTintList(null);
            
            if (btnRemove != null) {
                btnRemove.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
                btnRemove.setOnClickListener(v -> {
                     if (!isReadOnly && listener != null) listener.onRemove(item);
                });
            }
        }

        private String formatTokens(int tokens) {
            if (tokens <= 0) return "tokens";
            if (tokens >= 1000) {
                double k = tokens / 1000.0;
                return String.format(java.util.Locale.US, "%.1fk tokens", k);
            }
            return tokens + " tokens";
        }
    }
}
