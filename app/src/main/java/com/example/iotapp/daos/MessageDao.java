package com.example.iotapp.daos;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
import com.example.iotapp.models.Message;

@Dao
public interface MessageDao {
    @Insert
    void insert(Message message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<Message> getAllMessages();
}
