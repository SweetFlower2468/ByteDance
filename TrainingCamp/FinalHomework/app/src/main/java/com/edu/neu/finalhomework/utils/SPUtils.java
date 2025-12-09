package com.edu.neu.finalhomework.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences 工具类
 * 存取字体、背景、设置等
 */
public class SPUtils {
    
    private static final String SP_NAME = "finalhomework_prefs";
    private static SharedPreferences sp;
    
    public static void init(Context context) {
        sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }
    
    // 字体大小
    public static void setFontSizeScale(float scale) {
        sp.edit().putFloat("font_size_scale", scale).apply();
    }
    
    public static float getFontSizeScale() {
        return sp.getFloat("font_size_scale", 1.0f);
    }
    
    // 背景主题
    public static void setBackgroundTheme(String theme) {
        sp.edit().putString("background_theme", theme).apply();
    }
    
    public static String getBackgroundTheme() {
        return sp.getString("background_theme", "default");
    }
    
    // 语音设置
    public static void setVoiceSettings(String settings) {
        sp.edit().putString("voice_settings", settings).apply();
    }
    
    public static String getVoiceSettings() {
        return sp.getString("voice_settings", "");
    }
    
    // 通用方法
    public static void putString(String key, String value) {
        sp.edit().putString(key, value).apply();
    }
    
    public static String getString(String key, String defaultValue) {
        return sp.getString(key, defaultValue);
    }
    
    public static void putInt(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }
    
    public static int getInt(String key, int defaultValue) {
        return sp.getInt(key, defaultValue);
    }
    
    public static void putBoolean(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        return sp.getBoolean(key, defaultValue);
    }
    
    public static void putFloat(String key, float value) {
        sp.edit().putFloat(key, value).apply();
    }
    
    public static float getFloat(String key, float defaultValue) {
        return sp.getFloat(key, defaultValue);
    }
}
