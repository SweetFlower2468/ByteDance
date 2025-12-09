package com.edu.neu.finalhomework.domain.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import java.util.List;

/**
 * 本地模型数据访问接口
 */
@Dao
public interface ModelDao {
    
    @Query("SELECT * FROM local_models ORDER BY lastUseTime DESC")
    List<LocalModel> getAllModels();
    
    @Query("SELECT * FROM local_models WHERE status = :status LIMIT 1")
    LocalModel getActiveModel(LocalModel.Status status);
    
    @Query("SELECT * FROM local_models WHERE id = :id")
    LocalModel getModelById(long id);
    
    @Insert
    long insertModel(LocalModel model);
    
    @Update
    void updateModel(LocalModel model);
    
    @Delete
    void deleteModel(LocalModel model);
    
    @Query("UPDATE local_models SET status = :newStatus WHERE status = :oldStatus")
    void changeStatus(LocalModel.Status oldStatus, LocalModel.Status newStatus);
    
    @Query("UPDATE local_models SET status = :status WHERE id = :id")
    void updateModelStatus(long id, LocalModel.Status status);
}
