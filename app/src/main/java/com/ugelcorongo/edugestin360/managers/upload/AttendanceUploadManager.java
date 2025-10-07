package com.ugelcorongo.edugestin360.managers.upload;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class AttendanceUploadManager {
    private final Context ctx;
    private final String endpoint;

    public AttendanceUploadManager(Context ctx, String endpoint) {
        this.ctx      = ctx.getApplicationContext();
        this.endpoint = endpoint;
    }

    /** POST asíncrono */
    public void upload(Map<String,String> meta, UploadCallback cb) {
        Log.d("FETCH_ERROR", "managers.upload.AttendanceUploadManager POST to " + endpoint);
        Log.d("FETCH_ERROR", "Params → " + meta);

        StringRequest req = new StringRequest(
                Request.Method.POST,
                endpoint,
                response -> {
                    Log.d("FETCH_ERROR", "Response → " + response);
                    cb.onSuccess();
                },
                error -> {
                    String body = "";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        body = new String(error.networkResponse.data);
                    }
                    Log.e("FETCH_ERROR", "Error code=" +
                            (error.networkResponse != null
                                    ? error.networkResponse.statusCode
                                    : "n/a")
                            + " body=" + body, error);
                    cb.onError(error);
                }
        ) {
            @Override
            protected Map<String,String> getParams() throws AuthFailureError {
                // Este método se llama justo antes de enviarlo
                return meta;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        // Desactivar cache para que siempre mande
        req.setShouldCache(false);

        Volley.newRequestQueue(ctx).add(req);
    }

    /**
     * POST síncrono. Lanza IOException si falla o no hay red.
     */
    public void uploadSync(Map<String,String> meta) throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errRef = new AtomicReference<>();

        upload(meta, new UploadCallback() {
            @Override public void onSuccess()     { latch.countDown(); }
            @Override public void onError(Throwable t) {
                errRef.set(new IOException(t));
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IOException("Interrumpido", e);
        }
        if (errRef.get() != null) throw (IOException) errRef.get();
    }

    public interface UploadCallback {
        void onSuccess();
        void onError(Throwable t);
    }
}