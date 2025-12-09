package com.edu.neu.finalhomework.config;

/**
 * 限制常量配置
 * 最大图片数、最大文件数等
 */
public class LimitConfig {
    
    // 最大图片数量
    public static final int MAX_IMAGES = 9;
    
    // 最大文件数量
    public static final int MAX_FILES = 3;
    
    // 单张图片最大大小（MB）
    public static final int MAX_IMAGE_SIZE_MB = 10;
    
    // 单个文件最大大小（MB）
    public static final int MAX_FILE_SIZE_MB = 50;
    
    // 消息最大长度
    public static final int MAX_MESSAGE_LENGTH = 10000;
    
    // 单次对话最大 Token 数
    public static final int MAX_TOKENS = 4096;

    // 每次加载的消息数量
    public static final int PAGE_SIZE = 2;

    // 收藏每次加载数量
    public static final int FAVORITES_PAGE_SIZE = 2;
}
