package com.ugelcorongo.edugestin360.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Map;

public class NetworkUtil {
    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    /** Comprueba que tengamos red y que podamos hacer ping a un host fiable */
    public static boolean isConnected(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        if (ni == null || !ni.isConnected()) return false;

        // Opcional: comprobación de respuesta ICMP a Google DNS
        try {
            Process p = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8");
            int     exit = p.waitFor();
            return (exit == 0);
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Envía una petición POST con parámetros form-data y callbacks para éxito/error.
     *
     * @param ctx         Contexto de la Activity o Fragment.
     * @param url         URL del endpoint.
     * @param params      Mapa clave=valor con los campos del formulario.
     * @param listener    Callback ejecutado en caso de éxito (cadena JSON).
     * @param errListener Callback ejecutado en caso de error (VolleyError).
     */
    public static void postForm(
            Context ctx,
            String url,
            Map<String, String> params,
            Response.Listener<String> listener,
            Response.ErrorListener errListener
    ) {
        Log.d(
                "FETCH_ERROR",
                "NetworkUtil postForm ----------"
        );
        RequestQueue queue = Volley.newRequestQueue(ctx);
        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                listener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Extra log de cuerpo y código HTTP
                        NetworkResponse nr = error.networkResponse;
                        if (nr != null && nr.data != null) {
                            String body = new String(nr.data);
                            Log.d(
                                    "FETCH_ERROR",
                                    "HTTP " + nr.statusCode + " Body: " + body
                            );
                        } else {
                            Log.d(
                                    "FETCH_ERROR",
                                    "VolleyError: " + error.getMessage()
                            );
                        }
                        if (errListener != null) {
                            errListener.onErrorResponse(error);
                        }
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        queue.add(req);
    }

    /**
     * Envía una petición GET a la URL dada.
     *
     * @param ctx         Contexto desde el que se llama.
     * @param url         URL completa con query params si aplica.
     * @param listener    Callback que recibe el body en caso de 200 OK.
     * @param errListener Callback que recibe el VolleyError en caso de fallo.
     */
    public static void get(
            Context ctx,
            String url,
            Response.Listener<String> listener,
            Response.ErrorListener errListener
    ) {
        Log.d(
                "FETCH_ERROR",
                "NetworkUtil ----------"
        );
        Log.d(
                "FETCH_ERROR",
                "url get: " + url
        );
        RequestQueue queue = Volley.newRequestQueue(ctx);
        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                listener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Loguear código y body si vienen
                        NetworkResponse nr = error.networkResponse;
                        if (nr != null && nr.data != null) {
                            String body = new String(nr.data);
                            Log.d(
                                    "FETCH_ERROR",
                                    "HTTP " + nr.statusCode + " Body: " + body
                            );
                        } else {
                            Log.d(
                                    "FETCH_ERROR",
                                    "VolleyError: " + error.getMessage()
                            );
                        }
                        if (errListener != null) {
                            errListener.onErrorResponse(error);
                        }
                    }
                }
        );
        queue.add(req);
    }
}