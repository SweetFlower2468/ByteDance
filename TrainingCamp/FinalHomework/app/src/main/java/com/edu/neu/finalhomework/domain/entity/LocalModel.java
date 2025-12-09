package com.edu.neu.finalhomework.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.List;
import java.util.ArrayList;

/**
 * 本地模型信息实体
 */
@Entity(tableName = "local_models")
public class LocalModel {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    // Model Identity
    public String name;       // e.g., "Llama 3.2"
    public String version;    // e.g., "3B-Instruct"
    
    // Metadata
    public String sizeDisplay;     // e.g., "2.1 GB"
    public String params;          // e.g., "3B"
    public String quantization;    // e.g., "Q4_K_M"
    
    // Status
    public Status status;
    public int downloadProgress;   // 0-100
    
    // Capabilities
    public boolean isVision;       // If true, show vision icon
    
    // API / Network Config
    public String apiUrl;
    public String apiKey;
    public boolean isDeepThink;
    public boolean isLocal = true; // Default true (downloaded or imported file)
    public boolean isBuiltIn = false; // Is it a built-in configuration model
    public String localPath;       // Absolute path for imported local models
    
    public String description;
    public long lastUseTime;

    public enum Status {
        NOT_DOWNLOADED,
        DOWNLOADING,
        PAUSED,
        READY,
        ACTIVE
    }
    
    public LocalModel() {
        this.status = Status.NOT_DOWNLOADED;
        this.downloadProgress = 0;
        this.lastUseTime = System.currentTimeMillis();
    }
    
    // Constructor for Mock Data convenience
    public LocalModel(String name, String version, String sizeDisplay, String params, String quantization, Status status, int progress, boolean isVision) {
        this.name = name;
        this.version = version;
        this.sizeDisplay = sizeDisplay;
        this.params = params;
        this.quantization = quantization;
        this.status = status;
        this.downloadProgress = progress;
        this.isVision = isVision;
        this.lastUseTime = System.currentTimeMillis();
    }
}
