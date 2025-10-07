package com.ugelcorongo.edugestin360.storage;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * RoomDatabase principal que expone el DAO de tareas pendientes.
 */
@Database(
        entities = { PendingTaskEntity.class },
        version  = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "attendance_db";
    private static volatile AppDatabase instance;

    public abstract PendingTaskDao pendingDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
}