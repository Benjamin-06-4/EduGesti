package com.ugelcorongo.edugestin360.ui.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ugelcorongo.edugestin360.repository.DocenteInfoRepository;
import com.ugelcorongo.edugestin360.storage.PendingTaskEntity;
import com.ugelcorongo.edugestin360.storage.AppDatabase;
import com.ugelcorongo.edugestin360.storage.PendingTaskDao;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocenteViewModel extends AndroidViewModel {

    private final DocenteInfoRepository repository;
    private final PendingTaskDao pendingDao;
    private final ExecutorService ioExecutor;

    private final MutableLiveData<Boolean> attendanceResult = new MutableLiveData<>();

    public DocenteViewModel(@NonNull Application application) {
        super(application);
        repository = DocenteInfoRepository.getInstance(application);
        AppDatabase db = AppDatabase.getInstance(application);
        pendingDao = db.pendingDao();
        ioExecutor = Executors.newSingleThreadExecutor();
    }

    /** Observador para el resultado de registro de asistencia */
    public LiveData<Boolean> getAttendanceResult() {
        return attendanceResult;
    }

    /**
     * Registra la asistencia enviando coordenadas al servidor.
     * Si hay error de red, encola la tarea en Room para reintento.
     */
    public void registerAttendance(String locationName,
                                   double latitude,
                                   double longitude) {
        Map<String, String> meta = Map.of(
                "colegio", repository.getCurrentSchool(),
                "docente", repository.getCurrentTeacher(),
                "ubicacion", locationName,
                "gpsLat", String.valueOf(latitude),
                "gpsLon", String.valueOf(longitude)
        );

        repository.sendAttendance(meta,
                _response -> attendanceResult.postValue(true),
                _error    -> {
                    // En caso de fallo de red, encolamos la tarea:
                    ioExecutor.execute(() -> {
                        PendingTaskEntity task = new PendingTaskEntity(
                                "ATTENDANCE",
                                "",                     // sin archivo asociado
                                repository.metaToJson(meta)
                        );
                        pendingDao.insert(task);
                    });
                    attendanceResult.postValue(false);
                }
        );
    }

    /**
     * Encola una tarea de subida de archivo (PDF o IMG) en caso de offline.
     */
    public void enqueueUploadTask(String type, Uri fileUri, Map<String, String> meta) {
        ioExecutor.execute(() -> {
            // Construye la entidad usando el constructor existente
            PendingTaskEntity task = new PendingTaskEntity(
                    type,
                    fileUri != null ? fileUri.toString() : "",
                    repository.metaToJson(meta)
            );
            pendingDao.insert(task);
        });
    }
}