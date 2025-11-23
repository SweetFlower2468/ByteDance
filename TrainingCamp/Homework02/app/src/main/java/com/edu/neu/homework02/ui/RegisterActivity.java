package com.edu.neu.homework02.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.neu.homework02.R;
import com.edu.neu.homework02.model.User;
import com.edu.neu.homework02.helper.CaptchaHelper;
import com.edu.neu.homework02.helper.Constants;
import com.edu.neu.homework02.helper.SharedPreferencesHelper;
import com.edu.neu.homework02.helper.ToastUtils;

public class RegisterActivity extends AppCompatActivity {

    private EditText EmailText, PwdText, CaptchaText;
    private String currentCaptchaCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EmailText = findViewById(R.id.reg_email);
        PwdText = findViewById(R.id.reg_pwd);
        CaptchaText = findViewById(R.id.reg_captcha);
        Button getCaptchaBtn = findViewById(R.id.reg_captcha_btn);
        Button registerBtn = findViewById(R.id.register_confirm_btn);

        // 获取验证码 (需求：Toast展示)
        getCaptchaBtn.setOnClickListener(v -> {
            currentCaptchaCode = CaptchaHelper.generateCaptcha();
            // 弹出长时间Toast展示验证码
            ToastUtils.showCaptchaToast(this, currentCaptchaCode);
        });

        // 注册
        registerBtn.setOnClickListener(v -> {
            String email = EmailText.getText().toString().trim();
            String pwd = PwdText.getText().toString().trim();
            String code = CaptchaText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pwd)) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }

            // 校验验证码
            if (TextUtils.isEmpty(code) || !code.equalsIgnoreCase(currentCaptchaCode)) {
                Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferencesHelper sp = SharedPreferencesHelper.getInstance(this);
            if (sp.isUserExist(email)) {
                Toast.makeText(this, "该邮箱已注册", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建用户
            User newUser = new User(email, pwd, "用户" + CaptchaHelper.generateCaptcha(), "这个人很懒，什么都没写", "");
            sp.saveUser(newUser);
            sp.saveLastLoginInfo(email, pwd);

            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();

            // 直接进入个人中心
            Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
            intent.putExtra(Constants.EXTRA_USER_EMAIL, email);
            // 清除之前的Activity栈，防止回退到注册页
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }
}