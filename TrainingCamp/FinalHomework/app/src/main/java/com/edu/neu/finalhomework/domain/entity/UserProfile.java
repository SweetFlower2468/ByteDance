package com.edu.neu.finalhomework.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 用户资料实体
 */
@Entity(tableName = "user_profile")
public class UserProfile {
    
    @PrimaryKey
    public long id = 1; // 单例，固定 ID
    
    // 用户昵称
    public String nickname;
    
    // 头像路径
    public String avatarPath;
    
    // 个性签名
    public String signature;
    
    // 字体大小偏好
    public float fontSizeScale;
    
    // 背景主题
    public String backgroundTheme;
    
    // 语音设置
    public String voiceSettings;
    
    // 更新时间
    public long updateTime;
    
    public UserProfile() {
        this.id = 1;
        this.fontSizeScale = 1.0f;
        this.updateTime = System.currentTimeMillis();
    }
}
