package com.edu.neu.homework02.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.neu.homework02.R;
import com.edu.neu.homework02.helper.ToastUtils;
import com.edu.neu.homework02.model.MenuItem;

import java.util.List;

public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileMenuAdapter.MenuViewHolder> {

    private Context context;
    private List<MenuItem> menuList;

    // 1. 定义点击监听接口
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(MenuItem item);
    }

    // 2. 提供设置监听器的方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public ProfileMenuAdapter(Context context, List<MenuItem> menuList) {
        this.context = context;
        this.menuList = menuList;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_profile_menu_adapter, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = menuList.get(position);

        holder.TitleView.setText(item.getTitle());
        if (item.getIconResId() != 0) {
            holder.IconView.setImageResource(item.getIconResId());
        }

        // 3. 特殊处理：如果是退出登录，可以改变一下颜色（可选，为了美观）
        if ("退出登录".equals(item.getTitle())) {
            holder.TitleView.setTextColor(0xFFFF5252); // 设置为红色
            holder.ArrowView.setVisibility(View.GONE); // 退出通常不需要右箭头
        } else {
            holder.TitleView.setTextColor(0xFF333333); // 恢复黑色
            holder.ArrowView.setVisibility(View.VISIBLE);
        }

        // 4. 触发点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuList == null ? 0 : menuList.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView IconView;
        TextView TitleView;
        ImageView ArrowView;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            IconView = itemView.findViewById(R.id.menu_icon);
            TitleView = itemView.findViewById(R.id.menu_title);
            ArrowView = itemView.findViewById(R.id.arrow);
        }
    }
}