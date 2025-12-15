package com.edu.neu.finalhomework.utils;

/**
 * 简单的 Token 估算工具
 * 规则：以字符数/4 作为粗略估计（常见 BPE 经验），并提供空文本保护
 */
public class TokenUtils {

    /**
     * 估算文本 token 数量
     * @param text 输入文本
     * @return 估算 token 数量
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        // 粗略估算：字符数 / 4
        return Math.max(1, text.length() / 4);
    }
}

