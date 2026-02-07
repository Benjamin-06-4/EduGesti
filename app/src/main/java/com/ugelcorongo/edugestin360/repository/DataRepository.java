package com.ugelcorongo.edugestin360.repository;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ugelcorongo.edugestin360.domain.models.Ficha;
import com.ugelcorongo.edugestin360.domain.models.OpcionRespuesta;
import com.ugelcorongo.edugestin360.domain.models.Pregunta;
import com.ugelcorongo.edugestin360.remote.ApiService;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.RawFileReader;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

public class DataRepository {
    private static DataRepository instance;
    private final Context    context;
    private final ApiService api;

    private static final String COLEGIOS_FILE   = "datacolegio.txt";
    private static final String ESPECIAL_FILE   = "datainfoespecialistas.txt";
    private static final String FICHAS_FILE     = "datafichas.txt";
    private static final String PREGS_FILE      = "datapreguntas.txt";
    private static final String RESP_FILE       = "datarespuestas.txt";

    private DataRepository(Context ctx) {
        context = ctx.getApplicationContext();
        api     = ApiService.getInstance(context);
    }

    public static synchronized DataRepository getInstance(Context ctx) {
        if (instance == null) {
            instance = new DataRepository(ctx);
        }
        return instance;
    }

    /** Lee el CSV local de colegios ya descargado por BaseRoleActivity */
    public List<String[]> getColegiosLocal() throws IOException {
        return RawFileReader.readRawDatacolegio(context);
    }

    /** Lee el CSV local de especialistas ya descargado por BaseRoleActivity */
    public List<String[]> getEspecialistasLocal() throws IOException {
        return RawFileReader.readRawEspecialistas(context);
    }

    /** Lee el CSV local de fichas ya descargado por BaseRoleActivity */
    public List<Ficha> getFichasLocal() throws IOException {
        String raw = readRaw(FICHAS_FILE);
        return parseFichas(raw);
    }

    /**
     * Orquesta preguntas de la ficha seleccionada:
     * - Online solo para JSON (no actualiza textos plano)
     * - Offline: lee cache JSON en interno
     */
    public void fetchPreguntas(
            String idFicha,
            ApiService.ApiCallback<List<Pregunta>> cb
    ) {
        String url = URLPostHelper.Preguntas.VER + "?idficha=" + idFicha;
        if (NetworkUtil.isOnline(context)) {
            api.fetchJson(url, new ApiService.ApiCallback<String>() {
                @Override public void onSuccess(String raw) {
                    saveRaw(PREGS_FILE, raw);
                    cb.onSuccess(parsePreguntas(raw));
                }
                @Override public void onError(Exception e) {
                    offlinePregs(cb);
                }
            });
        } else {
            offlinePregs(cb);
        }
    }

    private void offlinePregs(ApiService.ApiCallback<List<Pregunta>> cb) {
        try {
            String raw = readRaw(PREGS_FILE);
            cb.onSuccess(parsePreguntas(raw));
        } catch (Exception ex) {
            cb.onError(ex);
        }
    }

    public void fetchRespuestas(
            String idPregunta,
            ApiService.ApiCallback<List<OpcionRespuesta>> cb
    ) {
        String url = URLPostHelper.Respuestas.VER + "?idpregunta=" + idPregunta;
        if (NetworkUtil.isOnline(context)) {
            api.fetchJson(url, new ApiService.ApiCallback<String>() {
                @Override public void onSuccess(String raw) {
                    saveRaw(RESP_FILE, raw);
                    cb.onSuccess(parseRespuestas(raw));
                }
                @Override public void onError(Exception e) {
                    offlineResps(cb);
                }
            });
        } else {
            offlineResps(cb);
        }
    }

    private void offlineResps(ApiService.ApiCallback<List<OpcionRespuesta>> cb) {
        try {
            String raw = readRaw(RESP_FILE);
            cb.onSuccess(parseRespuestas(raw));
        } catch (Exception ex) {
            cb.onError(ex);
        }
    }

    // — Helpers de I/O y parseo —

    private void saveRaw(String file, String txt) {
        try (FileOutputStream fos = context.openFileOutput(file, Context.MODE_PRIVATE)) {
            fos.write(txt.getBytes());
        } catch (IOException ignored) {}
    }

    private String readRaw(String file) throws IOException {
        try (FileInputStream fis = context.openFileInput(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = fis.read(buf)) > 0) bos.write(buf, 0, r);
            return bos.toString("UTF-8");
        }
    }

    private List<Ficha> parseFichas(String csv) {
        List<Ficha> out = new ArrayList<>();
        for (String line : csv.split("\\r?\\n")) {
            String[] p = line.split(";");
            if (p.length < 8 || !"ACTIVO".equalsIgnoreCase(p[7])) continue;
            String area = p[7].trim();
            out.add(new Ficha(
                    p[0], p[1], p[2], p[3],
                    p[4], p[5], p[6], area
            ));
        }
        return out;
    }

    /**
     * Parsea un JSON de preguntas y crea una lista de objetos Pregunta.
     * Cada Pregunta lleva su lista de opciones inicializada vacía.
     */
    private List<Pregunta> parsePreguntas(String rawJson) {
        List<Pregunta> listado = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(rawJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                String idFicha            = o.optString("idficha", "");
                String tipoFicha          = o.optString("tipoFicha", "");
                String idPregunta         = o.optString("idpregunta", "");
                String textoPregunta      = o.optString("texto", "");
                String tipoPregunta       = o.optString("tipo", "");
                boolean requiereComentario= o.optBoolean("requiereComentario", false);
                boolean requiereFoto      = o.optBoolean("requiereFoto", false);
                String seccion            = o.optString("seccion", "");

                // Inicializar lista vacía; luego se rellenará con fetchRespuestas(...)
                List<OpcionRespuesta> opciones = new ArrayList<>();

                Pregunta p = new Pregunta(
                        idFicha,
                        tipoFicha,
                        idPregunta,
                        textoPregunta,
                        tipoPregunta,
                        requiereComentario,
                        requiereFoto,
                        seccion,
                        opciones
                );
                listado.add(p);
            }
        } catch (JSONException e) {
            Log.e("DataRepository", "Error parsing preguntas JSON", e);
        }
        return listado;
    }

    private List<OpcionRespuesta> parseRespuestas(String rawJson) {
        List<OpcionRespuesta> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(rawJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new OpcionRespuesta(
                        o.getString("idrespuesta"),
                        o.getString("idpregunta"),
                        o.getString("tipo"),
                        o.getString("descripcion")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }
}