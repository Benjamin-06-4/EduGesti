package com.ugelcorongo.edugestin360.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ugelcorongo.edugestin360.storage.DBHelper;
import com.ugelcorongo.edugestin360.ui.ChangePasswordActivity;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Maneja login inicial (online), login offline y fuerza cambio de contraseña.
 */
public class RegistrationHandler {

    public interface LoginCallback {
        void onSuccess(String role, String docIdentidad);
        void onFail(String message);
        void onRequirePasswordChange(String userId);
    }

    /**
     * Intenta el login de este userId + password.
     *
     * - Si el usuario no existe localmente → requiere internet y va por la rama ONLINE
     * - Si el usuario ya existe       → valida offline con hash guardado
     */
    public static void attemptLogin(
            final Context ctx,
            final String userId,
            final String password,
            final LoginCallback cb) {

        DBHelper db = new DBHelper(ctx);
        boolean knownUser = db.userExists(userId);

        // Handler para callbacks en UI
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // 1) Nuevo usuario → login ONLINE obligatorio
        if (!knownUser) {
            if (!NetworkUtil.isOnline(ctx)) {
                mainHandler.post(() ->
                        cb.onFail("Primera vez: internet requerido para nuevo usuario."));
                return;
            }

            new Thread(() -> {
                try {
                    // Llama a tu endpoint remoto de verificación
                    JSONObject resp = verifyWithServer(userId, password);
                    boolean ok   = resp.getBoolean("ok");

                    if (!ok) {
                        mainHandler.post(() ->
                                cb.onFail("Credenciales inválidas."));
                        return;
                    }

                    // Extrae el rol y guarda hash+rol localmente
                    String role = resp.getString("rol");
                    String docident = resp.optString("docidentidad");
                    db.updateUserDocIdent(userId, docident);
                    db.upsertUser(userId,
                            DBHelper.hash(password),
                            role,
                            docident);

                    // Forzar cambio si sigue usando DNI
                    if (password.matches("\\d+")) {
                        mainHandler.post(() ->
                                cb.onRequirePasswordChange(userId));
                    } else {
                        mainHandler.post(() ->
                                cb.onSuccess(role, docident));
                    }

                } catch (Exception e) {
                    mainHandler.post(() ->
                            cb.onFail("Error de red o servidor: " + e.getMessage()));
                }
            }).start();

        } else {
            // 2) Usuario conocido → login OFFLINE
            boolean validLocal = db.verifyPassword(userId, password);
            if (!validLocal) {
                mainHandler.post(() ->
                        cb.onFail("Usuario o contraseña incorrectos."));
                return;
            }

            String role = db.getUserRole(userId);
            String docident = db.getUserDocIdent(userId);
            db.updateUserDocIdent(userId, docident);

            if (password.matches("\\d+")) {
                mainHandler.post(() ->
                        cb.onRequirePasswordChange(userId));
            } else {
                mainHandler.post(() ->
                        cb.onSuccess(role, docident));
            }
        }
    }

    /** Simula llamada HTTP POST al servidor de autenticación */
    private static JSONObject verifyWithServer(String userId, String pwd) throws Exception {
        URL url = new URL(URLPostHelper.Login.Verificar());

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject body = new JSONObject();
        body.put("userId", userId);
        body.put("password", pwd);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes("UTF-8"));
        }

        int status = conn.getResponseCode();
        InputStream is;
        if (status >= HttpURLConnection.HTTP_OK
                && status < HttpURLConnection.HTTP_BAD_REQUEST) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        String resp = sb.toString().trim();
        if (resp.isEmpty()) {
            throw new IOException("Respuesta vacía del servidor");
        }
        String respBody = sb.toString().trim();
        Log.d("FETCH_ERROR", "RegistrationHandler POST " + url.toString() + " -> code=" + status + " body=" + respBody);
        if (respBody.isEmpty()) {
            throw new IOException("Respuesta vacía del servidor");
        }
        return new JSONObject(resp);
    }

    /** Lógica para cambiar la contraseña (online + local) */
    /** Lanza el cambio de contraseña al servidor y actualiza localmente el hash y rol. */
    public static void changePassword(Context ctx,
                                      String userId,
                                      String newPassword,
                                      LoginCallback cb) {
        if (!NetworkUtil.isOnline(ctx)) {
            cb.onFail("Se requiere conexión para cambiar contraseña.");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // 1. Construyes la URL REST:
                URL url = new URL(URLPostHelper.Login.CambiarPassword());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(15_000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // 2. Armas payload JSON
                JSONObject payload = new JSONObject();
                payload.put("userId",      userId);
                payload.put("newPassword", newPassword);

                // 3. Escribes el payload en el body
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // 4. Lees la respuesta
                int code = conn.getResponseCode();
                InputStream is = (code < HttpURLConnection.HTTP_BAD_REQUEST)
                        ? conn.getInputStream()
                        : conn.getErrorStream();
                StringBuilder respText = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        respText.append(line.trim());
                    }
                }
                String respBody = respText.toString().trim();
                Log.d("FETCH_ERROR", "Change PUT " + url.toString() + " -> code=" + code + " body=" + respBody);
                JSONObject resp = new JSONObject(respText.toString());
                boolean ok = resp.optBoolean("ok", false);
                if (!ok) {
                    runOnMain(() -> cb.onFail("Error en servidor al cambiar contraseña."));
                    return;
                }

                // 5. Conseguir el rol: lo que venga del servidor o el local
                String role = resp.has("rol")
                        ? resp.getString("rol")
                        : new DBHelper(ctx).getUserRole(userId);

                String docident = resp.has("docidentidad")
                        ? resp.getString("docidentidad")
                        : new DBHelper(ctx).getUserDocIdent(userId);
                // 6. Guardar hash+rol en BD local
                new DBHelper(ctx)
                        .upsertUser(userId, DBHelper.hash(newPassword), role, docident);

                // 7. Callback de éxito en hilo principal
                runOnMain(() -> cb.onSuccess(role, docident));

            } catch (Exception e) {
                runOnMain(() -> cb.onFail("Error: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /** Pequeño helper para postear al hilo UI */
    private static void runOnMain(Runnable r) {
        new android.os.Handler(
                android.os.Looper.getMainLooper()
        ).post(r);
    }

    /** Devuelve un Intent explícito para ChangePasswordActivity */
    public static Intent getChangePasswordIntent(Context ctx, String userId) {
        Intent intent = new Intent(ctx, ChangePasswordActivity.class);
        intent.putExtra(ChangePasswordActivity.EXTRA_USER_ID, userId);
        return intent;
    }
}