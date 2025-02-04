package com.example.iotapp.database;

import androidx.room.RoomDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.iotapp.daos.CoordinateDao;
import com.example.iotapp.models.Coordinates;
import com.example.iotapp.models.Message;
import com.example.iotapp.daos.MessageDao;

@Database(entities = {Message.class, Coordinates.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract MessageDao messageDao();
    public abstract CoordinateDao coordinateDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "message_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
