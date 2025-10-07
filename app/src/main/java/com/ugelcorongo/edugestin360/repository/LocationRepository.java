package com.ugelcorongo.edugestin360.repository;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;

import java.util.HashMap;
import java.util.Map;

public class LocationRepository {

    private final Context context;
    private final RequestQueue queue;

    public interface Callback {
        void onSuccess();
        void onError(Exception e);
    }

    public LocationRepository(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.queue = Volley.newRequestQueue(this.context);
    }

    /**
     * Envía coordenadas al endpoint indicado (POST form-url-encoded).
     * - url: URLPostHelper.Coordenadas.REGISTRAR
     * - nombre: nombre del usuario
     * - rol: "Especialista" / "Docente" / "Director"
     * - docIdentidad: (opcional) dni
     * - usuarioId: (opcional) id interno
     */
    public void sendCoordinates(
            final String url,
            final String nombre,
            final String rol,
            final String docIdentidad,
            final String usuarioId,
            final double lat,
            final double lon,
            final Callback cb
    ) {
        if (!NetworkUtil.isConnected(context)) {
            if (cb != null) cb.onError(new IllegalStateException("No network"));
            return;
        }

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    if (cb != null) cb.onSuccess();
                },
                error -> {
                    // Mejor diagnóstico del error
                    String body = null;
                    int status = -1;
                    if (error.networkResponse != null) {
                        status = error.networkResponse.statusCode;
                        try {
                            body = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception ex) {
                            body = "<unable to decode body>";
                        }
                    }
                    if (cb != null) cb.onError(error);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> p = new HashMap<>();
                if (nombre != null) p.put("nombre", nombre);
                if (rol != null) p.put("rol", rol);
                if (docIdentidad != null) p.put("docidentidad", docIdentidad);
                if (usuarioId != null) p.put("usuario_id", usuarioId);
                p.put("latitud", String.valueOf(lat));
                p.put("longitud", String.valueOf(lon));
                return p;
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        // Evitar caché que pueda causar problemas y añadir política de retry/timeout razonable
        req.setShouldCache(false);
        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10_000, // timeout ms
                2,      // maxRetries
                1.0f    // backoff multiplier
        ));
        queue.add(req);
    }
}