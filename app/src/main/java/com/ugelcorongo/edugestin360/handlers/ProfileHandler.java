package com.ugelcorongo.edugestin360.handlers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ugelcorongo.edugestin360.storage.DBHelper;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Sincroniza campos de perfil del usuario desde el servidor hacia la BD local.
 */
public class ProfileHandler {

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Llama a GET /user/profile?userId=… y actualiza rol y docidentidad localmente.
     */
    public static void syncUserProfile(
            Context ctx,
            String userId,
            SyncCallback callback
    ) {
        if (!NetworkUtil.isOnline(ctx)) {
            callback.onError("Sin conexión, no fue posible sincronizar el perfil.");
            return;
        }

        new Thread(() -> {
            Handler ui = new Handler(Looper.getMainLooper());
            HttpURLConnection conn = null;
            try {
                // Construye URL de perfil (ajusta tu endpoint)
                String urlStr = URLPostHelper.Usuarios.CONSULTAR(userId);
                conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                InputStream is = (code < HttpURLConnection.HTTP_BAD_REQUEST)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                // Leer respuesta JSON
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }
                }
                JSONObject resp = new JSONObject(sb.toString());
                if (!resp.optBoolean("ok", false)) {
                    String err = resp.optString("error", "Error desconocido");
                    ui.post(() -> callback.onError(err));
                    return;
                }

                // Extrae nuevos valores
                String newDocident = resp.getString("docidentidad");

                // Actualiza localmente en SQLite
                DBHelper db = new DBHelper(ctx);
                db.updateUserDocIdent(userId, newDocident);
                db.close(); // asegúrate de cerrar la instancia

                ui.post(callback::onSuccess);

            } catch (Exception e) {
                Log.e("ProfileHandler", "syncUserProfile failed", e);
                ui.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}