package com.ugelcorongo.edugestin360.managers.upload;

import android.content.Context;
import android.net.Uri;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ugelcorongo.edugestin360.storage.AppDatabase;
import com.ugelcorongo.edugestin360.storage.PendingTaskDao;
import com.ugelcorongo.edugestin360.storage.PendingTaskEntity;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Lee todas las PendingTaskEntity de Room y las envía con uploadSync().
 * Al completarse correctamente, elimina la entrada de la tabla.
 */
public class PendingUploadProcessor {
    private final PendingTaskDao dao;
    private final Gson gson = new Gson();
    private final Context ctx;

    public PendingUploadProcessor(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.dao = AppDatabase.getInstance(ctx).pendingDao();
    }

    /**
     * Se debe invocar en un hilo de fondo.
     * Procesa todas las tareas pendientes.
     */
    public void processAll() {
        List<PendingTaskEntity> tasks = dao.getAll();
        Type mapType = new TypeToken<Map<String,String>>(){}.getType();

        for (PendingTaskEntity t : tasks) {
            try {
                Map<String,String> meta = gson.fromJson(t.getMetaJson(), mapType);
                switch (t.getType()) {
                    case "PDF":
                        new PdfUploadManager(ctx)
                                .uploadSync(Uri.parse(t.getFileUri()), meta);
                        break;
                    case "IMG":
                        new ImageUploadManager(ctx)
                                .uploadSync(Uri.parse(t.getFileUri()), meta);
                        break;
                    // agregar más casos si hay otros tipos
                    default:
                        throw new IllegalArgumentException("Tipo no soportado: " + t.getType());
                }
                // Eliminamos la tarea completada para evitar duplicados
                dao.delete(t);
            } catch (IOException | IllegalArgumentException ex) {
                ex.printStackTrace();
                // Si falla, lo dejamos para el siguiente ciclo
            }
        }
    }
}