package com.edu.neu.finalhomework.domain.dao;

import androidx.room.TypeConverter;
import com.edu.neu.finalhomework.domain.entity.Attachment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Message 实体的 List<Attachment> 类型转换器
 */
public class MessageTypeConverter {
    
    private static Gson gson = new Gson();
    
    @TypeConverter
    public static String fromAttachmentList(List<Attachment> attachments) {
        if (attachments == null) {
            return null;
        }
        return gson.toJson(attachments);
    }
    
    @TypeConverter
    public static List<Attachment> toAttachmentList(String data) {
        if (data == null) {
            return null;
        }
        Type listType = new TypeToken<List<Attachment>>() {}.getType();
        return gson.fromJson(data, listType);
    }
}
