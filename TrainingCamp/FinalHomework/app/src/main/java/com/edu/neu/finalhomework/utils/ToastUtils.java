package com.edu.neu.finalhomework.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.edu.neu.finalhomework.R;

public class ToastUtils {

    private static final String ICON_URL = "https://lf-flow-web-cdn.doubao.com/obj/flow-doubao/doubao/chat/static/image/logo-icon-white-bg.72df0b1a.png";
    private static Toast currentToast;

    public static void show(Context context, String message) {
        if (context == null) return;

        // 如果存在，则取消之前的toast
        if (currentToast != null) {
            currentToast.cancel();
        }

        // 展开自定义布局
        View layout = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null);

        // 设置文本
        TextView textView = layout.findViewById(R.id.toast_text);
        textView.setText(message);

        // 通过Glide设置图标
        ImageView imageView = layout.findViewById(R.id.toast_icon);
        Glide.with(context.getApplicationContext())
                .load(ICON_URL)
                .circleCrop()
                .placeholder(R.drawable.ic_logo_doubao) // 回退到新的应用程序图标
                .error(R.drawable.ic_logo_doubao) // 加载失败时回退
                .into(imageView);

        // 创建并显示toast
        currentToast = new Toast(context.getApplicationContext());
        currentToast.setDuration(Toast.LENGTH_SHORT);
        currentToast.setView(layout);
        // 在setView之后设置重心，确保它被当作自定义toast处理
        currentToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100); 
        currentToast.show();
    }
}
