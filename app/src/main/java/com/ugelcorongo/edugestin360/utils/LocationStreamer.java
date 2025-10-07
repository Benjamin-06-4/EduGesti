package com.ugelcorongo.edugestin360.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public final class LocationStreamer {

    public interface StreamCallback {
        void onUpdate(Location location);
        void onError(Exception e);
    }

    private final FusedLocationProviderClient client;
    private LocationCallback callback;

    public LocationStreamer(Context ctx) {
        this.client = LocationServices.getFusedLocationProviderClient(ctx.getApplicationContext());
    }

    /**
     * Inicia streaming de ubicación de alta precisión.
     * Requiere permisos FINE/COARSE y, para background, ACCESS_BACKGROUND_LOCATION.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void start(@Nullable Long intervalMs, StreamCallback cb) {
        stop(); // evita callbacks duplicados
        if (intervalMs == null || intervalMs <= 0L) intervalMs = 5000L;

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(intervalMs)
                .setFastestInterval(Math.max(1000L, intervalMs / 2L))
                .setSmallestDisplacement(0f);

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) {
                    cb.onError(new Exception("LocationResult null"));
                    return;
                }
                Location l = result.getLastLocation();
                if (l != null) cb.onUpdate(l);
            }
        };

        try {
            client.requestLocationUpdates(req, callback, Looper.getMainLooper());
        } catch (SecurityException ex) {
            cb.onError(ex);
        }
    }

    /**
     * Detiene el streaming si está activo.
     */
    public void stop() {
        if (client != null && callback != null) {
            client.removeLocationUpdates(callback);
            callback = null;
        }
    }
}