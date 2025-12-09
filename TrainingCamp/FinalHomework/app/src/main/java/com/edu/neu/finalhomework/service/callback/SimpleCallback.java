package com.edu.neu.finalhomework.service.callback;

/**
 * 通用成功/失败回调接口
 */
public interface SimpleCallback {
    
    void onSuccess();
    
    void onFailure(String error);
}
