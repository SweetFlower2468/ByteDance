package com.edu.neu.homework02.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.edu.neu.homework02.model.User;
import com.google.gson.Gson;

public class SharedPreferencesHelper {

    private static SharedPreferencesHelper instance;
    private final SharedPreferences sp;
    private final Gson gson;

    private SharedPreferencesHelper(Context context) {
        sp = context.getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // 单例模式
    public static synchronized SharedPreferencesHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 保存或更新用户信息
     */
    public void saveUser(User user) {
        if (user == null || TextUtils.isEmpty(user.getEmail())) return;
        String json = gson.toJson(user);
        // 使用 email 作为 Key 的一部分，实现多用户存储
        sp.edit().putString(Constants.KEY_USER_DATA_PREFIX + user.getEmail(), json).apply();
    }

    /**
     * 根据邮箱获取用户信息
     */
    public User getUser(String email) {
        String json = sp.getString(Constants.KEY_USER_DATA_PREFIX + email, null);
        if (json == null) return null;
        return gson.fromJson(json, User.class);
    }

    /**
     * 验证登录
     */
    public boolean validateLogin(String email, String password) {
        User user = getUser(email);
        return user != null && TextUtils.equals(user.getPassword(), password);
    }

    /**
     * 保存最后一次登录的凭证（用于快捷登录/回显）
     */
    public void saveLastLoginInfo(String email, String password) {
        sp.edit()
                .putString(Constants.KEY_LAST_LOGIN_EMAIL, email)
                .putString(Constants.KEY_LAST_LOGIN_PWD, password)
                .apply();
    }

    /**
     * 获取最后一次登录的 Email
     */
    public String getLastLoginEmail() {
        return sp.getString(Constants.KEY_LAST_LOGIN_EMAIL, "");
    }

    /**
     * 获取最后一次登录的 Password
     */
    public String getLastLoginPassword() {
        return sp.getString(Constants.KEY_LAST_LOGIN_PWD, "");
    }

    /**
     * 检查是否存在某个账号（用于注册查重）
     */
    public boolean isUserExist(String email) {
        return sp.contains(Constants.KEY_USER_DATA_PREFIX + email);
    }

}
