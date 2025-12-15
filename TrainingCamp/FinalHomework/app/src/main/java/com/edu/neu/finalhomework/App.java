package com.edu.neu.finalhomework;

import android.app.Application;
import androidx.room.Room;
import com.edu.neu.finalhomework.domain.dao.AppDatabase;
import com.edu.neu.finalhomework.service.LlamaService;
import com.edu.neu.finalhomework.utils.SPUtils;

/**
 * 全局 Application
 * 初始化 Room 数据库、SharedPreferences、Service 单例
 */
public class App extends Application {
    
    private static App instance;
    private AppDatabase database;
    
    // Service 单例
    private LlamaService llamaService;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(getApplicationContext());
        
        // 初始化数据库
        database = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "finalhomework_db"
        )
        // 允许在主线程查询（仅用于调试，生产环境应禁用）
        .allowMainThreadQueries()
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
    
    public LlamaService getLlamaService() {
        if (llamaService == null) {
            llamaService = LlamaService.getInstance();
        }
        return llamaService;
    }
}
