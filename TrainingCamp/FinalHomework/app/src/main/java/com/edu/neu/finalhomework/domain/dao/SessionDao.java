package com.edu.neu.finalhomework.domain.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.edu.neu.finalhomework.domain.entity.Session;
import java.util.List;

/**
 * 会话数据访问接口
 */
@Dao
public interface SessionDao {
    
    @Query("SELECT * FROM sessions ORDER BY updateTimestamp DESC")
    List<Session> getAllSessions();

    @Query("SELECT * FROM sessions ORDER BY updateTimestamp DESC LIMIT 1")
    Session getLatestSession();
    
    @Query("SELECT * FROM sessions WHERE title LIKE '%' || :keyword || '%' OR lastMessage LIKE '%' || :keyword || '%' ORDER BY updateTimestamp DESC")
    List<Session> searchSessions(String keyword);
    
    @Query("SELECT * FROM sessions WHERE id = :id")
    Session getSessionById(long id);
    
    @Insert
    long insertSession(Session session);
    
    @Update
    void updateSession(Session session);
    
    @Delete
    void deleteSession(Session session);
    
    @Query("DELETE FROM sessions WHERE id = :id")
    void deleteSessionById(long id);
    
    @Query("DELETE FROM sessions")
    void deleteAll();
}
