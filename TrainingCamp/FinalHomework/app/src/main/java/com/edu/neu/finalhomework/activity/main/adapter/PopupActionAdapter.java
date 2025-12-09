package com.edu.neu.finalhomework.activity.main.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import java.util.List;

public class PopupActionAdapter extends RecyclerView.Adapter<PopupActionAdapter.ViewHolder> {

    public static class ActionItem {
        public int iconRes;
        public String text;
        public int id;
        public int colorRes = 0; // Optional tint

        public ActionItem(int id, int iconRes, String text) {
            this.id = id;
            this.iconRes = iconRes;
            this.text = text;
        }
    }

    private List<ActionItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ActionItem item);
    }

    public PopupActionAdapter(List<ActionItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popup_actions, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActionItem item = items.get(position);
        holder.tvName.setText(item.text);
        holder.ivIcon.setImageResource(item.iconRes);
        if (item.colorRes != 0) {
            holder.ivIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(item.colorRes));
            holder.tvName.setTextColor(holder.itemView.getContext().getResources().getColor(item.colorRes));
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
        
        // Hide divider for last item
        if (position == items.size() - 1) {
            holder.divider.setVisibility(View.GONE);
        } else {
            holder.divider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        View divider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_action_icon);
            tvName = itemView.findViewById(R.id.tv_action_name);
            divider = itemView.findViewById(R.id.divider);
        }
    }
}



