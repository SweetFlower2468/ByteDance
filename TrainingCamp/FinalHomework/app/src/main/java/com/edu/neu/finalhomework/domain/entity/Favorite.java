package com.edu.neu.finalhomework.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.edu.neu.finalhomework.domain.dao.MessageTypeConverter;
import java.util.List;

@Entity(tableName = "favorites")
@TypeConverters({MessageTypeConverter.class})
public class Favorite {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long messageId;     // 被点击收藏的消息ID
    public Long userMessageId; // 关联的用户问题ID
    public Long aiMessageId;   // 关联的 AI 回复ID
    public String userContent; // 用户问题快照
    public String aiContent;   // AI 回复快照
    public List<Attachment> userAttachments; // 用户附件快照
    public String userQuotedContent;         // 用户引用内容
    public Long userQuotedMessageId;         // 用户引用消息ID
    public long createdAt;     // 收藏时间
}

