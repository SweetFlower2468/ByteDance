package com.edu.neu.finalhomework.domain.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.edu.neu.finalhomework.domain.entity.Favorite;
import java.util.List;

@Dao
public interface FavoriteDao {

    @Insert
    long insert(Favorite favorite);

    @Delete
    void delete(Favorite favorite);

    @Query("DELETE FROM favorites WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM favorites WHERE id = (SELECT id FROM favorites WHERE messageId = :messageId ORDER BY createdAt DESC LIMIT 1)")
    void deleteLatestByMessageId(long messageId);

    @Query("SELECT * FROM (SELECT * FROM favorites WHERE createdAt < :before ORDER BY createdAt DESC LIMIT :limit) ORDER BY createdAt DESC")
    List<Favorite> getFavoritesBefore(long before, int limit);

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC LIMIT :limit")
    List<Favorite> getLatestFavorites(int limit);

    @Query("SELECT * FROM favorites WHERE (userContent LIKE '%' || :keyword || '%' OR aiContent LIKE '%' || :keyword || '%') ORDER BY createdAt DESC")
    List<Favorite> searchFavorites(String keyword);

    @Query("SELECT * FROM favorites WHERE id = :id")
    Favorite getById(long id);
}

