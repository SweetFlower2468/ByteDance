package com.edu.neu.finalhomework.activity.model;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ImportModelDialogFragment extends DialogFragment {

    private OnModelImportListener listener;
    private View layoutLocal, layoutNetwork;
    private EditText etLocalName, etLocalPath;
    private EditText etNetUrl, etNetName, etNetKey;
    private MaterialSwitch switchDeepThink, switchVision;
    
    private boolean isLocalMode = false;
    private ActivityResultLauncher<String> fileLauncher;

    public interface OnModelImportListener {
        void onImport(LocalModel model);
    }

    public void setOnModelImportListener(OnModelImportListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                etLocalPath.setText(uri.getPath());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Set dialog width to match parent (with some margin typically handled by theme or padding)
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
        
        etNetUrl = view.findViewById(R.id.et_net_url);
        etNetName = view.findViewById(R.id.et_net_name);
        etNetKey = view.findViewById(R.id.et_net_key);
        switchDeepThink = view.findViewById(R.id.switch_deep_think);
        switchVision = view.findViewById(R.id.switch_vision);
        
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
        
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_import).setOnClickListener(v -> handleImport());
    }
    
    private void handleImport() {
        LocalModel model = new LocalModel();
        model.lastUseTime = System.currentTimeMillis();
        model.downloadProgress = 100;
        model.status = LocalModel.Status.READY;
        
        if (isLocalMode) {
            String name = etLocalName.getText().toString().trim();
            String path = etLocalPath.getText().toString().trim();
            
            if (name.isEmpty() || path.isEmpty()) {
                Toast.makeText(getContext(), "请完善信息", Toast.LENGTH_SHORT).show();
                return;
            }
            
            model.name = name;
            model.localPath = path;
            model.isLocal = true;
            model.version = "Local Import";
            model.sizeDisplay = "Unknown";
            model.params = "Unknown";
            model.quantization = "Unknown";
            
        } else {
            String url = etNetUrl.getText().toString().trim();
            String name = etNetName.getText().toString().trim();
            String key = etNetKey.getText().toString().trim();
            
            if (url.isEmpty() || name.isEmpty() || key.isEmpty()) {
                Toast.makeText(getContext(), "请完善信息", Toast.LENGTH_SHORT).show();
                return;
            }
            
            model.name = name;
            model.apiUrl = url;
            model.apiKey = key;
            model.isLocal = false;
            model.isDeepThink = switchDeepThink.isChecked();
            model.isVision = switchVision.isChecked();
            model.version = "Network API";
            model.sizeDisplay = "API";
            model.params = "API";
            model.quantization = "API";
        }
        
        if (listener != null) {
            listener.onImport(model);
        }
        dismiss();
    }
}