package com.edu.neu.finalhomework.config;

/**
 * 全局开关配置：Debug 模式、LogTag 等
 */
public class AppConfig {
    
    public static final boolean DEBUG = true;
    public static final String LOG_TAG = "FinalHomework";
    
    // 是否启用日志
    public static final boolean ENABLE_LOG = DEBUG;
    
    // 是否启用性能监控
    public static final boolean ENABLE_PERFORMANCE_MONITOR = DEBUG;
}
