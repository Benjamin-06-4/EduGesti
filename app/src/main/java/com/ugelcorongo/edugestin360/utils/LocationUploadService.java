package com.ugelcorongo.edugestin360.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.ugelcorongo.edugestin360.repository.LocationRepository;
import com.ugelcorongo.edugestin360.utils.LocationStreamer;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

public class LocationUploadService extends Service {
    public static final String EXTRA_DOC = "extra_doc";
    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_ROL = "extra_rol";

    private static final String TAG = "FETCH_ERROR";
    private static final String CHANNEL_ID = "loc_upload_chan";
    private static final int NOTIF_ID = 1;
    private static final long DEFAULT_INTERVAL_MS = 5000L;

    private LocationStreamer streamer;
    private LocationRepository locationRepo;

    private String docIdentidad;
    private String usuarioId;
    private String usuarioName;
    private String rol;

    private boolean streamingStarted = false;
    public static volatile boolean isRunning = false;
    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        locationRepo = new LocationRepository(getApplicationContext());
        createNotificationChannelIfNeeded();
        startForegroundWithNotification();
    }

    @SuppressLint("MissingPermission")
    private void startForegroundWithNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoreo de ubicación activo")
                .setContentText("Envío de ubicación en segundo plano")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, n);
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Ubicación en tiempo real",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            docIdentidad = intent.getStringExtra(EXTRA_DOC);
            usuarioId = intent.getStringExtra(EXTRA_ID);
            usuarioName = intent.getStringExtra(EXTRA_NAME);
            rol = intent.getStringExtra(EXTRA_ROL);
            if (rol == null) rol = "Especialista";
        }

        if (!hasLocationPermissions()) {
            Log.w(TAG, "Missing location permissions, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!streamingStarted) {
            streamer = new LocationStreamer(getApplicationContext());
            streamer.start(DEFAULT_INTERVAL_MS, new LocationStreamer.StreamCallback() {
                @Override
                public void onUpdate(Location location) {
                    if (location != null) {
                        handleLocationUpdate(location);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.w(TAG, "LocationStreamer error: " + e.getMessage(), e);
                    // Si quieres encolar la tarea cuando falla la petición al servidor,
                    // usa aquí PendingUploadProcessor o viewModel.enqueueUploadTask.
                }
            });
            streamingStarted = true;
        }

        return START_STICKY;
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void handleLocationUpdate(Location location) {
        if (!NetworkUtil.isConnected(getApplicationContext())) {
            return;
        }

        final double lat = location.getLatitude();
        final double lon = location.getLongitude();
        final String url = URLPostHelper.Coordenadas.REGISTRAR;
        final String nombre = usuarioName != null ? usuarioName : "";

        locationRepo.sendCoordinates(
                url,
                nombre,
                rol,
                docIdentidad,
                usuarioId,
                lat,
                lon,
                new LocationRepository.Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.w(TAG, "Failed to send coordinates", e);
                        // Registrar payload para depuración local (no persistir en producción sin control)
                        try {
                            Log.w(TAG, "Failed payload: nombre=" + nombre + " rol=" + rol + " doc=" + docIdentidad + " usuario_id=" + usuarioId + " lat=" + lat + " lon=" + lon);
                        } catch (Exception ignore) {}
                        // Intentar reintento simple programado (opcional)
                        // new Handler(Looper.getMainLooper()).postDelayed(() -> handleLocationUpdate(location), 5000);
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        try {
            if (streamer != null) streamer.stop();
        } catch (Exception ex) {
            Log.w(TAG, "Error stopping streamer", ex);
        }
        streamingStarted = false;
        isRunning = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}