package com.edu.neu.finalhomework.config;

/**
 * 上下文与对话配置
 */
public class ChatConfig {
    
    // 对话上下文最大消息数 (Request payload size limit)
    public static final int MAX_CONTEXT_MESSAGES = 20 + 1;
    
    // 本地模型使用的上下文消息数（更短，避免超出本地模型上下文）
    public static final int MAX_LOCAL_CONTEXT_MESSAGES = 5;

    // 本地模型 Prompt 最大字符数（输入截断保护，近似代替 token 长度限制）
    public static final int MAX_LOCAL_PROMPT_CHARS = 20000;

    // 本地模型输出最大字符数（输出截断保护）
    public static final int MAX_LOCAL_OUTPUT_CHARS = 6000;
    
}

