package com.edu.neu.finalhomework.activity.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.config.LimitConfig;
import com.edu.neu.finalhomework.domain.entity.Favorite;
import com.edu.neu.finalhomework.utils.TimeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FavoritesActivity extends BaseActivity {

    private EditText etSearch;
    private RecyclerView recyclerFavorites;
    private FavoriteAdapter adapter;
    
    // 分页控制
    private long lastTimestamp = Long.MAX_VALUE;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        
        initViews();
        resetAndLoad();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 返回详情后，收藏状态理论上不变（详情只读、不可删除），如需绝对一致可在此重载
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
             @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
             @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                 String keyword = s.toString().trim();
                 if (keyword.isEmpty()) {
                     isSearching = false;
                     resetAndLoad();
                 } else {
                     isSearching = true;
                     performSearch(keyword);
                 }
             }
             @Override public void afterTextChanged(Editable s) {}
        });
        
        recyclerFavorites = findViewById(R.id.recycler_favorites);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerFavorites.setLayoutManager(layoutManager);
        adapter = new FavoriteAdapter();
        recyclerFavorites.setAdapter(adapter);
        
        recyclerFavorites.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!isSearching && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMore();
                    }
                }
            }
        });
    }
    
    private void resetAndLoad() {
        lastTimestamp = Long.MAX_VALUE;
        isLastPage = false;
        isLoading = false;
        adapter.clear();
        loadMore();
    }
    
    private void loadMore() {
        if (isLoading || isLastPage) return;
        isLoading = true;
        
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Favorite> newItems = App.getInstance().getDatabase().favoriteDao()
                    .getFavoritesBefore(lastTimestamp, LimitConfig.FAVORITES_PAGE_SIZE);
            
            runOnUiThread(() -> {
                if (newItems.isEmpty()) {
                    isLastPage = true;
                } else {
                    adapter.appendItems(newItems);
                    lastTimestamp = newItems.get(newItems.size() - 1).createdAt;
                    if (newItems.size() < LimitConfig.FAVORITES_PAGE_SIZE) {
                        isLastPage = true;
                    }
                }
                isLoading = false;
            });
        });
    }
    
    private void performSearch(String keyword) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Favorite> results = App.getInstance().getDatabase().favoriteDao().searchFavorites(keyword);
            runOnUiThread(() -> {
                adapter.setItems(results);
            });
        });
    }
    
    class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {
        List<Favorite> items = new ArrayList<>();
        
        void setItems(List<Favorite> items) {
            this.items = new ArrayList<>(items);
            notifyDataSetChanged();
        }
        
        void appendItems(List<Favorite> newItems) {
            int start = items.size();
            items.addAll(newItems);
            notifyItemRangeInserted(start, newItems.size());
        }
        
        void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Favorite m = items.get(position);
            holder.tvQuestion.setText(m.userContent != null ? m.userContent : "(无提问)");
            holder.tvAnswer.setText(m.aiContent != null ? m.aiContent : "(无回答)");
            holder.tvTime.setText(TimeUtils.getFriendlyTimeSpanByNow(m.createdAt));
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, FavoriteDetailActivity.class);
                intent.putExtra("favorite_id", m.id);
                startActivity(intent);
            });
            
            holder.btnCancel.setOnClickListener(v -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    App.getInstance().getDatabase().favoriteDao().deleteById(m.id);
                    runOnUiThread(() -> {
                         int pos = items.indexOf(m);
                         if (pos != -1) {
                             items.remove(pos);
                             notifyItemRemoved(pos);
                         }
                    });
                });
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuestion, tvAnswer, tvTime;
            ImageView btnCancel;
            ViewHolder(View v) {
                super(v);
                tvQuestion = v.findViewById(R.id.tv_question);
                tvAnswer = v.findViewById(R.id.tv_answer);
                tvTime = v.findViewById(R.id.tv_time);
                btnCancel = v.findViewById(R.id.btn_cancel_favorite);
            }
        }
    }
}
