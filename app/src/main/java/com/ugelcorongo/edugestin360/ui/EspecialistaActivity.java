package com.ugelcorongo.edugestin360.ui;

import com.ugelcorongo.edugestin360.R;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import androidx.core.app.ActivityCompat;
import com.ugelcorongo.edugestin360.utils.LocationUploadService;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Especialista;
import com.ugelcorongo.edugestin360.domain.models.Ficha;
import com.ugelcorongo.edugestin360.managers.FileUploadManager;
import com.ugelcorongo.edugestin360.managers.file.FileUpdater;
import com.ugelcorongo.edugestin360.managers.file.FileUpdaterFactory;
import com.ugelcorongo.edugestin360.managers.upload.AttendanceUploadManager;
import com.ugelcorongo.edugestin360.managers.upload.ImageUploadManager;
import com.ugelcorongo.edugestin360.managers.upload.PendingUploadProcessor;
import com.ugelcorongo.edugestin360.repository.DataRepository;
import com.ugelcorongo.edugestin360.storage.AttendancePrefs;
import com.ugelcorongo.edugestin360.storage.WorkLocation;
import com.ugelcorongo.edugestin360.ui.adapter.FichasAdapter;
import com.ugelcorongo.edugestin360.ui.viewmodel.EspecialistaViewModel;
import com.ugelcorongo.edugestin360.utils.LocationProvider;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.RawFileReader;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

public class EspecialistaActivity extends BaseRoleActivity {

    // UI
    private ImageButton btnAsistencia, btnFichas, btnEvidencias;
    private TextView lbl_especialista;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // ViewModel / Repo
    private EspecialistaViewModel viewModel;
    private DataRepository repo;
    private AttendancePrefs prefs;

    // Dynamic data
    private List<WorkLocation> workLocations = new ArrayList<>();
    private List<Ficha> fichas              = new ArrayList<>();
    private ColegioInfo colegioInfo;
    private Especialista insta_especialista;

    // Extras
    private String docIdentidad;
    private String especialistaName;
    private String especialistaId;

    // COLEGIO LOCALIZADO
    private String colegioLocalizado;
    private String colegioId;
    private String colegioDirector;
    // Para foto
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private Bitmap evidenciaBitmap;
    private Uri tempImageUri;
    private ImageView ivEvidenciaPreview;
    private static final double MAX_DISTANCE_M = 100.0;
    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final int PERM_REQUEST_ALL = 300;
    private static final int PERM_REQUEST_BACKGROUND = 301;
    private boolean serviceStarted = false;
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQ_LOC = 200;
    @Override
    protected Map<String, String> getFileUrlMapping() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("datacolegio.txt",
                URLPostHelper.Data.Colegios);
        m.put("datafichas.txt",
                URLPostHelper.Fichas.VER);
        m.put("datainfoespecialistas.txt",
                URLPostHelper.Data.EspecialistasInfo);
        return m;
    }

    @Override
    protected void loadDataFromFiles() {
        workLocations.clear();
        fichas.clear();

        // 1) Leer datacolegio.txt (interno o raw)
        try {
            List<String> colegioLines = FileUpdaterFactory.create(this,"datacolegio.txt", URLPostHelper.Data.Colegios).readLines();

            for (String line : colegioLines) {
                String[] parts = line.split(";");
                if (parts.length < 11) continue;    // validación mínima

                double lat = Double.parseDouble(parts[5]);
                double lon = Double.parseDouble(parts[6]);
                String data_idcolegio = parts[4];

                String director = parts[1];
                String codmodular = parts[7];
                String codlocal = parts[8];
                String clave8 = parts[9];
                String nivel = parts[10];
                String nombreDirector = parts[3];

                workLocations.add(
                        new WorkLocation(parts[0], lat, lon, data_idcolegio,
                                director, codmodular, codlocal, clave8, nivel, nombreDirector)
                );
            }
        } catch (Exception ex) {
        }

        // 2) Leer datainfoespecialistas.txt (interno o raw)
        try {
            List<String> esp = FileUpdaterFactory.create(this,"datainfoespecialistas.txt", URLPostHelper.Data.EspecialistasInfo).readLines();

            for (String line : esp) {
                String[] parts = line.split(";");
                if (parts.length < 3) continue;    // validación mínima

                if (parts[0].equalsIgnoreCase(docIdentidad)) {
                    especialistaName   = parts[1];
                    especialistaId   = parts[3];
                    String dni_especialista   = parts[0];
                    lbl_especialista.setText(especialistaName);
                    insta_especialista = new Especialista(especialistaId, dni_especialista, especialistaName);
                    break;
                }
            }
        } catch (Exception ex) {
        }


        // 3) Leer datafichas.txt y filtrar por docIdentidad
        try {
            List<String> fichaLines = FileUpdaterFactory
                    .create(this,
                            "datafichas.txt",
                            URLPostHelper.Fichas.VER)
                    .readLines();

            for (String line : fichaLines) {
                String[] p = line.split(";");

                // id[0]; nombre[1]; fi[2]; ft[3]; estado[4], nrovisita[5]; tipoFicha[6];
                if (p.length < 7) continue;
                Ficha f = new Ficha(
                        p[0], p[1], p[2], p[3],
                        p[4], p[5], p[6]
                );
                fichas.add(f);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_especialista);

        // Extras
        docIdentidad = getIntent().getStringExtra(LoginActivity.EXTRA_DOCIDENTIDAD);
        lbl_especialista = findViewById(R.id.lbl_especialista);

        // ViewModel + Repo
        viewModel = new ViewModelProvider(this)
                .get(EspecialistaViewModel.class);
        repo       = DataRepository.getInstance(this);
        prefs   = new AttendancePrefs(this);

        bindViews();
        setupActivityResultLaunchers();

        loadDataFromFiles();

        setupListeners();
        checkAndRequestAllPermissionsPersistent();
        debugMissingPermissionsAndSettings();

        // Procesar cualquier subida pendiente
        new Thread(() -> {
            new PendingUploadProcessor(this).processAll();
        }).start();
    }

    private void bindViews() {
        btnAsistencia = findViewById(R.id.btc_asistencias_especialista);
        btnFichas     = findViewById(R.id.btc_fichas_especialistas);
        btnEvidencias = findViewById(R.id.btc_evidencias_especialistas);
        tabLayout     = findViewById(R.id.tabSections);
        viewPager     = findViewById(R.id.vpSections);
    }

    private void setupActivityResultLaunchers() {
        cameraPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) dispatchCameraIntent();
                    else Toast.makeText(this,
                            "Permiso cámara denegado", Toast.LENGTH_SHORT).show();
                }
        );
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getResultCode()==RESULT_OK && res.getData()!=null) {
                        evidenciaBitmap = (Bitmap)res.getData().getExtras().get("data");
                        ivEvidenciaPreview.setImageBitmap(evidenciaBitmap);
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

    private interface BestLocCallback { void onResult(Location best); }

    private void getBestLocation(final int maxAttempts, final long attemptIntervalMs, final long maxAgeMs, final BestLocCallback cb) {
        final List<Location> samples = new ArrayList<>();
        final int[] tries = {0};

        final Runnable attempt = new Runnable() {
            @Override
            public void run() {
                tries[0]++;
                LocationProvider.requestSingle(EspecialistaActivity.this, loc -> {
                    if (loc != null) samples.add(loc);
                    // si ya tenemos una medición suficientemente precisa, terminamos temprano
                    Location best = selectBest(samples, maxAgeMs);
                    if (best != null && best.hasAccuracy() && best.getAccuracy() <= 20f) {
                        cb.onResult(best);
                        return;
                    }
                    if (tries[0] >= maxAttempts) {
                        // devolver la mejor que tengamos, puede ser null
                        cb.onResult(selectBest(samples, maxAgeMs));
                        return;
                    }
                    // programar siguiente intento después de interval
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, attemptIntervalMs);
                });
            }
        };

        // iniciar primer intento
        attempt.run();
    }

    private Location selectBest(List<Location> samples, long maxAgeMs) {
        long now = System.currentTimeMillis();
        Location best = null;
        float bestAcc = Float.MAX_VALUE;
        for (Location s : samples) {
            if (s == null) continue;
            if (now - s.getTime() > maxAgeMs) continue;
            float acc = s.hasAccuracy() ? s.getAccuracy() : Float.MAX_VALUE;
            if (acc < bestAcc) {
                bestAcc = acc;
                best = s;
            } else if (acc == bestAcc && best != null) {
                // desempate: preferir GPS provider
                if ("gps".equalsIgnoreCase(s.getProvider()) && !"gps".equalsIgnoreCase(best.getProvider())) {
                    best = s;
                }
            }
        }
        return best;
    }

    private void setupListeners() {
        btnAsistencia.setOnClickListener(v -> startAttendanceFlow());
        btnFichas    .setOnClickListener(v -> showFichasDialog());
        btnEvidencias.setOnClickListener(v -> showEvidenciaDialog());
    }

    // ← 1) ASISTENCIA --------------------------------------------------

    private void startAttendanceFlow() {
        debugMissingPermissionsAndSettings();
        if (!isLocationProviderEnabled()) {
            openLocationSettings();
            return;
        }
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        getBestLocation(3, 700L, 10_000L, bestLoc -> {
            if (bestLoc == null) {
                runOnUiThread(() -> Toast.makeText(this, "No se obtuvo ubicación precisa. Intente nuevamente.", Toast.LENGTH_LONG).show());
                return;
            }
            WorkLocation chosen = findNearest(bestLoc);
            if (chosen == null) {
                runOnUiThread(() -> Toast.makeText(this, "No dentro de rango (" + MAX_DISTANCE_M + "m)", Toast.LENGTH_LONG).show());
                return;
            }
            String tipo = getNextTipoRegistro(colegioLocalizado);
            if (tipo == null) {
                runOnUiThread(() -> Toast.makeText(this, "Ya registraste hoy en " + colegioLocalizado, Toast.LENGTH_LONG).show());
                return;
            }
            runOnUiThread(() -> showPopupAsistencia(
                    colegioLocalizado,
                    bestLoc.getLatitude(),
                    bestLoc.getLongitude(),
                    tipo
            ));
        });
    }

    private void showPopupAsistencia(String colegio,
                                     double lat, double lon, String tipoRegistro)
    {
        // 1) Inflar layout
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_asistencia, null);
        EditText etComentario = dialogView.findViewById(R.id.et_comentario);

        // 2) Construir el Builder con NULL en el listener positivo
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(tipoRegistro + " en " + colegio)
                .setView(dialogView)
                .setPositiveButton("Sí", null)    // listener null para evitar auto-dismiss
                .setNegativeButton("No", (d, which) -> d.dismiss());

        // 3) Crear y mostrar el diálogo
        final AlertDialog dlg = builder.create();
        dlg.show();

        // 4) Una vez mostrado, sobreescribir el OnClickListener del botón "Sí"
        Button btnSi = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSi.setOnClickListener(v -> {
            String comentario = etComentario.getText().toString().trim();
            String hora = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
            ).format(new Date());

            // Preparar metadata
            Map<String,String> meta = new HashMap<>();
            meta.put("docidentidad",  docIdentidad);
            meta.put("especialista",  especialistaName);
            meta.put("colegio",       colegio);
            meta.put("tipo_registro", tipoRegistro);
            meta.put("hora_registro", hora);
            meta.put("latitud",       String.valueOf(lat));
            meta.put("longitud",      String.valueOf(lon));
            meta.put("id_colegio",    colegioId);

            if (!comentario.isEmpty()) {
                meta.put("comentario", comentario);
            }

            AttendanceUploadManager mgr =
                    new AttendanceUploadManager(this,
                            URLPostHelper.Asistencia.REGISTRAR);

            if (NetworkUtil.isConnected(this)) {
                mgr.upload(meta, new AttendanceUploadManager.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(EspecialistaActivity.this,
                                tipoRegistro + " registrada",
                                Toast.LENGTH_LONG
                        ).show();
                        prefs.setRegistered(tipoRegistro, colegio);
                        dlg.dismiss();
                    }
                    @Override
                    public void onError(Throwable t) {
                        viewModel.enqueueUploadTask(
                                "ATTENDANCE", null, meta
                        );
                        Toast.makeText(EspecialistaActivity.this,
                                "Registro onError",
                                Toast.LENGTH_LONG
                        ).show();
                        dlg.dismiss();
                    }
                });
            } else {
                viewModel.enqueueUploadTask("ATTENDANCE", null, meta);
                Toast.makeText(EspecialistaActivity.this,
                        "Registro encolado por falta de red",
                        Toast.LENGTH_LONG
                ).show();
                dlg.dismiss();
            }
        });
    }

    // ← 2) FICHAS ------------------------------------------------------

    private void showFichasDialog() {
        debugMissingPermissionsAndSettings(); // <-- llamada previa
        if (!isLocationProviderEnabled()) {
            openLocationSettings();
            return;
        }
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        // 1) Pide ubicación y solo allí inflamos el diálogo
        getBestLocation(3, 700L, 10_000L, bestLoc -> {
            if (bestLoc == null) {
                runOnUiThread(() -> Toast.makeText(this, "No se obtuvo ubicación precisa. Intente nuevamente.", Toast.LENGTH_LONG).show());
                return;
            }
            WorkLocation chosen = findNearest(bestLoc);
            if (chosen == null) {
                runOnUiThread(() -> Toast.makeText(this, "No dentro de rango (" + MAX_DISTANCE_M + "m)", Toast.LENGTH_LONG).show());
                return;
            }

            runOnUiThread(() -> {
                View popup = getLayoutInflater().inflate(R.layout.dialog_fichas, null);
                ListView lv = popup.findViewById(R.id.listFichas);
                FichasAdapter adapter = new FichasAdapter(
                        this,
                        fichas,
                        colegioInfo,
                        colegioLocalizado,
                        colegioId + "",
                        especialistaName,
                        "Especialista",
                        especialistaId,
                        docIdentidad,
                        insta_especialista
                );
                lv.setAdapter(adapter);
                new AlertDialog.Builder(this)
                        .setTitle("Selecciona Ficha (" + colegioLocalizado + ")")
                        .setView(popup)
                        .setNegativeButton("Cerrar", (d,i)->d.dismiss())
                        .show();
            });
        });
    }

    // ← 3) EVIDENCIAS --------------------------------------------------

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
        btnTomar.setOnClickListener(v ->cameraPermLauncher.launch(Manifest.permission.CAMERA));

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

                Map<String,String> meta = new HashMap<>();
                meta.put("docidentidad", docIdentidad);
                meta.put("docente",      especialistaName);
                meta.put("latitud",      String.valueOf(lat));
                meta.put("longitud",     String.valueOf(lon));

                ImageUploadManager uploader = new ImageUploadManager(this);

                if (NetworkUtil.isConnected(this)) {
                    uploader.upload(imageUri, meta, new FileUploadManager.UploadCallback() {
                        @Override public void onSuccess() {
                            runOnUiThread(() ->
                                    Toast.makeText(
                                            EspecialistaActivity.this,
                                            "Evidencia subida",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                        @Override public void onError(Throwable t) {
                            viewModel.enqueueUploadTask("IMG", imageUri, meta);
                            runOnUiThread(() ->
                                    Toast.makeText(
                                            EspecialistaActivity.this,
                                            "Error al subir; foto encolada",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    });
                } else {
                    viewModel.enqueueUploadTask("IMG", imageUri, meta);
                    runOnUiThread(() ->
                            Toast.makeText(
                                    EspecialistaActivity.this,
                                    "Sin conexión: foto encolada",
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

    /**
     * Retorna el WorkLocation más cercano a loc, o null si supera MAX_DISTANCE_M.
     * También actualiza colegioLocalizado, colegioId y colegioInfo.
     */
    // Reemplazar findNearest(Location loc)
    private WorkLocation findNearest(Location loc) {
        if (loc == null) return null;

        // Filtrar por precisión y edad
        float accuracy = loc.hasAccuracy() ? loc.getAccuracy() : Float.MAX_VALUE;
        long ageMs = System.currentTimeMillis() - loc.getTime();
        if (accuracy > 50.0f || ageMs > 10_000L) {
            Log.d("FETCH_ERROR", "Location precision");
        }

        WorkLocation bestW = null;
        double bestD = Double.MAX_VALUE;

        for (WorkLocation w : workLocations) {
            double d = w.distanceTo(loc.getLatitude(), loc.getLongitude());
            if (d < bestD) {
                bestD = d;
                bestW = w;
            }
        }

        if (bestW != null && bestD <= MAX_DISTANCE_M) {
            colegioLocalizado = bestW.getName();
            colegioId        = bestW.getIdcolegio();
            colegioInfo = new ColegioInfo(
                    bestW.getName(),
                    bestW.getCodigoModular(),
                    bestW.getCodigoLocal(),
                    bestW.getClave8(),
                    bestW.getDirector(),
                    bestW.getNivel(),
                    bestW.getLatitude(),
                    bestW.getLongitude(),
                    bestW.getIdcolegio(),
                    bestW.getNombreDirector()
            );
            return bestW;
        }
        return null;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                REQUEST_LOCATION_PERMISSION
        );
    }

    private void dispatchCameraIntent() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(i);
    }

    /** 1. Determina si toca Entrada/Salida o ya completó ambos */
    private String getNextTipoRegistro(String colegio) {
        if (!prefs.isRegistered("Entrada", colegio)) return "Entrada";
        if (!prefs.isRegistered("Salida", colegio))  return "Salida";
        return null;
    }

    /** Cambios para subir las coordenadas */
    private void startLocationUploadService() {
        if (LocationUploadService.isRunning) return;
        Intent i = new Intent(this, LocationUploadService.class);
        i.putExtra(LocationUploadService.EXTRA_DOC, docIdentidad);
        i.putExtra(LocationUploadService.EXTRA_ID, especialistaId);
        i.putExtra(LocationUploadService.EXTRA_NAME, especialistaName);
        i.putExtra(LocationUploadService.EXTRA_ROL, "Especialista");

        try {
            ContextCompat.startForegroundService(this, i);
        } catch (Exception ex) {
            Log.d("FETCH_ERROR", "startForegroundService failed", ex);
        }
        //serviceStarted = true;
    }

    private void stopLocationUploadService() {
        if (!LocationUploadService.isRunning) return;
        Intent i = new Intent(this, LocationUploadService.class);
        stopService(i);
        //serviceStarted = false;
    }

    private void checkAndRequestAllPermissionsPersistent() {
        String[] normalPerms = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        List<String> toRequest = new ArrayList<>();
        for (String p : normalPerms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p);
            }
        }

        // If any normal permission missing -> request them (fine + camera + coarse)
        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERM_REQUEST_ALL);
            return;
        }

        // Normal permissions granted -> check location provider state
        if (!isLocationProviderEnabled()) {
            showPermissionsModal("La aplicación necesita estos permisos para funcionar:\n\n- Ubicación GPS\n- Cámara\n\nAdemás active los servicios de ubicación del dispositivo.", true);
            return;
        }

        // If Android Q+ request BACKGROUND separately (best-effort)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_BACKGROUND_LOCATION }, PERM_REQUEST_BACKGROUND);
                return;
            }
        }

        // All checks OK -> start upload service (but only if network available)
        if (NetworkUtil.isConnected(this)) startLocationUploadService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERM_REQUEST_ALL) {
            boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

            if (fine && camera) {
                // proceed to check provider and background
                if (!isLocationProviderEnabled()) {
                    showPermissionsModal("La aplicación necesita estos permisos para funcionar:\n\n- Ubicación GPS\n- Cámara\n\nActive el GPS en Ajustes.", true);
                    return;
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_BACKGROUND_LOCATION }, PERM_REQUEST_BACKGROUND);
                        return;
                    }
                }
                if (NetworkUtil.isConnected(this)) startLocationUploadService();
                return;
            }

            // some normal permission denied -> determine if permanent
            boolean anyPermanent = false;
            for (String p : permissions) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, p)) {
                        anyPermanent = true;
                        break;
                    }
                }
            }

            if (anyPermanent) {
                showPermissionsModal("La aplicación necesita estos permisos para funcionar:\n\n- Ubicación GPS\n- Cámara\n\nConceda permisos en Ajustes.", false);
            } else {
                // re-request politely
                showPermissionsModal("La aplicación necesita permisos de Ubicación y Cámara para continuar.", true);
            }
            return;
        }

        if (requestCode == PERM_REQUEST_BACKGROUND) {
            // best-effort: if denied, we continue with foreground updates but warn user
            boolean bg = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!bg) {
                // user denied background; show info but still allow foreground flow
                Toast.makeText(this, "Permiso ubicación en segundo plano no concedido. El envío funcionará mientras la app esté en primer plano.", Toast.LENGTH_LONG).show();
            }
            if (NetworkUtil.isConnected(this)) startLocationUploadService();
        }
    }

    private void showPermissionsModal(String message, boolean showRetry) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Cerrar", (d, w) -> d.dismiss());

        b.setPositiveButton(showRetry ? "Ajustes" : "Ajustes", (d, w) -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        });

        AlertDialog dlg = b.create();
        dlg.show();
    }

    private void debugMissingPermissionsAndSettings() {
        List<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Ubicación ");
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Cámara");
        }
        boolean gpsEnabled = isLocationProviderEnabled();
        StringBuilder sb = new StringBuilder();
        if (!missing.isEmpty()) {
            for (String m : missing) sb.append("- ").append(m).append("\n");
        }
        if (!gpsEnabled) sb.append("- Servicios de ubicación desactivados\n");
        if (sb.length() > 0) {
            // show modal listing missing items (exact text requested)
            showPermissionsModal("La aplicación necesita estos permisos para funcionar:\n\n- Ubicación GPS\n- Cámara\n\nDebe activarlos para continuar", true);
        }
    }

    private boolean isLocationProviderEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return false;
        boolean gps = false;
        boolean network = false;
        try { gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
        try { network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
        return gps || network;
    }

    private void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        stopLocationUploadService(); // detener servicio si la Activity se destruye
        super.onDestroy();
    }
}