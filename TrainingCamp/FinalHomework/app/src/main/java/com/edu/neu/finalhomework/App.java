package com.edu.neu.finalhomework;

import android.app.Application;
import androidx.room.Room;
import com.edu.neu.finalhomework.domain.dao.AppDatabase;
import com.edu.neu.finalhomework.service.ChatService;
import com.edu.neu.finalhomework.service.LlamaService;
import com.edu.neu.finalhomework.service.SensorService;
import com.edu.neu.finalhomework.service.FileService;
import com.edu.neu.finalhomework.service.TtsService;
import com.edu.neu.finalhomework.service.UpdateService;
import com.edu.neu.finalhomework.utils.SPUtils;

/**
 * 全局 Application
 * 初始化 Room 数据库、SharedPreferences、Service 单例
 */
public class App extends Application {
    
    private static App instance;
    private AppDatabase database;
    
    // Service 单例
    private ChatService chatService;
    private LlamaService llamaService;
    private SensorService sensorService;
    private FileService fileService;
    private TtsService ttsService;
    private UpdateService updateService;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化数据库
        database = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "finalhomework_db"
        )
        .fallbackToDestructiveMigration()
        .build();
        
        // 初始化 SharedPreferences
        SPUtils.init(this);
        
        // 初始化 Service 单例（延迟初始化，按需创建）
    }
    
    public static App getInstance() {
        return instance;
    }
    
    public AppDatabase getDatabase() {
        return database;
    }
    
    public ChatService getChatService() {
        if (chatService == null) {
            chatService = ChatService.getInstance();
        }
        return chatService;
    }
    
    public LlamaService getLlamaService() {
        if (llamaService == null) {
            llamaService = LlamaService.getInstance();
        }
        return llamaService;
    }
    
    public SensorService getSensorService() {
        if (sensorService == null) {
            sensorService = SensorService.getInstance();
        }
        return sensorService;
    }
    
    public FileService getFileService() {
        if (fileService == null) {
            fileService = FileService.getInstance();
        }
        return fileService;
    }
    
    public TtsService getTtsService() {
        if (ttsService == null) {
            ttsService = TtsService.getInstance();
        }
        return ttsService;
    }
    
    public UpdateService getUpdateService() {
        if (updateService == null) {
            updateService = UpdateService.getInstance();
        }
        return updateService;
    }
}
