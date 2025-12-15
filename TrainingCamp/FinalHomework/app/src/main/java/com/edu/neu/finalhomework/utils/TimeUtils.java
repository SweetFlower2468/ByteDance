package com.edu.neu.finalhomework.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间格式化工具类
 */
public class TimeUtils {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    /**
     * 格式化时间戳为日期
     */
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * 格式化时间戳为时间
     */
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }
    
    /**
    * 格式化时间戳为日期时间
    */
    public static String formatDateTime(long timestamp) {
        return DATETIME_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * 格式化相对时间（友好显示）
     * 刚刚, 10分钟, 10:30, 昨天, yyyy-MM-dd
     */
    public static String getFriendlyTimeSpanByNow(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60 * 1000) {
            return "刚刚";
        }
        if (diff < 60 * 60 * 1000) {
            return (diff / (60 * 1000)) + "分钟";
        }
        
        Date date = new Date(timestamp);
        Date nowDate = new Date(now);
        
        // Check if today (approximate by date string)
        if (DATE_FORMAT.format(date).equals(DATE_FORMAT.format(nowDate))) {
            return TIME_FORMAT.format(date);
        }
        
        // Check if yesterday (approximate logic: within 48h and different date string)
        if (diff < 48 * 60 * 60 * 1000) {
             return "昨天";
        }
        
        return DATE_FORMAT.format(date);
    }

    /**
     * 格式化相对时间（今天显示时间，昨天显示“昨天”，更早显示日期）
     */
    public static String formatRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // 今天
        if (diff < 24 * 60 * 60 * 1000) {
            return formatTime(timestamp);
        }
        
        // 昨天
        if (diff < 2 * 24 * 60 * 60 * 1000) {
            return "昨天 " + formatTime(timestamp);
        }
        
        // 更早
        return formatDate(timestamp);
    }
}
