package com.ugelcorongo.edugestin360.ui.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.google.gson.Gson;
import com.ugelcorongo.edugestin360.storage.AppDatabase;
import com.ugelcorongo.edugestin360.storage.PendingTaskDao;
import com.ugelcorongo.edugestin360.storage.PendingTaskEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EspecialistaViewModel extends AndroidViewModel {
    private final PendingTaskDao dao;
    private final ExecutorService io;
    private final Gson gson = new Gson();

    // respuestas SINGLE_CHOICE
    private final Map<String,String> answers = new ConcurrentHashMap<>();
    // respuestas TEXT
    private final Map<String,String> textAnswers   = new ConcurrentHashMap<>();
    // comentarios opcionales
    private final Map<String,String> commentAnswers= new ConcurrentHashMap<>();
    private final Map<String, Set<String>> multiAnswers = new ConcurrentHashMap<>();
    // URIs de fotos por pregunta
    private final Map<String,Uri>    photoUris     = new ConcurrentHashMap<>();


    public EspecialistaViewModel(@NonNull Application app) {
        super(app);
        dao = AppDatabase.getInstance(app).pendingDao();
        io  = Executors.newSingleThreadExecutor();
    }

    /** Encola ATTENDANCE o IMG */
    public void enqueueUploadTask(
            String type, Uri fileUri, Map<String,String> meta
    ) {
        io.execute(() -> {
            String json = gson.toJson(meta);
            PendingTaskEntity t = new PendingTaskEntity(
                    type,
                    fileUri!=null?fileUri.toString():"",
                    json
            );
            dao.insert(t);
        });
    }

    /**
     * Borra de la cola la tarea cuyo tipo y meta coinciden.
     * Se ejecuta tras un envío exitoso para evitar duplicados.
     */
    public void removeUploadTask(String type, Map<String,String> meta) {
        io.execute(() -> {
            String json = gson.toJson(meta);
            // Buscar la entidad que coincida
            List<PendingTaskEntity> pendientes = dao.getAll();
            for (PendingTaskEntity t : pendientes) {
                if (t.getType().equals(type)
                        && t.getMetaJson().equals(json)) {
                    dao.delete(t);
                    break;
                }
            }
        });
    }

    /**
     * Retorna la Uri de la foto capturada para la pregunta dada,
     * o null si no existe.
     */
    public Uri getPhotoUri(String idPregunta) {
        return photoUris.get(idPregunta);
    }

    // Guardar respuesta
    public void setAnswer(String idPregunta, String respuesta) {
        answers.put(idPregunta, respuesta);
    }

    // Leer respuesta (null si no existe)
    public String getAnswer(String idPregunta) {
        return answers.get(idPregunta);
    }

    /**
     * Elimina una tarea pendiente de la cola (alias a removeUploadTask).
     */
    public void clearPendingTask(String type, Map<String,String> meta) {
        removeUploadTask(type, meta);
    }

    public Map<String,String> getAllAnswers() {
        return new HashMap<>(answers);
    }

    // --- TEXT ---
    public void setTextAnswer(String idPregunta, String texto) {
        textAnswers.put(idPregunta, texto);
    }

    public String getTextAnswer(String idPregunta) {
        return textAnswers.get(idPregunta);
    }

    // --- COMMENTARIO ---
    public void setComment(String idPregunta, String comentario) {
        commentAnswers.put(idPregunta, comentario);
    }

    public String getComment(String idPregunta) {
        return commentAnswers.get(idPregunta);
    }

    // --- FOTOS ---
    public void setPhotoUri(String idPregunta, Uri uri) {
        photoUris.put(idPregunta, uri);
    }

    /**
     * Retorna todas las fotos (campo multipart) que se hayan capturado.
     */
    public Map<String,Uri> getAllPhotoUris() {
        return new HashMap<>(photoUris);
    }

    public void setMultiAnswer(String idPregunta, String opcion, boolean checked) {
        Set<String> set = multiAnswers.computeIfAbsent(idPregunta, k -> new LinkedHashSet<>());
        if (checked) {
            set.add(opcion);
        } else {
            set.remove(opcion);
        }
        if (set.isEmpty()) {
            multiAnswers.remove(idPregunta);
        }
    }

    /** Obtiene la lista de opciones seleccionadas (o lista vacía). */
    public List<String> getMultiAnswers(String idPregunta) {
        Set<String> set = multiAnswers.get(idPregunta);
        return set == null
                ? Collections.emptyList()
                : new ArrayList<>(set);
    }

    /** Retorna todos los pares pregunta→opciones para el envío */
    public Map<String, Set<String>> getAllMultiAnswers() {
        return new HashMap<>(multiAnswers);
    }

    // --- LIMPIAR ESTADO ---
    /**
     * Borra todas las respuestas, textos, comentarios y URIs de foto.
     * Úsalo tras un envío exitoso para reiniciar el formulario.
     */
    public void clearAllAnswers() {
        answers.clear();
        textAnswers.clear();
        commentAnswers.clear();
        photoUris.clear();
    }
}