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
    private MaterialSwitch switchLocalDeepThink; // New Switch
    
    private boolean isLocalMode = false;
    private ActivityResultLauncher<String> fileLauncher;
    
    private LocalModel editModel; // Model being edited

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
        
        // Setup Provider Dropdown
        String[] providers = new String[]{"Doubao (Volcano Engine)", "OpenAI", "DeepSeek"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, providers);
        dropdownProvider.setAdapter(adapter);
        dropdownProvider.setText(providers[0], false); // Default
        
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
        
        // Handle Edit Mode
        if (editModel != null) {
            btnImport.setText("保存修改");
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            isLocalMode = editModel.isLocal;
            
            // Pre-fill
            if (isLocalMode) {
                toggleGroup.check(R.id.btn_type_local);
                layoutLocal.setVisibility(View.VISIBLE); // Explicitly show
                layoutNetwork.setVisibility(View.GONE); // Explicitly hide
                etLocalName.setText(editModel.name);
                // 本地模型禁用深度思考（隐藏开关已在布局中处理）
                
                // Show URL if exists, otherwise show path
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
                
                // Set Provider
                String p = editModel.provider;
                if (p != null) {
                    if (p.equalsIgnoreCase("openai")) dropdownProvider.setText("OpenAI", false);
                    else if (p.equalsIgnoreCase("deepseek")) dropdownProvider.setText("DeepSeek", false);
                    else dropdownProvider.setText("Doubao (Volcano Engine)", false);
                }
            }
            
            // Disable switching type
            for (int i = 0; i < toggleGroup.getChildCount(); i++) {
                toggleGroup.getChildAt(i).setEnabled(false);
            }
        }
    }
    
    private void scanAppDirectories() {
        new Thread(() -> {
            java.util.List<java.io.File> foundFiles = new java.util.ArrayList<>();
            
            // 1. Check getFilesDir() (Internal Storage /files)
            java.io.File internalFiles = requireContext().getFilesDir();
            scanDirForGGUF(internalFiles, foundFiles);
            
            // 2. Check getExternalFilesDir(null) (External Storage /files)
            java.io.File externalFiles = requireContext().getExternalFilesDir(null);
            scanDirForGGUF(externalFiles, foundFiles);
            
            // 3. Check getExternalFilesDir("models")
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
                // Avoid duplicates by absolute path
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
                // Auto fill name if empty
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
        // Create new or use existing
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
            
            // Check if it's a URL
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                model.downloadUrl = pathOrUrl;
                // Define target local path
                String fileName = name.endsWith(".gguf") ? name : name + ".gguf";
                java.io.File modelDir = requireContext().getExternalFilesDir("models");
                if (modelDir != null) {
                    model.localPath = new java.io.File(modelDir, fileName).getAbsolutePath();
                } else {
                    model.localPath = pathOrUrl; // Fallback
                }
                
                // If newly imported or if URL changed
                if (editModel == null || !pathOrUrl.equals(editModel.downloadUrl)) {
                    model.status = LocalModel.Status.NOT_DOWNLOADED;
                    model.downloadProgress = 0;
                }
            } else {
                model.localPath = pathOrUrl;
                model.downloadUrl = null;
                // Assume local file is ready
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
            model.version = modelId.isEmpty() ? name : modelId; // Use entered ID, or fallback to name if empty
            model.isLocal = false;
            model.isDeepThink = switchDeepThink.isChecked();
            model.isVision = switchVision.isChecked();
            
            // Map display string to code
            String displayProvider = dropdownProvider.getText().toString();
            if (displayProvider.contains("OpenAI")) model.provider = "openai";
            else if (displayProvider.contains("DeepSeek")) model.provider = "deepseek";
            else model.provider = "doubao";
            
            if (editModel == null) {
                // model.version already set above
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
