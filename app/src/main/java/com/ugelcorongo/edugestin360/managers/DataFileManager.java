package com.ugelcorongo.edugestin360.managers;

import android.content.Context;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class DataFileManager {

    public interface UpdateCallback {
        void onComplete();
        void onError(String errorMsg);
    }

    /**
     * Recorre el map <fileName, remoteUrl>, descarga cada recurso y escribe
     * en archivos internos. Ejecuta callbacks según resultado.
     */
    public static void updateFiles(Context ctx,
                                   Map<String, String> fileUrlMap,
                                   UpdateCallback callback) {
        new Thread(() -> {
            try {
                for (Map.Entry<String, String> entry : fileUrlMap.entrySet()) {
                    String fileName = entry.getKey();
                    String urlString = entry.getValue();
                    String data = fetchRemoteData(urlString);
                    writeToFile(ctx, fileName, data);
                }
                callback.onComplete();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static List<String> readLines(Context ctx, String filename) {
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = ctx.openFileInput(filename);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    /** Realiza GET simple al servidor y devuelve el cuerpo como String */
    private static String fetchRemoteData(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    /** Sobrescribe el archivo interno con el contenido descargado */
    private static void writeToFile(Context ctx, String fileName, String data) throws IOException {
        try (FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Lee líneas de un archivo interno y las devuelve en List<String> */
    public static List<String> readFileLines(Context ctx, String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        try (InputStream is = ctx.openFileInput(fileName);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}