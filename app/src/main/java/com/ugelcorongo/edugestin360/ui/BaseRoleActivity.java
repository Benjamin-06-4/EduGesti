package com.ugelcorongo.edugestin360.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ugelcorongo.edugestin360.managers.DataFileManager;
import com.ugelcorongo.edugestin360.managers.file.FileUpdater;
import com.ugelcorongo.edugestin360.managers.file.FileUpdaterFactory;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseRoleActivity extends AppCompatActivity {

    /** Mapa <nombreArchivo, URLHelper...()> específico de cada rol */
    protected abstract Map<String, String> getFileUrlMapping();

    /** Carga datos desde archivos y actualiza UI */
    protected abstract void loadDataFromFiles();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Creamos un executor de un solo hilo para I/O de archivos/red
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 2) Ejecutamos la descarga/actualización fuera del hilo UI
        executor.execute(() -> {
            for (Map.Entry<String, String> entry : getFileUrlMapping().entrySet()) {
                String fileName = entry.getKey();
                String url      = entry.getValue();
                FileUpdater upd = FileUpdaterFactory.create(this, fileName, url);

                if (upd.shouldUpdate()) {
                    try {
                        upd.update();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        // En caso de fallo, mostrar Toast en UI thread
                        runOnUiThread(() -> Toast.makeText(
                                BaseRoleActivity.this,
                                "No se pudo actualizar " + fileName + ": " + ex.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
                    }
                }
            }

            // 3) Una vez finalizada la I/O, cargar datos y refrescar UI en main thread
            runOnUiThread(this::loadDataFromFiles);
        });

        // Nota: no invocamos loadDataFromFiles() aquí, lo hará el executor
    }
}