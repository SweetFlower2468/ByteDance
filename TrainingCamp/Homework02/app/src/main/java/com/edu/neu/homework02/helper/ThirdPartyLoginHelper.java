package com.edu.neu.homework02.helper;

import android.content.Context;

import com.edu.neu.homework02.R;

public class ThirdPartyLoginHelper {

    public interface LoginCallback {
        void onSuccess(String platformName);
    }

    public static void loginWeChat(Context context, LoginCallback callback) {
        // 模拟登录耗时或逻辑
        // 弹出自定义 Icon Toast
        ToastUtils.showCustomToast(context, "正在拉起微信授权...", R.drawable.we_chat); // 需自备 icon

        // 模拟成功
        if (callback != null) {
            callback.onSuccess("WeChat");
        }
    }

    public static void loginApple(Context context, LoginCallback callback) {
        ToastUtils.showCustomToast(context, "正在连接 Apple ID...", R.drawable.apple); // 需自备 icon

        if (callback != null) {
            callback.onSuccess("Apple");
        }
    }
}
