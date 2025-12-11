package com.edu.neu.finalhomework.service.callback;

/**
 * AI 流式输出回调接口
 */
public interface StreamCallback {
    
    /**
     * 接收到新的文本片段
     */
    void onChunk(String chunk);
    
    /**
     * 接收到深度思考内容
     */
    void onDeepThink(String deepThink);
    
    /**
     * 流式输出完成
     */
    void onComplete();
    
    /**
     * 发生错误
     */
    void onError(String error, Throwable t);
}
