package com.edu.neu.finalhomework.domain.entity;

import java.io.Serializable;

/**
 * 附件对象（文件/图片）
 */
public class Attachment implements Serializable {
    
    // 附件类型：image, file
    public String type;
    
    // 文件路径
    public String filePath;
    
    // 文件名称
    public String fileName;
    
    // 文件大小（字节）
    public long fileSize;
    
    // 显示用的大小字符
    public String displaySize;
    
    // MIME 类型
    public String mimeType;
    
    // 缩略图路径（图片专用）
    public String thumbnailPath;
    
    // Token 数量（用于计算）
    public int tokenCount;
    
    public Attachment() {
    }
    
    public Attachment(String type, String filePath, String fileName) {
        this.type = type;
        this.filePath = filePath;
        this.fileName = fileName;
    }
}
