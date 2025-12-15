package com.edu.neu.finalhomework.domain.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.edu.neu.finalhomework.domain.entity.Feedback;
import java.util.List;

@Dao
public interface FeedbackDao {
    @Insert
    void insert(Feedback feedback);

    @Query("SELECT * FROM feedback ORDER BY submitTime DESC")
    List<Feedback> getAllFeedback();
}
