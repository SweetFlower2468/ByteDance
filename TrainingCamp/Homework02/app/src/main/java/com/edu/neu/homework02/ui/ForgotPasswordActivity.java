package com.edu.neu.homework02.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.neu.homework02.R;
import com.edu.neu.homework02.helper.CaptchaHelper;
import com.edu.neu.homework02.helper.SharedPreferencesHelper;
import com.edu.neu.homework02.helper.ToastUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    private String currentCaptchaCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        EditText emailText = findViewById(R.id.find_email);
        EditText captchaText = findViewById(R.id.find_captcha);
        Button getCaptchaBtn = findViewById(R.id.get_find_captcha_btn);
        Button confirmBtn = findViewById(R.id.fine_confirm_btn);

        getCaptchaBtn.setOnClickListener(v -> {
            currentCaptchaCode = CaptchaHelper.generateCaptcha();
            ToastUtils.showCaptchaToast(this, currentCaptchaCode);
        });

        confirmBtn.setOnClickListener(v -> {
            String emailStr = emailText.getText().toString().trim();
            String code = captchaText.getText().toString().trim();

            if (TextUtils.isEmpty(emailStr) || !emailStr.contains("@")) {
                Toast.makeText(this, "请输入正确的邮箱", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!code.equalsIgnoreCase(currentCaptchaCode)) {
                Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show();
                return;
            }

            if (SharedPreferencesHelper.getInstance(this).isUserExist(emailStr)) {
                // 模拟发送
                Toast.makeText(this, "密码已发送至您的邮箱，请查收", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "该账号未注册", Toast.LENGTH_SHORT).show();
            }
        });
    }
}