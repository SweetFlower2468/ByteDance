package com.edu.neu.finalhomework.config;

/**
 * API 配置
 * API Key、BaseUrl 等
 */
public class ApiConfig {
    
    // API基础URL
    public static final String BASE_URL = "https://api.example.com/";
    
    // API密钥（实际使用时应该从安全存储读取）
    public static final String API_KEY = "your_api_key_here";
    
    // 请求超时时间（秒）
    public static final int TIMEOUT_SECONDS = 90;
    
    // 流式响应超时时间（秒）
    public static final int STREAM_TIMEOUT_SECONDS = 120;

    // TTS配置
    public static final String TTS_APP_ID = "YOUR_TTS_APP_ID"; // 替换为你的火山引擎应用ID
    public static final String TTS_TOKEN = "YOUR_TTS_TOKEN";   // 替换为你的火山引擎令牌
}
