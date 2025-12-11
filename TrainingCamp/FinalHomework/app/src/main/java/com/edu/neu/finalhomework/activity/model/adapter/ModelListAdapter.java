package com.edu.neu.finalhomework.activity.model.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;

public class ModelListAdapter extends RecyclerView.Adapter<ModelListAdapter.ModelViewHolder> {
    
    private List<LocalModel> models;
    private OnModelActionListener listener;

    public interface OnModelActionListener {
        void onDownloadClick(LocalModel model);
        void onPauseClick(LocalModel model);
        void onResumeClick(LocalModel model);
        void onItemClick(LocalModel model);
        void onDeleteClick(LocalModel model);
        void onEditClick(LocalModel model);
    }
    
    public ModelListAdapter(List<LocalModel> models, OnModelActionListener listener) {
        this.models = models;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_card, parent, false);
        return new ModelViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        LocalModel model = models.get(position);
        holder.bind(model, listener);
    }
    
    @Override
    public int getItemCount() {
        return models != null ? models.size() : 0;
    }
    
    public void updateModels(List<LocalModel> models) {
        this.models = models;
        notifyDataSetChanged();
    }
    
    static class ModelViewHolder extends RecyclerView.ViewHolder {
        
        // Header
        TextView tvName, tvStatusActive, tvVersion;
        ImageButton btnDelete, btnEdit;
        ImageView ivBrain;
        
        // Meta
        TextView tvParams, tvSize, tvQuant;
        
        // Actions
        MaterialButton btnDownload;
        LinearLayout layoutDownloading;
        LinearProgressIndicator progressBar;
        TextView tvProgressPercent;
        MaterialButton btnPause;
        View layoutReady;
        TextView tvLastUsed;
        
        MaterialCardView cardView;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            
            tvName = itemView.findViewById(R.id.tv_model_name);
            tvStatusActive = itemView.findViewById(R.id.tv_status_active);
            tvVersion = itemView.findViewById(R.id.tv_model_version);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            ivBrain = itemView.findViewById(R.id.iv_brain);
            
            tvParams = itemView.findViewById(R.id.tv_meta_params);
            tvSize = itemView.findViewById(R.id.tv_meta_size);
            tvQuant = itemView.findViewById(R.id.tv_meta_quant);
            
            btnDownload = itemView.findViewById(R.id.btn_download);
            layoutDownloading = itemView.findViewById(R.id.layout_downloading);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvProgressPercent = itemView.findViewById(R.id.tv_progress_percent);
            btnPause = itemView.findViewById(R.id.btn_pause);
            layoutReady = itemView.findViewById(R.id.layout_ready);
            tvLastUsed = itemView.findViewById(R.id.tv_last_used);
        }

        public void bind(LocalModel model, OnModelActionListener listener) {
            tvName.setText(model.name);
            tvName.setTextColor(Color.BLACK); // Force black color
            tvVersion.setText(model.version);
            
            // Handle placeholders for missing metadata
            tvParams.setText(isValid(model.params) ? model.params : "--");
            tvSize.setText(isValid(model.sizeDisplay) ? model.sizeDisplay : "--");
            tvQuant.setText(isValid(model.quantization) ? model.quantization : "--");
            
            ivBrain.setVisibility(model.isDeepThink ? View.VISIBLE : View.GONE);

            // State Handling
            resetState();
            
            boolean isActive = false;
            
            switch (model.status) {
                case NOT_DOWNLOADED:
                    btnDownload.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE); // Allow delete record
                    btnDelete.setImageTintList(ColorStateList.valueOf(Color.RED)); // Ensure it's red
                    break;
                case DOWNLOADING:
                    layoutDownloading.setVisibility(View.VISIBLE);
                    progressBar.setProgress(model.downloadProgress);
                    tvProgressPercent.setText(model.downloadProgress + "%");
                    // Configure pause button
                    btnPause.setText("暂停下载");
                    btnPause.setIconResource(R.drawable.ic_pause);
                    btnPause.setOnClickListener(v -> {
                        if (listener != null) listener.onPauseClick(model);
                    });
                    btnDelete.setVisibility(View.VISIBLE); // Allow delete during download
                    btnDelete.setImageTintList(ColorStateList.valueOf(Color.RED));
                    break;
                case PAUSED:
                    layoutDownloading.setVisibility(View.VISIBLE);
                    progressBar.setProgress(model.downloadProgress);
                    tvProgressPercent.setText(model.downloadProgress + "%");
                    // Configure resume button
                    btnPause.setText("继续下载");
                    btnPause.setIconResource(R.drawable.ic_download); // Or play icon
                    btnPause.setOnClickListener(v -> {
                        if (listener != null) listener.onResumeClick(model);
                    });
                    btnDelete.setVisibility(View.VISIBLE); // Allow delete during pause
                    btnDelete.setImageTintList(ColorStateList.valueOf(Color.RED));
                    break;
                case READY:
                    layoutReady.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE); 
                    btnDelete.setImageTintList(ColorStateList.valueOf(Color.RED));
                    break;
                case ACTIVE:
                    layoutReady.setVisibility(View.VISIBLE);
                    tvStatusActive.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE);
                    btnDelete.setImageTintList(ColorStateList.valueOf(Color.RED));
                    isActive = true;
                    break;
            }
            
            // Card Style for Active
            if (isActive) {
                cardView.setStrokeColor(Color.parseColor("#4CAF50")); // Green border
                cardView.setStrokeWidth(4); // thicker
                // tvName.setTextColor(Color.BLACK); // Already set in XML
            } else {
                cardView.setStrokeColor(Color.TRANSPARENT);
                cardView.setStrokeWidth(0);
                cardView.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.surface_card));
                // tvName.setTextColor(Color.BLACK); // Already set in XML
            }
            
            // Listeners
            btnDownload.setOnClickListener(v -> {
                if (listener != null) listener.onDownloadClick(model);
            });
            
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(model);
            });
            
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onEditClick(model);
                });
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(model);
            });
        }
        
        private boolean isValid(String s) {
            return s != null && !s.isEmpty() && !"Unknown".equals(s) && !"API".equals(s);
        }
        
        private void resetState() {
            btnDownload.setVisibility(View.GONE);
            layoutDownloading.setVisibility(View.GONE);
            layoutReady.setVisibility(View.GONE);
            tvStatusActive.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }
}
