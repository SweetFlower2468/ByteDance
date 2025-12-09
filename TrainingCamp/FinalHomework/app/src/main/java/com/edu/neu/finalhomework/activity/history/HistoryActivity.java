package com.edu.neu.finalhomework.activity.history;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.history.adapter.HistoryAdapter;
import com.edu.neu.finalhomework.activity.main.ChatActivity;
import com.edu.neu.finalhomework.domain.entity.Session;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 历史会话记录页面
 * 对应 activity_history.xml
 */
public class HistoryActivity extends BaseActivity {

    private RecyclerView recyclerHistory;
    private HistoryAdapter adapter;
    private EditText etSearch;
    private ImageView btnBack;
    private TextView btnClearAll;
    private View layoutEmpty;
    
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private List<Session> allSessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initViews();
        initData();
        initListeners();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadSessions(); // Reload on resume to reflect changes from ChatActivity
    }

    private void initViews() {
        recyclerHistory = findViewById(R.id.recycler_history);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        
        etSearch = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
        btnClearAll = findViewById(R.id.btn_clear_all);
        layoutEmpty = findViewById(R.id.layout_empty);
        
        adapter = new HistoryAdapter(session -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("sessionId", session.id);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        recyclerHistory.setAdapter(adapter);
        
        setupSwipeToDelete();
    }

    private void initData() {
        loadSessions();
    }
    
    private void loadSessions() {
        executorService.execute(() -> {
            List<Session> sessions = App.getInstance().getDatabase().sessionDao().getAllSessions();
            
            List<Session> finalSessions = sessions;
            runOnUiThread(() -> {
                allSessions = finalSessions;
                updateList(allSessions);
            });
        });
    }
    

    private void updateList(List<Session> sessions) {
        adapter.submitList(new ArrayList<>(sessions)); // submit new list instance
        if (sessions.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerHistory.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerHistory.setVisibility(View.VISIBLE);
        }
    }

    private void initListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnClearAll.setOnClickListener(v -> {
            // Clear all logic
            if (allSessions.isEmpty()) return;
            
            executorService.execute(() -> {
                App.getInstance().getDatabase().sessionDao().deleteAll();
                runOnUiThread(() -> {
                    allSessions.clear();
                    updateList(allSessions);
                    Toast.makeText(this, "已清空所有记录", Toast.LENGTH_SHORT).show();
                });
            });
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSessions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterSessions(v.getText().toString());
                return true;
            }
            return false;
        });
    }
    
    private void filterSessions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            updateList(allSessions);
            return;
        }
        
        executorService.execute(() -> {
            List<Session> filtered = App.getInstance().getDatabase().sessionDao().searchSessions(keyword);
            runOnUiThread(() -> updateList(filtered));
        });
    }
    
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(0xFFE57373); // Red
            private final Drawable deleteIcon = ContextCompat.getDrawable(HistoryActivity.this, R.drawable.ic_trash_red); // Or white trash icon

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Session sessionToDelete = adapter.getSessionAt(position);
                
                // Remove from DB
                executorService.execute(() -> {
                    App.getInstance().getDatabase().sessionDao().deleteSession(sessionToDelete);
                });
                
                // Update UI
                List<Session> currentList = new ArrayList<>(adapter.getCurrentList());
                currentList.remove(position);
                updateList(currentList); // Submit list updates UI
                
                // Also update source of truth in memory if searching? 
                // Better to just reload or manage local list.
                allSessions.remove(sessionToDelete);

                // Undo Snackbar
                Snackbar.make(recyclerHistory, "已删除会话", Snackbar.LENGTH_LONG)
                        .setAction("撤销", v -> {
                            // Restore
                            executorService.execute(() -> {
                                App.getInstance().getDatabase().sessionDao().insertSession(sessionToDelete); // Re-insert might change ID if auto-gen? 
                                // Ideally we keep ID. But insertSession returns new ID.
                                // For undo, strict consistency suggests re-inserting same data.
                                // Let's just insert back.
                                runOnUiThread(() -> loadSessions());
                            });
                        }).show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                
                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20; // optional

                if (dX < 0) { // Swiping to the left
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);
                    
                    if (deleteIcon != null) {
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;

                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.setTint(Color.WHITE); // Make it white on red bg
                        deleteIcon.draw(c);
                    }
                }
            }
        };
        
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerHistory);
    }
}
