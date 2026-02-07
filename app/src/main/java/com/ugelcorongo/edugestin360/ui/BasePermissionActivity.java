package com.ugelcorongo.edugestin360.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public abstract class BasePermissionActivity extends AppCompatActivity {

    // Request codes
    protected static final int REQ_PERM_FINE = 1000;
    protected static final int REQ_PERM_COARSE = 1001;
    protected static final int REQ_PERM_STORAGE = 1003; // estructura
    protected static final int REQ_PERM_INTERNET = 1002; // estructura

    // Query methods
    protected boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean hasInternetPermission() {
        // INTERNET
        return true;
    }

    // Request methods
    protected void requestFineLocationPermission() {
        if (hasFineLocationPermission()) { onPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION); return; }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationaleDialog("La aplicación necesita permiso de ubicación para localizar colegios y registrar asistencias. ¿Conceder ahora?", true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQ_PERM_FINE);
        }
    }

    protected void requestCoarseLocationPermission() {
        if (hasCoarseLocationPermission()) { onPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION); return; }
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, REQ_PERM_COARSE);
    }

    protected void requestStoragePermission() {
        // Almacenamiento
        ActivityCompat.requestPermissions(this, new String[]{ android.Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQ_PERM_STORAGE);
    }

    protected void requestInternetPermission() {
        // estructura only, no runtime request required
        onPermissionGranted("android.permission.INTERNET");
    }

    // UI helpers
    protected void showPermissionRationaleDialog(String message, boolean showSettingsButton) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Cerrar", (d,w) -> d.dismiss());
        b.setPositiveButton("Ajustes", (d,w) -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        });
        b.create().show();
    }

    protected void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    // Callbacks for subclasses
    protected void onPermissionGranted(String permission) {}
    protected void onPermissionDenied(String permission) {}
    protected void onPermissionDeniedPermanently(String permission) {}

    // Handle runtime results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_PERM_FINE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show();
                onPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                boolean shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (shouldShow) {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    onPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION);
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    showPermissionRationaleDialog("La aplicación necesita permiso de Ubicación para funcionar. Conceda permisos en Ajustes.", false);
                    onPermissionDeniedPermanently(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
            return;
        }

        if (requestCode == REQ_PERM_COARSE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) onPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION);
            else onPermissionDenied(Manifest.permission.ACCESS_COARSE_LOCATION);
            return;
        }

        if (requestCode == REQ_PERM_STORAGE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) onPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            else onPermissionDenied(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
