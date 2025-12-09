package com.edu.neu.finalhomework.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 会话记录实体
 */
@Entity(tableName = "sessions")
public class Session {
    
    @PrimaryKey(autoGenerate = true)
    public long id; // sessionId
    
    // 会话标题（自动生成或用户自定义）
    public String title;
    
    // 最后一条消息内容（用于预览）
    public String lastMessage;
    
    // 最后更新时间
    public long updateTimestamp;
    
    // 累计 Token 消耗
    public int totalTokens;
    
    // 消息数量 (Optional but useful)
    public int messageCount;
    
    public Session() {
        this.updateTimestamp = System.currentTimeMillis();
        this.totalTokens = 0;
        this.messageCount = 0;
    }
    
    // Helper for mock data
    public Session(String title, String lastMessage, long timestamp, int tokens) {
        this.title = title;
        this.lastMessage = lastMessage;
        this.updateTimestamp = timestamp;
        this.totalTokens = tokens;
        this.messageCount = 1;
    }
}
