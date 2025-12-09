package com.edu.neu.finalhomework.activity.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.utils.SPUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

/**
 * 编辑个人资料 Activity
 * 对应 activity_edit_profile.xml
 */
public class EditProfileActivity extends BaseActivity {
    
    private ShapeableImageView ivAvatar;
    private TextInputEditText etNickname;
    private TextInputEditText etUserId;
    
    private ActivityResultLauncher<String> galleryLauncher;
    private String currentAvatarUri;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        initLaunchers();
        initViews();
        initData();
        initListeners();
    }
    
    private void initLaunchers() {
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    currentAvatarUri = uri.toString();
                    Glide.with(this).load(uri).into(ivAvatar);
                }
            }
        );
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfile());
        
        ivAvatar = findViewById(R.id.iv_avatar_preview);
        etNickname = findViewById(R.id.et_nickname);
        etUserId = findViewById(R.id.et_user_id);
        
        // User ID is read-only
        etUserId.setEnabled(false); 
    }
    
    private void initData() {
        String avatar = SPUtils.getString("user_avatar", null);
        String nickname = SPUtils.getString("user_nickname", "User_Name");
        String userId = SPUtils.getString("user_id", "842374457"); // Default ID
        
        if (avatar != null) {
            currentAvatarUri = avatar;
            Glide.with(this).load(Uri.parse(avatar)).into(ivAvatar);
        }
        etNickname.setText(nickname);
        etUserId.setText(userId);
    }
    
    private void initListeners() {
        ivAvatar.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }
    
    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SPUtils.putString("user_nickname", nickname);
        if (currentAvatarUri != null) {
            SPUtils.putString("user_avatar", currentAvatarUri);
        }
        
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        finish();
    }
}