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
import com.edu.neu.finalhomework.domain.entity.Message;
import com.edu.neu.finalhomework.utils.TimeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FavoritesActivity extends BaseActivity {

    private EditText etSearch;
    private RecyclerView recyclerFavorites;
    private FavoriteAdapter adapter;
    
    // Pagination
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
        // If coming back from detail, favorite status might have changed?
        // But DetailActivity is read-only so status won't change unless we allow it there.
        // User said "click in ... just browse ... cannot delete ...". 
        // So list should be stable. No need to reload onResume unless strict consistency needed.
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
            try { Thread.sleep(1000); } catch (InterruptedException e) {} 
            
            List<Message> newItems = App.getInstance().getDatabase().messageDao()
                    .getFavoriteMessagesBefore(lastTimestamp, LimitConfig.FAVORITES_PAGE_SIZE);
            
            runOnUiThread(() -> {
                if (newItems.isEmpty()) {
                    isLastPage = true;
                } else {
                    adapter.appendItems(newItems);
                    lastTimestamp = newItems.get(newItems.size() - 1).timestamp;
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
            List<Message> results = App.getInstance().getDatabase().messageDao().searchFavoriteMessages(keyword);
            runOnUiThread(() -> {
                adapter.setItems(results);
            });
        });
    }
    
    class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {
        List<Message> items = new ArrayList<>();
        
        void setItems(List<Message> items) {
            this.items = new ArrayList<>(items);
            notifyDataSetChanged();
        }
        
        void appendItems(List<Message> newItems) {
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
            Message m = items.get(position);
            holder.tvContent.setText(m.content);
            holder.tvTime.setText(TimeUtils.getFriendlyTimeSpanByNow(m.timestamp));
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, FavoriteDetailActivity.class);
                intent.putExtra("message_id", m.id);
                startActivity(intent);
            });
            
            holder.btnCancel.setOnClickListener(v -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    App.getInstance().getDatabase().messageDao().updateFavoriteStatus(m.id, false);
                    m.isFavorite = false;
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
            TextView tvContent, tvTime;
            ImageView btnCancel;
            ViewHolder(View v) {
                super(v);
                tvContent = v.findViewById(R.id.tv_content);
                tvTime = v.findViewById(R.id.tv_time);
                btnCancel = v.findViewById(R.id.btn_cancel_favorite);
            }
        }
    }
}