package com.ugelcorongo.edugestin360.managers;

import android.content.Context;
import android.util.Log;
import com.ugelcorongo.edugestin360.storage.DBHelper;
import okhttp3.*;

import java.io.IOException;

public class AttendanceManager {
    private static final String URL = "https://api.example.com/attendance";
    private final OkHttpClient client;
    private final DBHelper db;

    public AttendanceManager(Context ctx) {
        client = new OkHttpClient();
        db     = new DBHelper(ctx);
    }

    /**
     * Listener renombrado para evitar conflictos con okhttp3.Callback.
     */
    public interface OnAttendanceCompleteListener {
        void onComplete(boolean success);
    }

    /**
     * Envía la asistencia al servidor o elimina el registro local si tuvo éxito.
     *
     * @param teacherId identificador del docente
     * @param lat       latitud
     * @param lon       longitud
     * @param listener  callback con el resultado del envío
     */
    public void sendAttendance(
            String teacherId,
            double lat,
            double lon,
            OnAttendanceCompleteListener listener
    ) {
        RequestBody body = new FormBody.Builder()
                .add("teacherId", teacherId)
                .add("latitude",  String.valueOf(lat))
                .add("longitude", String.valueOf(lon))
                .build();

        Request req = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("AttendanceManager", "Error envío asistencia", e);
                listener.onComplete(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean ok = response.isSuccessful();
                if (ok) {
                    db.deleteAttendanceRecord(teacherId, lat, lon);
                }
                listener.onComplete(ok);
            }
        });
    }

    public void saveAttendanceOffline(String teacherId, double lat, double lon) {
        db.insertAttendance(teacherId, lat, lon);
    }
}