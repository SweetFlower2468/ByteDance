package com.edu.neu.finalhomework.activity.profile;

import android.content.Intent;
import android.os.Bundle;
import com.bumptech.glide.Glide;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.feedback.HelpActivity;
import com.edu.neu.finalhomework.activity.profile.adapter.ProfileMenuAdapter;
import com.edu.neu.finalhomework.activity.profile.settings.BackgroundActivity;
import com.edu.neu.finalhomework.activity.profile.settings.FontSizeActivity;
import com.edu.neu.finalhomework.activity.profile.settings.VoiceSettingsActivity;
import com.edu.neu.finalhomework.utils.SPUtils;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

import com.edu.neu.finalhomework.utils.ToastUtils;

/**
 * 个人中心 Activity
 * 对应 activity_profile.xml
 */
public class ProfileActivity extends BaseActivity {
    
    private RecyclerView recyclerMenu;
    private ProfileMenuAdapter adapter;
    private MaterialButton btnEditProfile;
    private ImageView btnBack;
    private ImageView ivAvatar;
    private TextView tvNickname;
    
    // 菜单项 ID 定义
    private static final int ID_FONT = 1;
    private static final int ID_BACKGROUND = 2;
    private static final int ID_VOICE = 3;
    private static final int ID_HELP = 4;
    private static final int ID_UPDATE = 5;
    private static final int ID_ABOUT = 6;
    private static final int ID_FAVORITE = 7;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        initViews();
        initData();
        initListeners();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 返回时刷新数据（设置可能已变更）
        initData(); 
    }
    
    private void initViews() {
        recyclerMenu = findViewById(R.id.recycler_menu);
        recyclerMenu.setLayoutManager(new LinearLayoutManager(this));
        
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvNickname = findViewById(R.id.tv_nickname);
    }
    
    private void initData() {
        // 更新头像与昵称显示
        String avatar = SPUtils.getString("user_avatar", null);
        String nickname = SPUtils.getString("user_nickname", "User_Name");
        
        if (avatar != null) {
            Glide.with(this).load(Uri.parse(avatar)).into(ivAvatar);
        }
        tvNickname.setText(nickname);

        List<ProfileMenuAdapter.MenuItem> items = new ArrayList<>();
        
        // 设置分组
        items.add(new ProfileMenuAdapter.MenuItem(ID_FONT, R.drawable.ic_text_size, "字体大小", getFontSizeText()));
        items.add(new ProfileMenuAdapter.MenuItem(ID_BACKGROUND, R.drawable.ic_photos, "背景设置", SPUtils.getString("background_theme", "默认")));
        items.add(new ProfileMenuAdapter.MenuItem(ID_VOICE, R.drawable.ic_volume_high, "语音设置", "豆包·温柔女声"));
        items.add(new ProfileMenuAdapter.MenuItem(ID_FAVORITE, R.drawable.ic_star_filled, "我的收藏", null));
        
        // 支持分组
        items.add(new ProfileMenuAdapter.MenuItem(ID_HELP, R.drawable.ic_search, "帮助与反馈", null)); // 使用搜索图标作为帮助占位
        items.add(new ProfileMenuAdapter.MenuItem(ID_UPDATE, R.drawable.ic_download, "检查更新", "v1.0.0"));
        
        // 关于分组
        items.add(new ProfileMenuAdapter.MenuItem(ID_ABOUT, R.drawable.ic_file_other, "关于应用", null));

        adapter = new ProfileMenuAdapter(items, item -> {
            switch (item.id) {
                case ID_FONT:
                    startActivity(new Intent(this, FontSizeActivity.class));
                    break;
                case ID_BACKGROUND:
                    startActivity(new Intent(this, BackgroundActivity.class));
                    break;
                case ID_VOICE:
                    startActivity(new Intent(this, VoiceSettingsActivity.class));
                    break;
                case ID_FAVORITE:
                    startActivity(new Intent(this, com.edu.neu.finalhomework.activity.favorites.FavoritesActivity.class));
                    break;
                case ID_HELP:
                    startActivity(new Intent(this, HelpActivity.class));
                    break;
                case ID_UPDATE:
                    ToastUtils.show(this, "已是最新版");
                    break;
                case ID_ABOUT:
                    startActivity(new Intent(this, AboutActivity.class));
                    break;
            }
        });
        recyclerMenu.setAdapter(adapter);
    }
    
    private String getFontSizeText() {
        float scale = SPUtils.getFontSizeScale();
        if (scale == 0.85f) return "小";
        if (scale == 1.15f) return "大";
        if (scale == 1.30f) return "超大";
        return "标准";
    }
    
    private void initListeners() {
        btnEditProfile.setOnClickListener(v -> navigateToEditProfile());
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btn_more).setOnClickListener(v -> ToastUtils.show(this, "更多设置"));
    }
    
    private void navigateToEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }
}
