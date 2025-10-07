package com.ugelcorongo.edugestin360.storage;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO para la cola de tareas pendientes (envíos offline).
 */
@Dao
public interface PendingTaskDao {

    /** Inserta una tarea (PDF, IMG o ATTENDANCE). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PendingTaskEntity task);

    /** Obtiene todas las tareas pendientes en la base. */
    @Query("SELECT * FROM pending_tasks")
    List<PendingTaskEntity> getAll();

    /** Elimina una tarea tras el envío exitoso. */
    @Delete
    void delete(PendingTaskEntity task);
}