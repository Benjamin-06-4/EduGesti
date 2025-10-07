package com.ugelcorongo.edugestin360.ui.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.google.gson.Gson;
import com.ugelcorongo.edugestin360.storage.AppDatabase;
import com.ugelcorongo.edugestin360.storage.PendingTaskDao;
import com.ugelcorongo.edugestin360.storage.PendingTaskEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel encargado de encolar y limpiar tareas de envío
 * (asistencias, evidencias u otros) para el rol Director.
 */
public class DirectorViewModel extends AndroidViewModel {
    private final PendingTaskDao dao;
    private final ExecutorService io;
    private final Gson gson = new Gson();

    public DirectorViewModel(@NonNull Application app) {
        super(app);
        dao = AppDatabase.getInstance(app).pendingDao();
        io  = Executors.newSingleThreadExecutor();
    }

    /**
     * Encola una tarea de envío (formulario o imagen).
     *
     * @param type    Identificador de tipo de tarea (p.ej. "ASIS_<docId>")
     * @param fileUri URI del archivo a subir, o null si es solo formulario
     * @param meta    Mapa de campos POST que se enviarán
     */
    public void enqueueUploadTask(
            String type,
            Uri fileUri,
            Map<String,String> meta
    ) {
        io.execute(() -> {
            String json = gson.toJson(meta);
            PendingTaskEntity t = new PendingTaskEntity(
                    type,
                    fileUri != null ? fileUri.toString() : "",
                    json
            );
            dao.insert(t);
        });
    }

    /**
     * Elimina de la cola la tarea cuyo tipo y metadata coinciden.
     * Úsalo tras un envío exitoso para evitar duplicados.
     *
     * @param type Identificador de la tarea encolada
     * @param meta Mapa de campos POST originalmente encolados
     */
    public void removeUploadTask(
            String type,
            Map<String,String> meta
    ) {
        io.execute(() -> {
            String json = gson.toJson(meta);
            List<PendingTaskEntity> pendientes = dao.getAll();
            for (PendingTaskEntity t : pendientes) {
                if (t.getType().equals(type)
                        && t.getMetaJson().equals(json)) {
                    dao.delete(t);
                    break;
                }
            }
        });
    }

    /**
     * Alias a removeUploadTask.
     */
    public void clearPendingTask(
            String type,
            Map<String,String> meta
    ) {
        removeUploadTask(type, meta);
    }
}