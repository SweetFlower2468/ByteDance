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

import com.edu.neu.finalhomework.utils.ToastUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 模型管理 Activity
 * 对应 activity_model_manager.xml
 */
public class ModelManagerActivity extends BaseActivity {
    
    private RecyclerView recyclerModels;
    private ModelListAdapter adapter;
    private ImageView btnBack;
    private FloatingActionButton fabAdd;
    private List<LocalModel> modelList; // 模型列表（持有以便刷新）
    
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
        
        // 统计模型目录大小
        File modelDir = getExternalFilesDir("models");
        long modelSize = getFolderSize(modelDir);
        int modelCount = (modelDir != null && modelDir.exists()) ? (modelDir.listFiles() != null ? modelDir.listFiles().length : 0) : 0;
        
        // 更新界面展示
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
                if (model.downloadUrl == null || model.downloadUrl.isEmpty()) {
                    ToastUtils.show(ModelManagerActivity.this, "无下载地址");
                    return;
                }
                
                model.status = LocalModel.Status.DOWNLOADING;
                model.downloadProgress = 0;
                updateModelInDb(model);
                adapter.notifyDataSetChanged();
                ToastUtils.show(ModelManagerActivity.this, "开始下载 " + model.name);
                
                // 实际下载逻辑
                new Thread(() -> {
                    OkHttpClient client = new OkHttpClient();
                    
                    // 准备保存文件的目录与路径
                    File dir = getExternalFilesDir("models");
                    if (!dir.exists()) dir.mkdirs();
                    
                    File file;
                    if (model.localPath != null && !model.localPath.isEmpty()) {
                        file = new File(model.localPath);
                    } else {
                        file = new File(dir, model.name + ".gguf");
                        model.localPath = file.getAbsolutePath(); // 写回本地路径
                    }
                    
                    long existingLength = 0;
                    if (file.exists()) {
                        existingLength = file.length();
                    }
                    
                    Request.Builder reqBuilder = new Request.Builder().url(model.downloadUrl);
                    if (existingLength > 0) {
                        reqBuilder.header("Range", "bytes=" + existingLength + "-");
                    }
                    Request request = reqBuilder.build();
                    
                    try (Response response = client.newCall(request).execute()) {
                        // 处理 416/403：服务端不接受 Range 或链接限制
                        // 若携带 Range 失败则改为全量重试
                        if (response.code() == 416 || (response.code() == 403 && existingLength > 0)) {
                            // 关闭当前响应
                            response.close();
                            
                            // 全量重下
                            if (file.exists()) file.delete();
                            
                            Request retryRequest = new Request.Builder().url(model.downloadUrl).build();
                            try (Response retryResponse = client.newCall(retryRequest).execute()) {
                                handleDownloadResponse(retryResponse, file, model, totalBytes -> {});
                            }
                            return;
                        }

                        if (!response.isSuccessful()) {
                            if (response.code() == 403) {
                                // 调试用日志，记录完整响应
                                android.util.Log.e("DownloadDebug", "403 Forbidden in main request. " + response.toString());
                                throw new IOException("Access Forbidden (403)");
                            }
                            throw new IOException("Unexpected code " + response);
                        }
                        
                        handleDownloadResponse(response, file, model, totalBytes -> {});
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 记录异常信息
                        android.util.Log.e("DownloadDebug", "Download failed", e);
                        
                        model.status = LocalModel.Status.PAUSED; // 失败后置为暂停以便重试
                        // downloadProgress 保留
                        updateModelInDb(model);
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            // 简化提示文案
                            ToastUtils.show(ModelManagerActivity.this, "下载失败");
                        });
                    }
                }).start();
            }

            @Override
            public void onPauseClick(LocalModel model) {
                model.status = LocalModel.Status.PAUSED;
                updateModelInDb(model); // 更新数据库
                adapter.notifyDataSetChanged();
                ToastUtils.show(ModelManagerActivity.this, "已暂停");
            }
            
            @Override
            public void onResumeClick(LocalModel model) {
                // 恢复逻辑暂等同于重新下载；若要断点续传需进一步完善 Range 处理，这里先视为重试
                onDownloadClick(model);
            }

            @Override
            public void onItemClick(LocalModel model) {
                if (model.status == LocalModel.Status.READY || model.status == LocalModel.Status.ACTIVE) {
                     ToastUtils.show(ModelManagerActivity.this, "切换至 " + model.name);
                     
                     Executors.newSingleThreadExecutor().execute(() -> {
                         // 更新数据库状态
                         App.getInstance().getDatabase().modelDao().changeStatus(LocalModel.Status.ACTIVE, LocalModel.Status.READY);
                         model.status = LocalModel.Status.ACTIVE;
                         App.getInstance().getDatabase().modelDao().updateModel(model);
                         
                         // 重新加载列表
                         loadModelsFromDb();
                     });
                }
            }

            @Override
            public void onDeleteClick(LocalModel model) {
                confirmDeleteModel(model);
            }

            @Override
            public void onEditClick(LocalModel model) {
                ImportModelDialogFragment dialog = new ImportModelDialogFragment();
                dialog.setEditModel(model);
                dialog.setOnModelImportListener(new ImportModelDialogFragment.OnModelImportListener() {
                    @Override
                    public void onImport(LocalModel updatedModel) {
                        updateModelInDb(updatedModel);
                        runOnUiThread(() -> {
                            loadModelsFromDb(); // 重新加载以反映修改
                            ToastUtils.show(ModelManagerActivity.this, "模型已更新");
                        });
                    }

                    @Override
                    public void onDelete(LocalModel model) {
                        confirmDeleteModel(model);
                    }
                });
                dialog.show(getSupportFragmentManager(), "EditModelDialog");
            }
        });
        
        recyclerModels.setAdapter(adapter);
        loadModelsFromDb();
    }
    
    private void confirmDeleteModel(LocalModel model) {
        boolean hasUrl = (model.downloadUrl != null && !model.downloadUrl.isEmpty());
        File file = (model.localPath != null) ? new File(model.localPath) : null;
        boolean fileExists = (file != null && file.exists());
        
        String message;
        if (hasUrl && fileExists) {
            message = "删除已下载的模型文件？\n（记录将保留，可重新下载）";
        } else {
            message = "彻底删除此模型记录？";
        }

        new AlertDialog.Builder(ModelManagerActivity.this)
            .setTitle("删除模型")
            .setMessage(message)
            .setPositiveButton("删除", (dialog, which) -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (hasUrl && fileExists) {
                        // 情形1：删除已下载文件并重置状态
                        if (file.delete()) {
                            model.status = LocalModel.Status.NOT_DOWNLOADED;
                            model.downloadProgress = 0;
                            App.getInstance().getDatabase().modelDao().updateModel(model);
                            runOnUiThread(() -> {
                                ToastUtils.show(ModelManagerActivity.this, "已删除模型文件");
                                loadModelsFromDb(); // 刷新界面
                                updateStorageInfo();
                            });
                        } else {
                                runOnUiThread(() -> ToastUtils.show(ModelManagerActivity.this, "删除文件失败"));
                        }
                    } else {
                        // 删除记录
                        if (file != null && file.exists()) file.delete(); 
                        App.getInstance().getDatabase().modelDao().deleteModel(model);
                        runOnUiThread(() -> {
                            ToastUtils.show(ModelManagerActivity.this, "已删除模型记录");
                            loadModelsFromDb();
                            updateStorageInfo();
                        });
                    }
                });
            })
            .setNegativeButton("取消", null)
            .show();
    }

    
    private void updateModelInDb(LocalModel model) {
        Executors.newSingleThreadExecutor().execute(() -> {
            App.getInstance().getDatabase().modelDao().updateModel(model);
        });
    }

    private void insertModelToDb(LocalModel model) {
        Executors.newSingleThreadExecutor().execute(() -> {
            App.getInstance().getDatabase().modelDao().insertModel(model);
            runOnUiThread(() -> {
                loadModelsFromDb();
                ToastUtils.show(ModelManagerActivity.this, "添加成功");
            });
        });
    }

    private void loadModelsFromDb() {
        Executors.newSingleThreadExecutor().execute(() -> {
            android.content.SharedPreferences sp = getSharedPreferences("app_config", android.content.Context.MODE_PRIVATE);
            boolean isInit = sp.getBoolean("models_initialized", false);
            
            if (!isInit) {
                List<LocalModel> dbList = App.getInstance().getDatabase().modelDao().getAllModels();
                List<LocalModel> configList = ModelConfig.getBuiltInModels();
                
                // 仅首次初始化内置模型
                for (LocalModel configModel : configList) {
                    boolean exists = false;
                    for (LocalModel dbModel : dbList) {
                        if (dbModel.name.equals(configModel.name) && dbModel.version.equals(configModel.version)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        App.getInstance().getDatabase().modelDao().insertModel(configModel);
                    }
                }
                sp.edit().putBoolean("models_initialized", true).apply();
            }
            
            List<LocalModel> finalDbList = App.getInstance().getDatabase().modelDao().getAllModels();
            runOnUiThread(() -> {
                modelList.clear();
                modelList.addAll(finalDbList);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void insertDefaultModels() {
        // 已废弃，改用 loadModelsFromDb 中的同步初始化逻辑
    }
    
    private void createDummyModelFile(String name) {
        File dir = getExternalFilesDir("models");
        if (dir != null && !dir.exists()) dir.mkdirs();
        File file = new File(dir, name);
        try {
            // 写入 10MB 占位数据以模拟模型大小
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
            dialog.setOnModelImportListener(new ImportModelDialogFragment.OnModelImportListener() {
                @Override
                public void onImport(LocalModel model) {
                    // 若 localPath 为 content URI，则拷贝到应用私有目录
                    if (model.isLocal && model.localPath != null && model.localPath.startsWith("content://")) {
                         ToastUtils.show(ModelManagerActivity.this, "正在导入模型文件...");
                         new Thread(() -> {
                             try {
                                 android.net.Uri uri = android.net.Uri.parse(model.localPath);
                                 String filename = model.name + ".gguf";
                                 File destDir = getExternalFilesDir("models");
                                 if (!destDir.exists()) destDir.mkdirs();
                                 File destFile = new File(destDir, filename);
                                 
                                 InputStream is = getContentResolver().openInputStream(uri);
                                 if (is == null) throw new IOException("Cannot open input stream");
                                 
                                 FileOutputStream fos = new FileOutputStream(destFile);
                                 byte[] buffer = new byte[8192];
                                 int read;
                                 while ((read = is.read(buffer)) != -1) {
                                     fos.write(buffer, 0, read);
                                 }
                                 fos.flush();
                                 fos.close();
                                 is.close();
                                 
                                 model.localPath = destFile.getAbsolutePath();
                                 model.status = LocalModel.Status.READY;
                                 model.downloadProgress = 100;
                                 
                                 insertModelToDb(model);
                                 
                             } catch (Exception e) {
                                 runOnUiThread(() -> ToastUtils.show(ModelManagerActivity.this, "导入失败: " + e.getMessage()));
                             }
                         }).start();
                    } else {
                        insertModelToDb(model);
                    }
                }

                @Override
                public void onDelete(LocalModel model) {
                    // 新建模型的删除按钮本不应出现，此处占位不做处理
                }
            });
            dialog.show(getSupportFragmentManager(), "ImportModelDialog");
        });
        
        // 搜索功能
        android.widget.EditText etSearch = findViewById(R.id.et_search);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterModels(s.toString());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }
    
    private void handleDownloadResponse(Response response, File file, LocalModel model, java.util.function.Consumer<Long> progressCallback) throws IOException {
        if (!response.isSuccessful()) {
            if (response.code() == 403) {
                // 记录 403 详细响应便于排查
                android.util.Log.e("DownloadDebug", "403 Forbidden Error. Details: " + response.toString());
                throw new IOException("链接已失效或禁止访问 (403)");
            }
            throw new IOException("Unexpected code " + response);
        }
        
        long totalBytes = response.body().contentLength();
        boolean isResume = (response.code() == 206);
        long existingLength = isResume ? file.length() : 0;
        
        if (isResume) {
            totalBytes += existingLength;
        }
        
        InputStream is = response.body().byteStream();
        
        // 仅在 206 续传时使用追加写
        FileOutputStream fos = new FileOutputStream(file, isResume);
        
        byte[] buffer = new byte[8192];
        int read;
        long downloadedBytes = existingLength;
        
        while ((read = is.read(buffer)) != -1) {
            // 检查暂停/取消状态
            if (model.status != LocalModel.Status.DOWNLOADING) {
                // 关闭流并退出
                fos.close();
                is.close();
                return; // 结束线程
            }
            
            fos.write(buffer, 0, read);
            downloadedBytes += read;
            
            if (totalBytes > 0) {
                int progress = (int) (downloadedBytes * 100 / totalBytes);
                // 仅在进度增长时刷新，降低 UI 更新频次
                if (progress > model.downloadProgress) {
                    model.downloadProgress = progress;
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            }
        }
        
        fos.flush();
        fos.close();
        is.close();
        
        // 下载成功收尾
        model.status = LocalModel.Status.READY;
        model.downloadProgress = 100;
        updateModelInDb(model);
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            updateStorageInfo();
            ToastUtils.show(ModelManagerActivity.this, "下载完成");
        });
    }
    
    private void filterModels(String query) {
        if (adapter == null) return;
        
        List<LocalModel> filteredList = new ArrayList<>();
        
        // 在数据库中查询过滤
        Executors.newSingleThreadExecutor().execute(() -> {
            List<LocalModel> allModels = App.getInstance().getDatabase().modelDao().getAllModels();
            if (query == null || query.isEmpty()) {
                filteredList.addAll(allModels);
            } else {
                String lowerQuery = query.toLowerCase();
                for (LocalModel m : allModels) {
                    if (m.name.toLowerCase().contains(lowerQuery) || 
                        (m.description != null && m.description.toLowerCase().contains(lowerQuery))) {
                        filteredList.add(m);
                    }
                }
            }
            
            runOnUiThread(() -> {
                modelList.clear();
                modelList.addAll(filteredList);
                adapter.notifyDataSetChanged();
            });
        });
    }
}
