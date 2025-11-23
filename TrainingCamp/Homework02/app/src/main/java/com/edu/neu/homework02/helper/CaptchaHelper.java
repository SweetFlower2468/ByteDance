package com.edu.neu.homework02.helper;

import java.util.Random;

public class CaptchaHelper {
    /**
     * 生成 4 位随机数字/字母验证码
     */
    public static String generateCaptcha() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
