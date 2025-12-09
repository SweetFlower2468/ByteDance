package com.edu.neu.finalhomework.service.callback;

/**
 * 左右手变化回调接口
 */
public interface SensorCallback {
    
    /**
     * 检测到左右手变化
     * @param isLeftHand true 表示左手，false 表示右手
     */
    void onHandChanged(boolean isLeftHand);
    
    /**
     * 检测失败
     */
    void onError(String error);
}
