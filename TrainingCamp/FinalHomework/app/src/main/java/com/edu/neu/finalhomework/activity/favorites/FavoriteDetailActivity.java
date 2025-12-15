package com.edu.neu.finalhomework.activity.favorites;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.main.adapter.ChatAdapter;
import com.edu.neu.finalhomework.domain.entity.Message;
import com.edu.neu.finalhomework.domain.entity.Favorite;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class FavoriteDetailActivity extends BaseActivity {
    
    private RecyclerView recyclerDetail;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_detail);
        
        long favId = getIntent().getLongExtra("favorite_id", -1);
        if (favId == -1) { finish(); return; }
        
        initViews();
        loadData(favId);
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        recyclerDetail = findViewById(R.id.recycler_detail);
        recyclerDetail.setLayoutManager(new LinearLayoutManager(this));
        // 初始为空列表
        chatAdapter = new ChatAdapter(new ArrayList<>());
        chatAdapter.setReadOnly(true); 
        recyclerDetail.setAdapter(chatAdapter);
    }
    
    private void loadData(long id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Favorite fav = App.getInstance().getDatabase().favoriteDao().getById(id);
            if (fav == null) return;

            List<Message> displayList = new ArrayList<>();
            if (fav.userContent != null) {
                Message q = new Message(); // 构造用户消息
                q.type = "user";
                q.content = fav.userContent;
                q.timestamp = fav.createdAt - 1;
                q.attachments = fav.userAttachments;
                q.quotedContent = fav.userQuotedContent;
                q.quotedMessageId = fav.userQuotedMessageId;
                displayList.add(q);
            }
            if (fav.aiContent != null) {
                Message a = new Message(); // 构造 AI 回复消息
                a.type = "ai";
                a.content = fav.aiContent;
                a.timestamp = fav.createdAt;
                displayList.add(a);
            }

            runOnUiThread(() -> chatAdapter.updateMessages(displayList));
        });
    }
}
