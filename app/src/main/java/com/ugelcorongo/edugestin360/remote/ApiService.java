package com.ugelcorongo.edugestin360.remote;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Solo GET de catálogos y POST de JSON simple (si lo necesitas).
 * No guarda nada en disco ni encola.
 */
public class ApiService {
    private static ApiService instance;
    private final RequestQueue queue;

    private ApiService(Context ctx) {
        queue = Volley.newRequestQueue(ctx.getApplicationContext());
    }

    public static synchronized ApiService getInstance(Context ctx) {
        if (instance == null) instance = new ApiService(ctx);
        return instance;
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }


    /** Descarga raw JSON de preguntas */
    public void fetchPreguntas(String url, final ApiCallback<String> cb) {
        StringRequest r = new StringRequest(
                Request.Method.GET, url,
                cb::onSuccess,
                error -> cb.onError(error)
        );
        queue.add(r);
    }

    /**
     * Llama al endpoint que devuelve las opciones de respuesta
     * para una pregunta concreta.
     */
    public void fetchRespuestas(String url, final ApiCallback<String> cb) {
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                cb::onSuccess,
                error -> cb.onError(error)
        );
        queue.add(request);
    }

    /**
     * GET para obtener JSON como String.
     */
    public void fetchJson(String url, ApiCallback<String> cb) {
        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                cb::onSuccess,
                error -> cb.onError(error)
        );
        queue.add(req);
    }
}