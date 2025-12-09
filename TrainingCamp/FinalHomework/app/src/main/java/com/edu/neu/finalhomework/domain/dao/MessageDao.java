package com.edu.neu.finalhomework.domain.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.edu.neu.finalhomework.domain.entity.Message;
import java.util.List;

/**
 * 消息数据访问接口
 */
@Dao
public interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<Message> getMessagesBySession(long sessionId);

    @Query("SELECT * FROM (SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp < :lastTimestamp ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    List<Message> getMessagesBySessionBefore(long sessionId, long lastTimestamp, int limit);

    @Query("SELECT * FROM (SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    List<Message> getLatestMessagesBySession(long sessionId, int limit);
    
    @Query("SELECT * FROM messages WHERE id = :id")
    Message getMessageById(long id);
    
    @Query("SELECT * FROM messages WHERE isFavorite = 1 ORDER BY timestamp DESC")
    List<Message> getFavoriteMessages();

    @Query("SELECT * FROM messages WHERE isFavorite = 1 ORDER BY timestamp DESC LIMIT :limit")
    List<Message> getLatestFavoriteMessages(int limit);

    @Query("SELECT * FROM messages WHERE isFavorite = 1 AND timestamp < :lastTimestamp ORDER BY timestamp DESC LIMIT :limit")
    List<Message> getFavoriteMessagesBefore(long lastTimestamp, int limit);

    @Query("SELECT * FROM messages WHERE isFavorite = 1 AND content LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    List<Message> searchFavoriteMessages(String keyword);
    
    @Insert
    long insertMessage(Message message);
    
    @Insert
    void insertMessages(List<Message> messages);
    
    @Update
    void updateMessage(Message message);
    
    @Delete
    void deleteMessage(Message message);
    
    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    void deleteMessagesBySession(long sessionId);
    
    @Query("UPDATE messages SET isFavorite = :isFavorite WHERE id = :id")
    void updateFavoriteStatus(long id, boolean isFavorite);

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp < :timestamp ORDER BY timestamp DESC LIMIT 1")
    Message getPreviousMessage(long sessionId, long timestamp);

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp > :timestamp ORDER BY timestamp ASC LIMIT 1")
    Message getNextMessage(long sessionId, long timestamp);

    @Query("SELECT id FROM messages WHERE isFavorite = 1")
    List<Long> getFavoriteMessageIds();
}
