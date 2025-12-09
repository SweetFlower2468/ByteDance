package com.edu.neu.finalhomework.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 反馈记录实体
 */
@Entity(tableName = "feedback")
public class Feedback {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    // 反馈类型：bug, suggestion, question
    public String type;
    
    // 反馈标题
    public String title;
    
    // 反馈内容
    public String content;
    
    // 附件路径（截图等）
    public String attachmentPath;
    
    // 联系方式
    public String contact;
    
    // 提交时间
    public long submitTime;
    
    // 处理状态：pending, processing, resolved
    public String status;
    
    // 回复内容
    public String reply;
    
    public Feedback() {
        this.submitTime = System.currentTimeMillis();
        this.status = "pending";
    }
}
