package com.ugelcorongo.edugestin360.storage;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para cola de tareas pendientes (envíos offline).
 */
@Entity(tableName = "pending_tasks")
public class PendingTaskEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "type")
    private String type;

    /** URI en formato String (puede quedar vacío para registros sin archivo) */
    @ColumnInfo(name = "file_uri")
    private String fileUri;

    /** JSON con metadatos: colegio, docente, gps, etc */
    @ColumnInfo(name = "meta_json")
    private String metaJson;

    public PendingTaskEntity(String type, String fileUri, String metaJson) {
        this.type     = type;
        this.fileUri  = fileUri;
        this.metaJson = metaJson;
    }

    // Getters & Setters

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getFileUri() {
        return fileUri;
    }
    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String getMetaJson() {
        return metaJson;
    }
    public void setMetaJson(String metaJson) {
        this.metaJson = metaJson;
    }
}