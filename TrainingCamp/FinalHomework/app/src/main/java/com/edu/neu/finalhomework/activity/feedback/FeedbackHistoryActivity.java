package com.edu.neu.finalhomework.activity.feedback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.domain.entity.Feedback;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class FeedbackHistoryActivity extends BaseActivity {

    private RecyclerView recyclerHistory;
    private FeedbackAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_history);
        
        initViews();
        loadData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        recyclerHistory = findViewById(R.id.recycler_history);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeedbackAdapter();
        recyclerHistory.setAdapter(adapter);
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Feedback> list = App.getInstance().getDatabase().feedbackDao().getAllFeedback();
            runOnUiThread(() -> {
                adapter.setItems(list);
            });
        });
    }

    private static class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.ViewHolder> {
        private List<Feedback> items = new ArrayList<>();
        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        public void setItems(List<Feedback> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Feedback item = items.get(position);
            holder.tvContent.setText(item.content);
            holder.tvDate.setText(sdf.format(new Date(item.submitTime)));
            
            String statusText = "待处理";
            int colorRes = R.color.warning;
            if ("processing".equals(item.status)) {
                statusText = "处理中";
                colorRes = R.color.brand_primary;
            } else if ("resolved".equals(item.status)) {
                statusText = "已解决";
                colorRes = R.color.success;
            }
            holder.tvStatus.setText(statusText);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(colorRes, null));
            
            holder.tvType.setText(item.type == null ? "反馈" : item.type);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvType, tvDate, tvContent, tvStatus;
            ViewHolder(View v) {
                super(v);
                tvType = v.findViewById(R.id.tv_type);
                tvDate = v.findViewById(R.id.tv_date);
                tvContent = v.findViewById(R.id.tv_content);
                tvStatus = v.findViewById(R.id.tv_status);
            }
        }
    }
}