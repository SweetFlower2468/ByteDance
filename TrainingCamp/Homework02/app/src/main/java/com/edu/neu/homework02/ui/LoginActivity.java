package com.edu.neu.homework02.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.edu.neu.homework02.R;
import com.edu.neu.homework02.helper.Constants;
import com.edu.neu.homework02.helper.SharedPreferencesHelper;
import com.edu.neu.homework02.helper.ThirdPartyLoginHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText emailText, passwordText;
    private SharedPreferencesHelper spHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        spHelper = SharedPreferencesHelper.getInstance(this);
        initView();
        checkLastLogin();
    }

    private void initView() {
        emailText = findViewById(R.id.email);
        passwordText = findViewById(R.id.password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.register);
        TextView tvForgot = findViewById(R.id.forgot_pwd);
        LinearLayout wechatLogin = findViewById(R.id.wechat_login);
        ImageView wechatIcon = findViewById(R.id.wechat_icon);
        // Apple登录按钮
        LinearLayout appleLogin = findViewById(R.id.apple_login);
        ImageView appleIcon = findViewById(R.id.apple_icon);
        // 登录点击
        btnLogin.setOnClickListener(v -> {
            String email = emailText.getText().toString().trim();
            String pwd = passwordText.getText().toString().trim();

            if (spHelper.validateLogin(email, pwd)) {
                // 登录成功，记录本次信息方便下次快捷登录
                spHelper.saveLastLoginInfo(email, pwd);
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();

                // 跳转到个人中心
                Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                intent.putExtra(Constants.EXTRA_USER_EMAIL, email);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
            }
        });

        // 跳转注册
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // 跳转忘记密码
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // 第三方登录点击
        wechatLogin.setOnClickListener(v -> ThirdPartyLoginHelper.loginWeChat(this, null));
        appleLogin.setOnClickListener(v -> ThirdPartyLoginHelper.loginApple(this, null));
        wechatLogin.setOnClickListener(v -> ThirdPartyLoginHelper.loginWeChat(this, null));
        appleLogin.setOnClickListener(v -> ThirdPartyLoginHelper.loginApple(this, null));
    }

    private void checkLastLogin() {
        String lastEmail = spHelper.getLastLoginEmail();
        String lastPwd = spHelper.getLastLoginPassword();
        if (!lastEmail.isEmpty()) {
            emailText.setText(lastEmail);
            passwordText.setText(lastPwd);
        }
    }
}