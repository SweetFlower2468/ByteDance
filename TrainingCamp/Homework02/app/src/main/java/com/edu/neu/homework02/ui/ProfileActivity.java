package com.edu.neu.homework02.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.neu.homework02.R;
import com.edu.neu.homework02.adapter.ProfileMenuAdapter;
import com.edu.neu.homework02.model.MenuItem;
import com.edu.neu.homework02.model.User;
import com.edu.neu.homework02.helper.Constants;
import com.edu.neu.homework02.helper.SharedPreferencesHelper;
import com.edu.neu.homework02.helper.ToastUtils;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String email = getIntent().getStringExtra(Constants.EXTRA_USER_EMAIL);
        if (email != null) {
            currentUser = SharedPreferencesHelper.getInstance(this).getUser(email);
        }

        initView();
    }

    private void initView() {
        CardView AvatarView = findViewById(R.id.avatar);
        TextView NameView = findViewById(R.id.username);
        TextView SignView = findViewById(R.id.sign);

        if (currentUser != null) {
            NameView.setText(currentUser.getUsername());
            SignView.setText(currentUser.getSignature());
        }

        // 1. 准备数据：
        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem("个人信息", android.R.drawable.ic_menu_myplaces));
        menuItems.add(new MenuItem("我的收藏", R.drawable.favourite_file));
        menuItems.add(new MenuItem("浏览历史", android.R.drawable.ic_menu_recent_history));
        menuItems.add(new MenuItem("设置", android.R.drawable.ic_menu_preferences));
        menuItems.add(new MenuItem("关于我们", android.R.drawable.ic_menu_info_details));
        menuItems.add(new MenuItem("意见反馈", android.R.drawable.ic_menu_help));
        // 添加退出登录项
        menuItems.add(new MenuItem("退出登录", android.R.drawable.ic_lock_power_off));

        // 2. 设置 Adapter
        RecyclerView recyclerView = findViewById(R.id.recycler_view_profile);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ProfileMenuAdapter adapter = new ProfileMenuAdapter(this, menuItems);
        recyclerView.setAdapter(adapter);

        // 3. 处理点击事件
        adapter.setOnItemClickListener(item -> {
            if ("退出登录".equals(item.getTitle())) {
                showLogoutDialog();
            } else {
                ToastUtils.showCustomToast(ProfileActivity.this, "点击了: " + item.getTitle(), item.getIconResId());
            }
        });
    }

    // 弹窗逻辑保持不变
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}