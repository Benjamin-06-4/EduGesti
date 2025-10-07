package com.ugelcorongo.edugestin360.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;

public class LocationProvider {

    private final FusedLocationProviderClient client;

    // Renombramos la interfaz para no chocar con la clase de Google
    public interface OnLocationResultListener {
        void onLocation(Location location) throws IOException;
    }

    public LocationProvider(Context ctx) {
        this.client = LocationServices.getFusedLocationProviderClient(ctx);
    }

    public static void requestSingle(Context ctx, OnLocationResultListener listener) {
        LocationProvider provider = new LocationProvider(ctx);
        provider.getLastLocation(listener);
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation(OnLocationResultListener listener) {
        client.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        try {
                            listener.onLocation(loc);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        requestFreshLocation(listener);
                    }
                })
                .addOnFailureListener(e -> requestFreshLocation(listener));
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation(OnLocationResultListener listener) {
        // Creamos la petición de alta precisión
        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5_000)
                .setNumUpdates(1);

        // Instancia del callback de Google
        LocationCallback googleCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                try {
                    listener.onLocation(loc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // Eliminamos esta misma instancia de callback
                client.removeLocationUpdates(this);
            }
        };

        // Pedimos la actualización
        client.requestLocationUpdates(req, googleCallback, Looper.getMainLooper());
    }
}