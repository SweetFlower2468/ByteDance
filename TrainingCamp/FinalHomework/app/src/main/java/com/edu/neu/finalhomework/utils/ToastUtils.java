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

        // Cancel previous toast if it exists
        if (currentToast != null) {
            currentToast.cancel();
        }

        // Inflate custom layout
        View layout = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null);

        // Set text
        TextView textView = layout.findViewById(R.id.toast_text);
        textView.setText(message);

        // Set icon via Glide
        ImageView imageView = layout.findViewById(R.id.toast_icon);
        Glide.with(context.getApplicationContext())
                .load(ICON_URL)
                .circleCrop()
                .placeholder(R.drawable.ic_logo_doubao) // Fallback to new app logo
                .error(R.drawable.ic_logo_doubao) // Fallback if load fails
                .into(imageView);

        // Create and show toast
        currentToast = new Toast(context.getApplicationContext());
        currentToast.setDuration(Toast.LENGTH_SHORT);
        currentToast.setView(layout);
        // Set gravity AFTER setView to ensure it's treated as a custom toast
        currentToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100); 
        currentToast.show();
    }
}