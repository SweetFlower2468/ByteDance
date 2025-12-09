package com.edu.neu.finalhomework.config;

/**
 * API 配置
 * API Key、BaseUrl 等
 */
public class ApiConfig {
    
    // API Base URL
    public static final String BASE_URL = "https://api.example.com/";
    
    // API Key（实际使用时应该从安全存储读取）
    public static final String API_KEY = "your_api_key_here";
    
    // 请求超时时间（秒）
    public static final int TIMEOUT_SECONDS = 30;
    
    // 流式响应超时时间（秒）
    public static final int STREAM_TIMEOUT_SECONDS = 60;
}
