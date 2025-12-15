package com.edu.neu.finalhomework.activity.model;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.transition.TransitionManager;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

import com.edu.neu.finalhomework.utils.ToastUtils;

public class ImportModelDialogFragment extends DialogFragment {

    private OnModelImportListener listener;
    private View layoutLocal, layoutNetwork;
    private EditText etLocalName, etLocalPath;
    private EditText etNetUrl, etNetName, etNetModelId, etNetKey;
    private AutoCompleteTextView dropdownProvider;
    private MaterialSwitch switchDeepThink, switchVision;
    private MaterialSwitch switchLocalDeepThink; // 新增本地深度思考开关（UI 隐藏）
    
    private boolean isLocalMode = false;
    private ActivityResultLauncher<String> fileLauncher;
    
    private LocalModel editModel; // 正在编辑的模型

    public interface OnModelImportListener {
        void onImport(LocalModel model);
        void onDelete(LocalModel model);
    }

    public void setOnModelImportListener(OnModelImportListener listener) {
        this.listener = listener;
    }
    
    public void setEditModel(LocalModel model) {
        this.editModel = model;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                etLocalPath.setText(uri.toString());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.fragment_import_model, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggle_group_type);
        ViewGroup containerForm = view.findViewById(R.id.container_form);
        layoutLocal = view.findViewById(R.id.layout_local);
        layoutNetwork = view.findViewById(R.id.layout_network);
        
        etLocalName = view.findViewById(R.id.et_local_name);
        etLocalPath = view.findViewById(R.id.et_local_path);
        View btnBrowse = view.findViewById(R.id.btn_browse_file);
        View btnScan = view.findViewById(R.id.btn_scan_dir);
        
        etNetUrl = view.findViewById(R.id.et_net_url);
        etNetName = view.findViewById(R.id.et_net_name);
        etNetModelId = view.findViewById(R.id.et_net_model_id);
        etNetKey = view.findViewById(R.id.et_net_key);
        dropdownProvider = view.findViewById(R.id.dropdown_provider);
        switchDeepThink = view.findViewById(R.id.switch_deep_think);
        switchVision = view.findViewById(R.id.switch_vision);
        switchLocalDeepThink = view.findViewById(R.id.switch_local_deep_think);
        
        // 配置下拉选择模型供应商
        String[] providers = new String[]{"Doubao (Volcano Engine)", "OpenAI", "DeepSeek"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, providers);
        dropdownProvider.setAdapter(adapter);
        dropdownProvider.setText(providers[0], false); // 默认选择 Doubao
        
        Button btnImport = view.findViewById(R.id.btn_import);
        
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean newIsLocal = (checkedId == R.id.btn_type_local);
                if (isLocalMode != newIsLocal) {
                    isLocalMode = newIsLocal;
                    TransitionManager.beginDelayedTransition(containerForm);
                    layoutLocal.setVisibility(isLocalMode ? View.VISIBLE : View.GONE);
                    layoutNetwork.setVisibility(isLocalMode ? View.GONE : View.VISIBLE);
                }
            }
        });
        
        btnBrowse.setOnClickListener(v -> fileLauncher.launch("*/*"));
        btnScan.setOnClickListener(v -> scanAppDirectories());
        
        View btnDelete = view.findViewById(R.id.btn_delete);
        
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        btnImport.setOnClickListener(v -> handleImport());
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (listener != null && editModel != null) {
                    listener.onDelete(editModel);
                    dismiss();
                }
            });
        }
        
        // 编辑模式预填
        if (editModel != null) {
            btnImport.setText("保存修改");
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            isLocalMode = editModel.isLocal;
            
            // 预填表单
            if (isLocalMode) {
                toggleGroup.check(R.id.btn_type_local);
                layoutLocal.setVisibility(View.VISIBLE); // 显示本地表单
                layoutNetwork.setVisibility(View.GONE); // 隐藏网络表单
                etLocalName.setText(editModel.name);
                // 本地模型禁用深度思考（隐藏开关已在布局中处理）
                
                // 若有下载 URL 显示 URL，否则展示本地路径
                if (editModel.downloadUrl != null && !editModel.downloadUrl.isEmpty()) {
                    etLocalPath.setText(editModel.downloadUrl);
                } else {
                    etLocalPath.setText(editModel.localPath);
                }
            } else {
                toggleGroup.check(R.id.btn_type_network);
                etNetUrl.setText(editModel.apiUrl);
                etNetName.setText(editModel.name);
                etNetModelId.setText(editModel.version);
                etNetKey.setText(editModel.apiKey);
                switchDeepThink.setChecked(editModel.isDeepThink);
                switchVision.setChecked(editModel.isVision);
                
                // 回填供应商显示文案
                String p = editModel.provider;
                if (p != null) {
                    if (p.equalsIgnoreCase("openai")) dropdownProvider.setText("OpenAI", false);
                    else if (p.equalsIgnoreCase("deepseek")) dropdownProvider.setText("DeepSeek", false);
                    else dropdownProvider.setText("Doubao (Volcano Engine)", false);
                }
            }
            
            // 编辑模式禁用类型切换
            for (int i = 0; i < toggleGroup.getChildCount(); i++) {
                toggleGroup.getChildAt(i).setEnabled(false);
            }
        }
    }
    
    private void scanAppDirectories() {
        new Thread(() -> {
            java.util.List<java.io.File> foundFiles = new java.util.ArrayList<>();
            
            // 1. 扫描内部存储 /files 目录
            java.io.File internalFiles = requireContext().getFilesDir();
            scanDirForGGUF(internalFiles, foundFiles);
            
            // 2. 扫描外部私有 /files 目录
            java.io.File externalFiles = requireContext().getExternalFilesDir(null);
            scanDirForGGUF(externalFiles, foundFiles);
            
            // 3. 扫描外部私有 /files/models 目录
            java.io.File modelFiles = requireContext().getExternalFilesDir("models");
            scanDirForGGUF(modelFiles, foundFiles);
            
            if (foundFiles.isEmpty()) {
                requireActivity().runOnUiThread(() -> 
                    ToastUtils.show(getContext(), "未在应用目录下找到 .gguf 模型文件")
                );
                return;
            }
            
            requireActivity().runOnUiThread(() -> showFileSelectionDialog(foundFiles));
        }).start();
    }
    
    private void scanDirForGGUF(java.io.File dir, java.util.List<java.io.File> results) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        
        for (java.io.File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".gguf")) {
                // 通过绝对路径去重
                boolean exists = false;
                for (java.io.File existing : results) {
                    if (existing.getAbsolutePath().equals(f.getAbsolutePath())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) results.add(f);
            }
        }
    }
    
    private void showFileSelectionDialog(java.util.List<java.io.File> files) {
        String[] fileNames = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            fileNames[i] = files.get(i).getName();
        }
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("选择已发现的模型文件")
            .setItems(fileNames, (dialog, which) -> {
                java.io.File selected = files.get(which);
                etLocalPath.setText(selected.getAbsolutePath());
                // 若名称为空则自动填充
                if (etLocalName.getText().toString().isEmpty()) {
                    String name = selected.getName();
                    if (name.toLowerCase().endsWith(".gguf")) {
                        name = name.substring(0, name.length() - 5);
                    }
                    etLocalName.setText(name);
                }
                ToastUtils.show(getContext(), "已选择: " + selected.getName());
            })
            .show();
    }

    private void handleImport() {
        // 新建或复用待编辑模型实例
        LocalModel model = (editModel != null) ? editModel : new LocalModel();
        
        if (editModel == null) {
            model.lastUseTime = System.currentTimeMillis();
            model.downloadProgress = 100;
            model.status = LocalModel.Status.READY;
        }
        
        if (isLocalMode) {
            String name = etLocalName.getText().toString().trim();
            String pathOrUrl = etLocalPath.getText().toString().trim();
            
            if (name.isEmpty() || pathOrUrl.isEmpty()) {
                ToastUtils.show(getContext(), "请完善信息");
                return;
            }
            
            model.name = name;
            model.isLocal = true;
            model.isDeepThink = false; // 本地模型不支持深度思考           
            // 判断是否为 URL
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                model.downloadUrl = pathOrUrl;
                // 计算目标本地路径
                String fileName = name.endsWith(".gguf") ? name : name + ".gguf";
                java.io.File modelDir = requireContext().getExternalFilesDir("models");
                if (modelDir != null) {
                    model.localPath = new java.io.File(modelDir, fileName).getAbsolutePath();
                } else {
                    model.localPath = pathOrUrl; // 兜底
                }
                
                // 新增或更换 URL 时重置状态
                if (editModel == null || !pathOrUrl.equals(editModel.downloadUrl)) {
                    model.status = LocalModel.Status.NOT_DOWNLOADED;
                    model.downloadProgress = 0;
                }
            } else {
                model.localPath = pathOrUrl;
                model.downloadUrl = null;
                // 本地文件视为就绪
                if (editModel == null || !pathOrUrl.equals(editModel.localPath)) {
                    model.status = LocalModel.Status.READY;
                    model.downloadProgress = 100;
                }
            }

            if (editModel == null) {
                model.version = "Local Import";
                model.sizeDisplay = "Unknown";
                model.params = "Unknown";
                model.quantization = "Unknown";
            }
            
        } else {
            String url = etNetUrl.getText().toString().trim();
            String name = etNetName.getText().toString().trim();
            String modelId = etNetModelId.getText().toString().trim();
            String key = etNetKey.getText().toString().trim();
            
            if (url.isEmpty() || name.isEmpty() || key.isEmpty()) {
                ToastUtils.show(getContext(), "请完善信息");
                return;
            }
            
            model.name = name;
            model.apiUrl = url;
            model.apiKey = key;
            model.version = modelId.isEmpty() ? name : modelId; // 优先用填写的 ID，否则回退为名称
            model.isLocal = false;
            model.isDeepThink = switchDeepThink.isChecked();
            model.isVision = switchVision.isChecked();
            
            // 将下拉显示文案映射为内部 provider 代号
            String displayProvider = dropdownProvider.getText().toString();
            if (displayProvider.contains("OpenAI")) model.provider = "openai";
            else if (displayProvider.contains("DeepSeek")) model.provider = "deepseek";
            else model.provider = "doubao";
            
            if (editModel == null) {
                // model.version 已在上方设置
                model.sizeDisplay = "API";
                model.params = "API";
                model.quantization = "API";
            }
        }
        
        if (listener != null) {
            listener.onImport(model);
        }
        dismiss();
    }
}
