package com.ugelcorongo.edugestin360.managers.upload;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ugelcorongo.edugestin360.managers.VolleyMultipartRequest;
import com.ugelcorongo.edugestin360.storage.AppDatabase;
import com.ugelcorongo.edugestin360.storage.PendingTaskDao;
import com.ugelcorongo.edugestin360.storage.PendingTaskEntity;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;
import com.ugelcorongo.edugestin360.utils.UriUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manager para subir fichas (multipart) con Volley.
 * Provee métodos asíncrono (upload) y síncrono (uploadSync).
 */
public class FichaUploadManager {

    private final Context ctx;
    private final String  endpoint;
    private final PendingTaskDao dao;
    private final Gson gson = new Gson();

    public FichaUploadManager(Context ctx) {
        this.ctx      = ctx.getApplicationContext();
        this.endpoint = URLPostHelper.Fichas.REGISTRAR;
        this.dao      = AppDatabase.getInstance(ctx).pendingDao();
    }

    /**
     * Envía la ficha al servidor vía POST multipart.
     * @param params   Campos form‐data (textuales)
     * @param byteData Archivos (fotos) vía DataPart
     * @param cb       Callback asíncrono de resultado
     */
    public void upload(
            Map<String,String> params,
            Map<String,VolleyMultipartRequest.DataPart> byteData,
            UploadCallback cb
    ) {
        VolleyMultipartRequest req = new VolleyMultipartRequest(
                Request.Method.POST,
                endpoint,
                resp -> cb.onSuccess(),
                err -> {
                    // Si realmente perdimos la red tras arrancar el request
                    if (!NetworkUtil.isConnected(ctx)) {
                        enqueuePendingTask(params, byteData);
                        cb.onError(new IOException("Offline after request, queued"));
                    } else {
                        // aquí la red estaba, pero el servidor respondió mal
                        cb.onError(err);
                    }
                }
        ) {
            @Override protected Map<String,String> getParams() {
                Log.d("FETCH_ERROR", "FichaUpload === SUBMIT ALL: parámetros a enviar ===");
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    Log.d("FETCH_ERROR",
                            String.format("param → %s = '%s'",
                                    entry.getKey(),
                                    entry.getValue()));
                }
                return params;
            }
            @Override protected Map<String,DataPart> getByteData() {
                return byteData;
            }
        };

        Volley.newRequestQueue(ctx).add(req);
    }

    /**
     * Envía la ficha de forma bloqueante. Lanza IOException si falla.
     */
    public void uploadSync(
            Map<String,String> params,
            Map<String,VolleyMultipartRequest.DataPart> byteData
    ) throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errRef = new AtomicReference<>();

        upload(params, byteData, new UploadCallback() {
            @Override public void onSuccess() {
                latch.countDown();
            }
            @Override public void onError(Throwable t) {
                errRef.set(new IOException(t));
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IOException("Interrupted", e);
        }
        if (errRef.get() != null) {
            throw (IOException) errRef.get();
        }
    }

    /**
     * Guarda en Room la tarea para reintento posterior.
     */
    private void enqueuePendingTask(
            Map<String,String> params,
            Map<String, VolleyMultipartRequest.DataPart> byteData
    ) {
        // Meta = params + lista de keys de archivos
        Map<String,String> meta = new HashMap<>(params);
        meta.put("__files__", gson.toJson(byteData.keySet()));

        String jsonMeta = gson.toJson(meta);
        PendingTaskEntity t = new PendingTaskEntity(
                "FICHA",
                "",         // aquí podrías guardar URIs si las necesitas
                jsonMeta
        );
        Executors.newSingleThreadExecutor()
                .execute(() -> dao.insert(t));
    }

    /**
     * Procesa TODAS las tareas pendientes:
     * - Reconstruye params y byteData desde JSON
     * - Llama a uploadSync()
     * - Borra la entidad si tuvo éxito
     */
    public void processPendingTasks() {
        List<PendingTaskEntity> list = dao.getAll();
        for (PendingTaskEntity t : list) {
            Map<String,String> meta = gson.fromJson(
                    t.getMetaJson(),
                    new TypeToken<Map<String,String>>(){}.getType()
            );

            // Extraer la lista de keys de archivos
            List<String> fileKeys = gson.fromJson(
                    meta.remove("__files__"),
                    new TypeToken<List<String>>(){}.getType()
            );

            Map<String, VolleyMultipartRequest.DataPart> bd = new HashMap<>();
            for (String key : fileKeys) {
                Uri uri = Uri.parse(meta.get(key));
                byte[] bytes = UriUtils.readBytesFromUri(ctx, uri);
                bd.put(key, new VolleyMultipartRequest.DataPart(key + ".jpg", bytes, "image/jpeg"));
                meta.remove(key);
            }

            try {
                uploadSync(meta, bd);
                dao.delete(t);
            } catch (IOException ignored) { }
        }
    }

    /** Callback genérico para envíos */
    public interface UploadCallback {
        void onSuccess();
        void onError(Throwable t);
    }
}