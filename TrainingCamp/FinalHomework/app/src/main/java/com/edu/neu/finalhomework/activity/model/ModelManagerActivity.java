package com.edu.neu.finalhomework.activity.model;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.model.adapter.ModelListAdapter;
import com.edu.neu.finalhomework.config.ModelConfig;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import com.edu.neu.finalhomework.App;

import android.os.Environment;
import android.os.StatFs;
import java.io.File;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 模型管理 Activity
 * 对应 activity_model_manager.xml
 */
public class ModelManagerActivity extends BaseActivity {
    
    private RecyclerView recyclerModels;
    private ModelListAdapter adapter;
    private ImageView btnBack;
    private FloatingActionButton fabAdd;
    private View btnFilter;
    private List<LocalModel> modelList; // Added field
    
    private TextView tvStorageUsage, tvModelCount, tvFreeSpace;
    private ProgressBar pbStorage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_manager);
        
        initViews();
        initData();
        initListeners();
    }
    
    private void initViews() {
        recyclerModels = findViewById(R.id.recycler_models);
        recyclerModels.setLayoutManager(new LinearLayoutManager(this));
        
        btnBack = findViewById(R.id.btn_back);
        fabAdd = findViewById(R.id.fab_add_model);
        btnFilter = findViewById(R.id.btn_filter);
        
        tvStorageUsage = findViewById(R.id.tv_storage_usage);
        tvModelCount = findViewById(R.id.tv_model_count);
        tvFreeSpace = findViewById(R.id.tv_free_space);
        pbStorage = findViewById(R.id.pb_storage);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStorageInfo();
    }

    private void updateStorageInfo() {
        File dataDir = Environment.getDataDirectory();
        StatFs stat = new StatFs(dataDir.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        
        long totalSpace = totalBlocks * blockSize;
        long freeSpace = availableBlocks * blockSize;
        long usedSpace = totalSpace - freeSpace;
        
        // Calculate model folder size
        File modelDir = getExternalFilesDir("models");
        long modelSize = getFolderSize(modelDir);
        int modelCount = (modelDir != null && modelDir.exists()) ? (modelDir.listFiles() != null ? modelDir.listFiles().length : 0) : 0;
        
        // Update UI
        String usedStr = formatSize(usedSpace);
        String totalStr = formatSize(totalSpace);
        String freeStr = formatSize(freeSpace);
        
        if (tvStorageUsage != null) tvStorageUsage.setText("本地存储 " + usedStr + " / " + totalStr);
        if (tvFreeSpace != null) tvFreeSpace.setText(freeStr + " 可用");
        if (tvModelCount != null) tvModelCount.setText(modelCount + " 个已下载模型 (" + formatSize(modelSize) + ")");
        
        if (pbStorage != null) {
            pbStorage.setMax(100);
            pbStorage.setProgress((int) ((usedSpace * 100) / totalSpace));
        }
    }
    
    private long getFolderSize(File file) {
        long size = 0;
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        size += getFolderSize(child);
                    }
                }
            } else {
                size = file.length();
            }
        }
        return size;
    }
    
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
    
    private void initData() {
        modelList = new ArrayList<>();
        adapter = new ModelListAdapter(modelList, new ModelListAdapter.OnModelActionListener() {
            @Override
            public void onDownloadClick(LocalModel model) {
                model.status = LocalModel.Status.DOWNLOADING;
                model.downloadProgress = 0;
                adapter.notifyDataSetChanged();
                updateModelInDb(model); // Update DB
                Toast.makeText(ModelManagerActivity.this, "开始下载: " + model.name, Toast.LENGTH_SHORT).show();
                
                // Mock download progress
                new Thread(() -> {
                    try {
                        for (int i = 0; i <= 100; i += 5) {
                            if (model.status != LocalModel.Status.DOWNLOADING) break;
                            Thread.sleep(200);
                            int finalI = i;
                            model.downloadProgress = finalI;
                            runOnUiThread(() -> adapter.notifyDataSetChanged());
                        }
                        if (model.status == LocalModel.Status.DOWNLOADING) {
                            model.status = LocalModel.Status.READY;
                            updateModelInDb(model); // Update DB
                            createDummyModelFile(model.name);
                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                updateStorageInfo();
                            });
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onPauseClick(LocalModel model) {
                model.status = LocalModel.Status.PAUSED;
                updateModelInDb(model); // Update DB
                adapter.notifyDataSetChanged();
                Toast.makeText(ModelManagerActivity.this, "已暂停", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onResumeClick(LocalModel model) {
                model.status = LocalModel.Status.DOWNLOADING;
                updateModelInDb(model); // Update DB
                adapter.notifyDataSetChanged();
                Toast.makeText(ModelManagerActivity.this, "继续下载", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemClick(LocalModel model) {
                if (model.status == LocalModel.Status.READY || model.status == LocalModel.Status.ACTIVE) {
                     Toast.makeText(ModelManagerActivity.this, "切换到: " + model.name, Toast.LENGTH_SHORT).show();
                     
                     Executors.newSingleThreadExecutor().execute(() -> {
                         // Update DB
                         App.getInstance().getDatabase().modelDao().changeStatus(LocalModel.Status.ACTIVE, LocalModel.Status.READY);
                         model.status = LocalModel.Status.ACTIVE;
                         App.getInstance().getDatabase().modelDao().updateModel(model);
                         
                         // Reload list
                         loadModelsFromDb();
                     });
                }
            }

            @Override
            public void onDeleteClick(LocalModel model) {
                new AlertDialog.Builder(ModelManagerActivity.this)
                    .setTitle("删除模型")
                    .setMessage("确定要删除 " + model.name + " 吗？" + (model.isBuiltIn ? "内置模型将重置为未下载状态。" : "这将从设备中彻底移除该模型。"))
                    .setPositiveButton("删除", (dialog, which) -> {
                        deleteModelFile(model.name);
                        
                        Executors.newSingleThreadExecutor().execute(() -> {
                            if (model.isBuiltIn) {
                                // Built-in model: Reset status
                                model.status = LocalModel.Status.NOT_DOWNLOADED;
                                model.downloadProgress = 0;
                                App.getInstance().getDatabase().modelDao().updateModel(model);
                            } else {
                                // Imported model: Delete completely
                                App.getInstance().getDatabase().modelDao().deleteModel(model);
                            }
                            loadModelsFromDb();
                        });
                        
                        updateStorageInfo();
                        Toast.makeText(ModelManagerActivity.this, "已删除模型", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });
        
        recyclerModels.setAdapter(adapter);
        loadModelsFromDb();
    }
    
    private void updateModelInDb(LocalModel model) {
        Executors.newSingleThreadExecutor().execute(() -> {
            App.getInstance().getDatabase().modelDao().updateModel(model);
        });
    }

    private void loadModelsFromDb() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<LocalModel> dbList = App.getInstance().getDatabase().modelDao().getAllModels();
            List<LocalModel> configList = ModelConfig.getBuiltInModels();
            
            // Check for missing built-in models
            boolean changed = false;
            for (LocalModel configModel : configList) {
                boolean exists = false;
                for (LocalModel dbModel : dbList) {
                    // Check by name and version
                    if (dbModel.name.equals(configModel.name) && dbModel.version.equals(configModel.version)) {
                        exists = true;
                        // Optional: update metadata if config changed (not implemented here to preserve user status)
                        // If we want to strictly enforce built-in flag:
                        if (!dbModel.isBuiltIn) {
                            dbModel.isBuiltIn = true;
                            App.getInstance().getDatabase().modelDao().updateModel(dbModel);
                        }
                        break;
                    }
                }
                
                if (!exists) {
                    App.getInstance().getDatabase().modelDao().insertModel(configModel);
                    changed = true;
                }
            }
            
            if (changed) {
                dbList = App.getInstance().getDatabase().modelDao().getAllModels();
            }
            
            List<LocalModel> finalDbList = dbList;
            runOnUiThread(() -> {
                modelList.clear();
                modelList.addAll(finalDbList);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void insertDefaultModels() {
        // Deprecated, replaced by loadModelsFromDb sync logic
    }
    
    private void createDummyModelFile(String name) {
        File dir = getExternalFilesDir("models");
        if (dir != null && !dir.exists()) dir.mkdirs();
        File file = new File(dir, name);
        try {
            // Write 10MB dummy data to simulate model size
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            byte[] buffer = new byte[1024]; // 1KB
            for (int i=0; i<10240; i++) { // 10MB
                fos.write(buffer);
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteModelFile(String name) {
        File dir = getExternalFilesDir("models");
        if (dir != null) {
            File file = new File(dir, name);
            if (file.exists()) {
                file.delete();
            }
        }
    }
    
    private void initListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        fabAdd.setOnClickListener(v -> {
            ImportModelDialogFragment dialog = new ImportModelDialogFragment();
            dialog.setOnModelImportListener(model -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    long id = App.getInstance().getDatabase().modelDao().insertModel(model);
                    model.id = id;
                    runOnUiThread(() -> {
                        loadModelsFromDb();
                        updateStorageInfo();
                        Toast.makeText(this, "已添加模型: " + model.name, Toast.LENGTH_SHORT).show();
                    });
                });
            });
            dialog.show(getSupportFragmentManager(), "ImportModelDialog");
        });
        
        btnFilter.setOnClickListener(v -> Toast.makeText(this, "筛选", Toast.LENGTH_SHORT).show());
    }
}
