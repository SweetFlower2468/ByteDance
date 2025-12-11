package com.edu.neu.finalhomework.config;

/**
 * 手势与传感器配置
 */
public class HandConfig {
    // 侧躺判断阈值
    public static final float GRAVITY_SIDE_LYING_THRESHOLD = 8.8f;
    
    // 正常倾斜判断阈值
    public static final float GRAVITY_TILT_THRESHOLD = 2.0f;
    
    // 重力传感器刷新阈值
    public static final float GRAVITY_CHANGE_THRESHOLD = 2.0f;
    
    // 错误判定次数阈值 (Touch override)
    public static final int TOUCH_OVERRIDE_COUNT = 2;
}





