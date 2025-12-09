package com.edu.neu.finalhomework.domain.dao;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.edu.neu.finalhomework.domain.entity.Feedback;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.edu.neu.finalhomework.domain.entity.Message;
import com.edu.neu.finalhomework.domain.entity.Session;
import com.edu.neu.finalhomework.domain.entity.UserProfile;

/**
 * Room 数据库入口
 */
@Database(
    entities = {
        Message.class,
        Session.class,
        LocalModel.class,
        UserProfile.class,
        Feedback.class
    },
    version = 5,
    exportSchema = false
)
@TypeConverters({MessageTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract MessageDao messageDao();
    public abstract SessionDao sessionDao();
    public abstract ModelDao modelDao();
    public abstract FeedbackDao feedbackDao();
}
