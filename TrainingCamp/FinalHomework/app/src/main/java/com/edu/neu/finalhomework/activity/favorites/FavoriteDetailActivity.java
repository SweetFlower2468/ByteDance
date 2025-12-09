package com.edu.neu.finalhomework.activity.favorites;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.main.adapter.ChatAdapter;
import com.edu.neu.finalhomework.domain.entity.Message;
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
        
        long msgId = getIntent().getLongExtra("message_id", -1);
        if (msgId == -1) { finish(); return; }
        
        initViews();
        loadData(msgId);
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        recyclerDetail = findViewById(R.id.recycler_detail);
        recyclerDetail.setLayoutManager(new LinearLayoutManager(this));
        // Empty list initially
        chatAdapter = new ChatAdapter(new ArrayList<>());
        chatAdapter.setReadOnly(true); 
        recyclerDetail.setAdapter(chatAdapter);
    }
    
    private void loadData(long id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Message target = App.getInstance().getDatabase().messageDao().getMessageById(id);
            if (target == null) return;
            
            List<Message> displayList = new ArrayList<>();
            
            if ("ai".equals(target.type)) {
                // Find question
                Message question = App.getInstance().getDatabase().messageDao().getPreviousMessage(target.sessionId, target.timestamp);
                if (question != null) displayList.add(question);
                displayList.add(target);
            } else if ("user".equals(target.type)) {
                displayList.add(target);
                // Find answer
                Message answer = App.getInstance().getDatabase().messageDao().getNextMessage(target.sessionId, target.timestamp);
                if (answer != null) displayList.add(answer);
            } else {
                displayList.add(target);
            }
            
            runOnUiThread(() -> {
                chatAdapter.updateMessages(displayList);
            });
        });
    }
}