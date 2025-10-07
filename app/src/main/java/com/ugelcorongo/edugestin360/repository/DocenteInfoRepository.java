package com.ugelcorongo.edugestin360.repository;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import org.json.JSONObject;

import java.util.Map;

/**
 * Singleton que administra la información del docente y envía la asistencia al servidor.
 */
public class DocenteInfoRepository {

    private static volatile DocenteInfoRepository instance;
    private final Context ctx;
    private final RequestQueue requestQueue;

    // Valores actuales de la sesión
    private String currentSchool;
    private String currentTeacher;

    private DocenteInfoRepository(Context context) {
        this.ctx = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(ctx);
    }

    /** Obtiene la instancia singleton */
    public static DocenteInfoRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (DocenteInfoRepository.class) {
                if (instance == null) {
                    instance = new DocenteInfoRepository(context);
                }
            }
        }
        return instance;
    }

    /** Inicializa contexto de sesión */
    public void initSession(String school, String teacher) {
        this.currentSchool = school;
        this.currentTeacher = teacher;
    }

    public String getCurrentSchool() {
        return currentSchool;
    }

    public String getCurrentTeacher() {
        return currentTeacher;
    }

    /**
     * Envía la asistencia al endpoint remoto.
     *
     * @param meta    Mapa con claves: colegio, docente, ubicacion, gpsLat, gpsLon…
     * @param listener Callback de éxito
     * @param error    Callback de error
     */
    public void sendAttendance(Map<String, String> meta,
                               Response.Listener<String> listener,
                               Response.ErrorListener error) {
        String url = URLPostHelper.Data.DocentesInfo;
        StringRequest req = new StringRequest(Request.Method.POST, url, listener, error) {
            @Override
            protected Map<String, String> getParams() {
                return meta;
            }
        };
        requestQueue.add(req);
    }

    /**
     * Convierte un Map<String,String> en un JSON string (para persistir en Room).
     */
    public String metaToJson(Map<String, String> meta) {
        try {
            return new JSONObject(meta).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
}