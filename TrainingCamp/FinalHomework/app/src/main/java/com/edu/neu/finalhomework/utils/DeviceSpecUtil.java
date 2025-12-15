package com.edu.neu.finalhomework.utils;

import android.app.ActivityManager;
import android.content.Context;

/**
 * 设备规格工具，用于根据内存与 CPU 动态调整本地推理参数（如 nBatch）
 */
public class DeviceSpecUtil {

    public static int recommendBatch(Context ctx) {
        int memMb = getMemoryClass(ctx);
        int cores = Runtime.getRuntime().availableProcessors();

        // 简单规则：内存越大、核心越多，batch 越大
        if (memMb >= 7000 && cores >= 6) {
            return 768;
        } else if (memMb >= 5500 && cores >= 4) {
            return 512;
        } else if (memMb >= 4000) {
            return 384;
        } else {
            return 256;
        }
    }

    private static int getMemoryClass(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                return am.getMemoryClass() * 1024; // MB -> MB (largeHeap 已在 manifest? 如果需要可�?getLargeMemoryClass)
            }
        } catch (Exception ignored) {}
        return 2048; // fallback 2GB
    }
}

