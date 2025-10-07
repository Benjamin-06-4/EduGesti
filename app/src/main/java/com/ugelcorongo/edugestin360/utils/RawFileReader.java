package com.ugelcorongo.edugestin360.utils;

import android.content.Context;
import android.util.Log;

import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class RawFileReader {
    private RawFileReader() { /* No instanciable */ }

    /**
     * Lee R.raw.datacolegio y devuelve una lista de arreglos:
     * [0]=colegio, [1]=docidencidad, [2]=usuario(nombre), [3]=rol,
     * [4]=idColegio, [5]=latitud, [6]=longitud.
     */
    public static List<String[]> readRawDatacolegio(Context ctx) throws IOException {
        Log.d("FETCH_ERROR", "RawFileReader en: " + ctx);
        InputStream is = ctx.getResources().openRawResource(R.raw.datacolegio);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<String[]> rows = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            // Ignorar líneas vacías
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split(";");
            // Solo agregar si tiene al menos 7 columnas
            Log.d("FETCH_ERROR", "RawFileReader length: " + parts.length);
            if (parts.length >= 7) {
                rows.add(parts);
            }
        }
        br.close();
        return rows;
    }

    /**
     * Lee datainfoespecialistas.txt (interno si existe, sino raw/datainfoespecialistas.txt)
     * Cada línea: docIdentidad;nombreEspecialista
     */
    public static List<String[]> readRawEspecialistas(Context ctx) throws IOException {
        File internal = new File(ctx.getFilesDir(), "datainfoespecialistas.txt");
        InputStream is = internal.exists()
                ? new FileInputStream(internal)
                : ctx.getResources().openRawResource(R.raw.datainfoespecialistas);

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<String[]> rows = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split(";");
            if (parts.length >= 3) {
                rows.add(parts);
            }
        }
        br.close();
        return rows;
    }

    private static Double tryParseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return null; }
    }
}