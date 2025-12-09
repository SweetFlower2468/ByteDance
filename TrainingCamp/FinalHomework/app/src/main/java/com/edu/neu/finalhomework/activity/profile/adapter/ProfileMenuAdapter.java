package com.edu.neu.finalhomework.activity.profile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import java.util.List;

public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileMenuAdapter.MenuViewHolder> {

    private List<MenuItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MenuItem item);
    }

    public static class MenuItem {
        public int id;
        public int iconRes;
        public String title;
        public String value;
        public int type; // 0: Normal, 1: Divider

        public MenuItem(int id, int iconRes, String title, String value) {
            this.id = id;
            this.iconRes = iconRes;
            this.title = title;
            this.value = value;
            this.type = 0;
        }
    }

    public ProfileMenuAdapter(List<MenuItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = items.get(position);
        
        holder.tvTitle.setText(item.title);
        holder.ivIcon.setImageResource(item.iconRes);
        if (item.value != null && !item.value.isEmpty()) {
            holder.tvValue.setText(item.value);
            holder.tvValue.setVisibility(View.VISIBLE);
        } else {
            holder.tvValue.setVisibility(View.GONE);
        }
        
        // Hide separator for last item potentially, but we didn't add separator in XML?
        // Let's assume layout has internal padding.
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvValue;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvValue = itemView.findViewById(R.id.tv_value);
        }
    }
}


