package com.ugelcorongo.edugestin360.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.managers.file.DocenteInfoParser;
import com.ugelcorongo.edugestin360.managers.file.FileUpdater;
import com.ugelcorongo.edugestin360.managers.file.FileUpdaterFactory;
import com.ugelcorongo.edugestin360.utils.LocationProvider;
import com.ugelcorongo.edugestin360.managers.FileUploadManager;
import com.ugelcorongo.edugestin360.managers.upload.PdfUploadManager;
import com.ugelcorongo.edugestin360.managers.upload.ImageUploadManager;
import com.ugelcorongo.edugestin360.storage.WorkLocation;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;
import com.ugelcorongo.edugestin360.ui.viewmodel.DocenteViewModel;

import java.io.IOException;
import java.util.*;

public class DocenteActivity extends BaseRoleActivity {

    private ImageButton btcAsistencia;
    private ImageButton btcSesionPdf;
    private ImageButton btcEvidencia;

    private DocenteViewModel viewModel;
    private List<WorkLocation> workLocations = new ArrayList<>();
    private List<String> availableColegios = new ArrayList<>();

    // PARA PDF
    private ActivityResultLauncher<String> pdfPickerLauncher;
    private Uri selectedPdfUri;
    private TextView tvNombreArchivo, txtDocente;

    // PARA FOTO
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private Bitmap evidenciaBitmap;
    private ImageView ivEvidenciaPreview;
    private String docente, docidentidad;
    private EditText etComentarioPdf;

    // --- BaseRoleActivity overrides ------------------

    @Override
    protected Map<String, String> getFileUrlMapping() {
        // Se actualizará datainfodocente.txt al arrancar
        return Collections.singletonMap(
                "datainfodocente.txt",
                URLPostHelper.Data.DocentesInfo
        );
    }

    @Override
    protected void loadDataFromFiles() {
        workLocations.clear();
        availableColegios.clear();
        // Si tuviste 7 campos antes, adapta a 4 o renombra tu parser
        try {
            FileUpdater reader = FileUpdaterFactory
                    .create(this, "datainfodocente.txt", URLPostHelper.Data.DocentesInfo);
            List<String> lines = reader.readLines();

            // parseo genérico
            List<DocenteInfoParser> lista = DocenteInfoParser.parseLines(lines);
            Set<String> set = new LinkedHashSet<>();
            for (DocenteInfoParser info : lista) {
                if (info.docente.equalsIgnoreCase(getIntent().getStringExtra("docente"))) {
                    // Ejemplo: convertir nivel a WorkLocation u otro modelo
                    workLocations.add(new WorkLocation(
                            info.nivel, 0.0, 0.0, "999",
                            "","","","","",""
                    ));
                }
                if (info.docidentidad.equalsIgnoreCase(this.docidentidad)) {
                    this.docente = info.docente;
                    set.add(info.colegio);
                }
            }
            txtDocente.setText(docente);
            availableColegios.addAll(set);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error leyendo docentes: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // --- Activity Lifecycle ---------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docente);
        txtDocente = findViewById(R.id.txtDocente);

        // 2) Recuperar userId de extras (LoginActivity ya lo envió)
        docidentidad = getIntent().getStringExtra(LoginActivity.EXTRA_DOCIDENTIDAD);


        viewModel = new ViewModelProvider(this).get(DocenteViewModel.class);

        bindViews();
        setupActivityResultLaunchers();
        setupListeners();
    }

    private void bindViews() {
        btcAsistencia  = findViewById(R.id.btc_asistencia);
        btcSesionPdf   = findViewById(R.id.btc_sesionpdf);
        btcEvidencia   = findViewById(R.id.btc_evidencia);
    }

    private void setupActivityResultLaunchers() {
        // Picker de PDF
        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    selectedPdfUri = uri;
                    String name = getFileName(uri);
                    if (tvNombreArchivo != null) {
                        tvNombreArchivo.setText(name);
                    }
                }
        );

        // Permiso y cámara
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) openCamera();
                    else Toast.makeText(this, "Permiso Cámara denegado", Toast.LENGTH_SHORT).show();
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        evidenciaBitmap = (Bitmap) result.getData().getExtras().get("data");
                        if (ivEvidenciaPreview != null) {
                            ivEvidenciaPreview.setImageBitmap(evidenciaBitmap);
                        }
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(
                                this,
                                "Permiso de ubicación denegado",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    // Si ya estaba granted, el flujo btnSubir se encargará de llamar LocationProvider
                }
        );
    }

    private void setupListeners() {
        btcAsistencia.setOnClickListener(v -> showLocationDialog());
        btcSesionPdf.setOnClickListener(v -> showPdfDialog());
        btcEvidencia.setOnClickListener(v -> showEvidenciaDialog());
    }

    // --- 1) Asistencia con ubicación dinámica --------

    private void showLocationDialog() {
        if (workLocations.isEmpty()) {
            Toast.makeText(this, "No hay ubicaciones disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extraemos solo los nombres para el diálogo
        String[] locationNames = new String[workLocations.size()];
        for (int i = 0; i < workLocations.size(); i++) {
            locationNames[i] = workLocations.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Seleccione ubicación")
                .setItems(locationNames, (dialog, which) -> {
                    // Aquí `which` es el índice de la opción elegida
                    WorkLocation selected = workLocations.get(which);

                    // Al escoger, pedimos la ubicación actual y registramos
                    LocationProvider.requestSingle(this, location -> {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();

                        // Ya existe WorkLocation.getName(), no hay error
                        viewModel.registerAttendance(
                                selected.getName(),
                                lat,
                                lon
                        );
                        Toast.makeText(
                                this,
                                "Asistencia registrada en: " + selected.getName(),
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                })
                .show();
    }

    // --- 2) Subida de PDF ------------------------------

    private void showPdfDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_pdf, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Select option de colegios
        Spinner spinnerColegios = dialogView.findViewById(R.id.spinnerColegios);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                availableColegios
        );
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerColegios.setAdapter(adapter);

        tvNombreArchivo = dialogView.findViewById(R.id.tvNombreArchivo);
        Button btnElegir   = dialogView.findViewById(R.id.btnElegirArchivo);
        Button btnSubir    = dialogView.findViewById(R.id.btnSubirPdf);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarPdf);
        etComentarioPdf = dialogView.findViewById(R.id.etComentarioPdf);

        btnElegir.setOnClickListener(v ->
                pdfPickerLauncher.launch("application/pdf")
        );

        btnSubir.setOnClickListener(v -> {
            if (selectedPdfUri == null) {
                Toast.makeText(this, "Seleccione un PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pedir permiso de ubicación si no está dado
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }

            LocationProvider.requestSingle(this, location -> {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                // 3) Construir meta con lat y lon
                String colegio    = (String) spinnerColegios.getSelectedItem();
                String auxdocente = txtDocente.getText().toString().trim();
                String comentario = etComentarioPdf.getText().toString().trim();

                Map<String,String> meta = new HashMap<>();
                meta.put("docidentidad", docidentidad);
                meta.put("docente",      auxdocente);
                meta.put("colegio",      colegio);
                meta.put("latitud",      String.valueOf(lat));
                meta.put("longitud",     String.valueOf(lon));
                if (!comentario.isEmpty()) meta.put("comentario", comentario);

                PdfUploadManager uploader = new PdfUploadManager(this);

                if (NetworkUtil.isConnected(this)) {
                    uploader.upload(selectedPdfUri, meta, new FileUploadManager.UploadCallback() {
                        @Override public void onSuccess() {
                            runOnUiThread(() ->
                                    Toast.makeText(
                                            DocenteActivity.this,
                                            "PDF subido correctamente",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                        @Override public void onError(Throwable t) {
                            viewModel.enqueueUploadTask("PDF", selectedPdfUri, meta);
                            runOnUiThread(() ->
                                    Toast.makeText(
                                            DocenteActivity.this,
                                            "Error al subir; tarea encolada",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    });
                } else {
                    viewModel.enqueueUploadTask("PDF", selectedPdfUri, meta);
                    runOnUiThread(() ->
                            Toast.makeText(
                                    DocenteActivity.this,
                                    "Sin conexión: tarea encolada",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }

                dialog.dismiss();
            });
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- 3) Subida de Evidencia (Foto) -----------------

    private void showEvidenciaDialog() {
        View dialogView = LayoutInflater
                .from(this)
                .inflate(R.layout.dialog_evidencia, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        ivEvidenciaPreview = dialogView.findViewById(R.id.ivEvidenciaPreview);
        Button btnTomar   = dialogView.findViewById(R.id.btnTomarFoto);
        Button btnSubir   = dialogView.findViewById(R.id.btnSubirEvidencia);
        Button btnCancelar= dialogView.findViewById(R.id.btnCancelarEvidencia);

        // 1) Toma la foto
        btnTomar.setOnClickListener(v ->cameraPermissionLauncher.launch(Manifest.permission.CAMERA));

        // 2) Sube o encola
        btnSubir.setOnClickListener(v -> {
            if (evidenciaBitmap == null) {
                Toast.makeText(
                        this,
                        "Tome foto primero",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }

            LocationProvider.requestSingle(this, location -> {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                // Convertir bitmap a Uri temporal
                String path = MediaStore.Images.Media.insertImage(
                        getContentResolver(), evidenciaBitmap, null, null
                );
                Uri imageUri = Uri.parse(path);

                String auxdocente = txtDocente.getText().toString().trim();
                Map<String,String> meta = new HashMap<>();
                meta.put("docidentidad", docidentidad);
                meta.put("docente",      auxdocente);
                meta.put("latitud",      String.valueOf(lat));
                meta.put("longitud",     String.valueOf(lon));

                ImageUploadManager uploader = new ImageUploadManager(this);

                if (NetworkUtil.isConnected(this)) {
                    uploader.upload(imageUri, meta, new FileUploadManager.UploadCallback() {
                        @Override public void onSuccess() {
                            runOnUiThread(() ->
                                    Toast.makeText(
                                            DocenteActivity.this,
                                            "Evidencia subida OK",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                        @Override public void onError(Throwable t) {
                            viewModel.enqueueUploadTask("IMG", imageUri, meta);
                            runOnUiThread(() ->
                                    Toast.makeText(
                                            DocenteActivity.this,
                                            "Error al subir; tarea encolada",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    });
                } else {
                    viewModel.enqueueUploadTask("IMG", imageUri, meta);
                    runOnUiThread(() ->
                            Toast.makeText(
                                    DocenteActivity.this,
                                    "Sin conexión: tarea encolada",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }

                dialog.dismiss();
            });
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- Helpers ---------------------------------------

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            String path = uri.getPath();
            int cut = path.lastIndexOf('/');
            result = (cut >= 0 ? path.substring(cut + 1) : path);
        }
        return result;
    }
}