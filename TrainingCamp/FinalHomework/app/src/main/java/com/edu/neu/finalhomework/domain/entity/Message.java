package com.edu.neu.finalhomework.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.edu.neu.finalhomework.domain.dao.MessageTypeConverter;
import java.util.List;

/**
 * 聊天消息实体
 * 包含 deepThink 字段
 */
@Entity(tableName = "messages")
@TypeConverters({MessageTypeConverter.class})
public class Message {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    // 会话ID
    public long sessionId;
    
    // 消息类型：user, ai, system
    public String type;
    
    // 消息内容
    public String content;
    
    // 深度思考内容（AI消息特有）
    public String deepThink;
    
    // 附件列表（图片、文件）
    public List<Attachment> attachments;
    
    // 引用的消息ID（用于回复）
    public Long quotedMessageId;
    
    // 引用内容（显示用）
    public String quotedContent;
    
    // 时间戳
    public long timestamp;
    
    // 是否已收藏
    public boolean isFavorite;
    
    // 是否正在发送
    public boolean isSending;
    
    // 是否发送失败
    public boolean isFailed;
    
    // 是否点赞
    public boolean isLiked;
    
    // 是否点踩
    public boolean isDisliked;
    
    // 是否正在生成 (用于显示 Loading 动画)
    public boolean isGenerating;
    
    // UI State: 是否展开深度思考
    public boolean isDeepThinkExpanded = true;
    
    public Message() {
        this.timestamp = System.currentTimeMillis();
        this.isFavorite = false;
        this.isSending = false;
        this.isFailed = false;
        this.isLiked = false;
        this.isDisliked = false;
    }
}
