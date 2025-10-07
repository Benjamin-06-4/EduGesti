package com.ugelcorongo.edugestin360.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.ugelcorongo.edugestin360.storage.DBHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EvidenceManager {
    private static final String URL = "https://api.example.com/evidence";
    private final OkHttpClient client;
    private final DBHelper db;

    public EvidenceManager(Context ctx) {
        client = new OkHttpClient();
        db     = new DBHelper(ctx);
    }

    /**
     * Listener renombrado para evitar colisiones con okhttp3.Callback.
     */
    public interface OnEvidenceCompleteListener {
        void onComplete(boolean success);
    }

    /**
     * Envía la foto de evidencia junto con latitud/longitud al servidor.
     * Si tiene éxito, elimina el registro local correspondiente.
     *
     * @param teacherId  identificador del docente
     * @param lat        latitud
     * @param lon        longitud
     * @param photo      bitmap JPEG a enviar
     * @param listener   callback con el resultado
     */
    public void sendEvidence(
            String teacherId,
            double lat,
            double lon,
            Bitmap photo,
            OnEvidenceCompleteListener listener
    ) {
        // 1. Convertir Bitmap a JPEG en byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();

        // 2. Construir multipart/form-data
        RequestBody imageBody = RequestBody.create(
                MediaType.parse("image/jpeg"),
                imageBytes
        );

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("teacherId", teacherId)
                .addFormDataPart("latitude",  String.valueOf(lat))
                .addFormDataPart("longitude", String.valueOf(lon))
                .addFormDataPart(
                        "photo",
                        teacherId + "_" + System.currentTimeMillis() + ".jpg",
                        imageBody
                )
                .build();

        // 3. Construir request
        Request request = new Request.Builder()
                .url(URL)
                .post(requestBody)
                .build();

        // 4. Encolar con okhttp3.Callback explícito
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EvidenceManager", "Error envío evidencia", e);
                listener.onComplete(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean ok = response.isSuccessful();
                if (ok) {
                    // elimina registro local solo si el servidor aceptó la imagen
                    db.deleteEvidenceRecord(teacherId, lat, lon);
                }
                listener.onComplete(ok);
            }
        });
    }

    /**
     * Guarda localmente la foto de evidencia junto con lat/lon.
     */
    public void saveEvidenceOffline(
            String teacherId,
            double lat,
            double lon,
            Bitmap photo
    ) {
        db.insertEvidence(teacherId, lat, lon, photo);
    }
}