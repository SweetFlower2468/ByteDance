package com.edu.neu.finalhomework.activity.favorites.adapter;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.domain.entity.Message;
import java.util.List;

/**
 * 收藏列表适配器
 * 对应 item_favorite.xml
 */
public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
    
    private List<Message> favorites;
    
    public FavoriteAdapter(List<Message> favorites) {
        this.favorites = favorites;
    }
    
    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TODO: 创建 ViewHolder
        return null;
    }
    
    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        // TODO: 绑定数据
    }
    
    @Override
    public int getItemCount() {
        return favorites != null ? favorites.size() : 0;
    }
    
    public void updateFavorites(List<Message> favorites) {
        this.favorites = favorites;
        notifyDataSetChanged();
    }
    
    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        public FavoriteViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
        }
    }
}
