package com.edu.neu.finalhomework.service;

import com.edu.neu.finalhomework.domain.entity.Message;
import com.edu.neu.finalhomework.service.callback.StreamCallback;
import java.util.List;

/**
 * 聊天服务
 * 核心功能：发送消息、流式接收、深度思考解析
 */
public class ChatService {
    
    private static ChatService instance;
    
    private ChatService() {
    }
    
    public static ChatService getInstance() {
        if (instance == null) {
            synchronized (ChatService.class) {
                if (instance == null) {
                    instance = new ChatService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 发送消息
     * @param message 消息对象
     * @param callback 流式回调
     */
    public void sendMessage(Message message, StreamCallback callback) {
        // TODO: 实现消息发送逻辑
    }
    
    /**
     * 发送带附件的消息
     * @param message 消息对象
     * @param attachments 附件列表
     * @param callback 流式回调
     */
    public void sendMessageWithAttachments(Message message, List<com.edu.neu.finalhomework.domain.entity.Attachment> attachments, StreamCallback callback) {
        // TODO: 实现带附件的消息发送逻辑
    }
    
    /**
     * 取消当前请求
     */
    public void cancelRequest() {
        // TODO: 实现取消逻辑
    }
}
