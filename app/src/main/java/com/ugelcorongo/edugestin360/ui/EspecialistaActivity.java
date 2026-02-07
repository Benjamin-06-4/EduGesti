package com.ugelcorongo.edugestin360.ui;

import com.ugelcorongo.edugestin360.R;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Especialista;
import com.ugelcorongo.edugestin360.domain.models.Ficha;
import com.ugelcorongo.edugestin360.managers.FileUploadManager;
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
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import org.json.JSONArray;
import org.json.JSONObject;

public class EspecialistaActivity extends BaseRoleActivity {

    // UI
    private ImageButton btnAsistencia, btnFichas, btnEvidencias, btnReportes, btnGenerate;
    private ImageButton btnMonitoreoDirectivo, btnMonitoreoDocente;
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
    private static final String REPORTS_CHANNEL_ID = "reports_channel_id";
    private static final int REPORT_NOTIFICATION_ID = 0;
    private Bitmap evidenciaBitmap;
    private Uri tempImageUri;
    private ImageView ivEvidenciaPreview;
    private volatile String lastGeneratedReportPath;
    private BroadcastReceiver providersChangeReceiver;
    private volatile boolean isRecapturing = false;
    private boolean lastGpsEnabledState = false;
    private boolean lastPermissionState = false;
    private static final double MAX_DISTANCE_M = 150.0;
    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final int PERM_REQUEST_ALL = 300;
    private static final int PERM_REQUEST_BACKGROUND = 301;
    private boolean serviceStarted = false;
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQ_LOC = 200;

    private static final String PREFS_NAME = "app_consent_prefs";
    private static final String KEY_CONSENT_ESPECIALISTA = "consent_especialista";
    private static final String KEY_CONSENT_DOCENTE = "consent_docente";
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
                if (p.length < 8) continue;
                Ficha f = new Ficha(
                        p[0], p[1], p[2], p[3],
                        p[4], p[5], p[6], p[7]
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.POST_NOTIFICATIONS }, 400);
            }
        }

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
        btnMonitoreoDirectivo = findViewById(R.id.btc_monitoreo_directivo);
        btnMonitoreoDocente   = findViewById(R.id.btc_monitoreo_docente);
        btnEvidencias = findViewById(R.id.btc_evidencias_especialistas);
        btnReportes = findViewById(R.id.btc_reportes_especialistas);
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
                        //Toast.makeText(
                                //this,
                                //"Permiso de ubicación denegado",
                                //Toast.LENGTH_SHORT
                        //).show();
                        showPermissionRequiredDialog();
                    } else {
                        recaptureLocationAfterPermission();
                    }
                }
        );
    }

    private interface BestLocCallback { void onResult(Location best); }

    private void getBestLocation(final long timeoutMs, final BestLocCallback cb) {
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        final LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) { cb.onResult(null); return; }

        final List<Location> samples = Collections.synchronizedList(new ArrayList<>());
        final android.location.LocationListener listener = new android.location.LocationListener() {
            @Override public void onLocationChanged(Location location) { if (location != null) samples.add(location); }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        try {
            try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, listener, android.os.Looper.getMainLooper()); } catch (SecurityException ignored) {}

            final long start = System.currentTimeMillis();
            final Runnable timeoutRunnable = new Runnable() {
                @Override public void run() {
                    try { lm.removeUpdates(listener); } catch (Exception ignored) {}
                    Location best = selectBest(samples, timeoutMs);
                    cb.onResult(best);
                }
            };
            h.postDelayed(timeoutRunnable, timeoutMs);

            final Runnable evaluate = new Runnable() {
                @Override public void run() {
                    Location best = selectBest(samples, timeoutMs);
                    if (best != null && best.hasAccuracy() && best.getAccuracy() <= 15f) {
                        h.removeCallbacks(timeoutRunnable);
                        try { lm.removeUpdates(listener); } catch (Exception ignored) {}
                        cb.onResult(best);
                        return;
                    }
                    if (System.currentTimeMillis() - start >= timeoutMs) return;
                    h.postDelayed(this, 500L);
                }
            };
            h.postDelayed(evaluate, 500L);
        } catch (Throwable t) {
            try { lm.removeUpdates(listener); } catch (Exception ignored) {}
            cb.onResult(null);
        }
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
        // Listener para Monitoreo Directivo (tipo "Director")
        btnMonitoreoDirectivo.setOnClickListener(v -> {
            debugMissingPermissionsAndSettings();

            if (!isLocationProviderEnabled()) {
                openLocationSettings();
                return;
            }
            if (!hasLocationPermission()) {
                requestLocationPermission();
                return;
            }

            // Obtener mejor ubicación antes de permitir elegir área
            getBestLocation(8000L, bestLoc -> {
                if (bestLoc == null) {
                    runOnUiThread(() -> Toast.makeText(
                            EspecialistaActivity.this,
                            "No se obtuvo ubicación precisa. Intente nuevamente.",
                            Toast.LENGTH_LONG
                    ).show());
                    return;
                }

                WorkLocation chosen = findNearestAllowAnyDistance(bestLoc);

                // En este punto colegioLocalizado/colegioId/colegioInfo ya están actualizados
                runOnUiThread(() -> showMonitoreoAreaDialog("Director"));
            });
        });

        // Listener para Monitoreo Docente (tipo "Docente")
        btnMonitoreoDocente.setOnClickListener(v -> {
            debugMissingPermissionsAndSettings();

            if (!isLocationProviderEnabled()) {
                openLocationSettings();
                return;
            }
            if (!hasLocationPermission()) {
                requestLocationPermission();
                return;
            }

            // Obtener mejor ubicación antes de permitir elegir área
            getBestLocation(8000L, bestLoc -> {
                if (bestLoc == null) {
                    runOnUiThread(() -> Toast.makeText(
                            EspecialistaActivity.this,
                            "No se obtuvo ubicación precisa. Intente nuevamente.",
                            Toast.LENGTH_LONG
                    ).show());
                    return;
                }

                WorkLocation chosen = findNearestAllowAnyDistance(bestLoc);

                // En este punto colegioLocalizado/colegioId/colegioInfo ya están actualizados por findNearest
                runOnUiThread(() -> showMonitoreoAreaDialog("Docente"));
            });
        });

        btnReportes.setOnClickListener(v -> showReportesDialog());

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
            ensureConsentAndRequestLocation("Especialista");
            return;
        }

        getBestLocation(8000L, bestLoc -> {
            if (bestLoc == null) {
                runOnUiThread(() -> Toast.makeText(this, "No se obtuvo ubicación precisa. Intente nuevamente.", Toast.LENGTH_LONG).show());
                return;
            }

            WorkLocation chosen = findNearestAllowAnyDistance(bestLoc);

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

    private void showMonitoreoAreaDialog(String tipoFichaFilter) {
        if (tipoFichaFilter == null) return;

        if (tipoFichaFilter.equalsIgnoreCase("Docente")) {
            // Para Docente: listar directamente las fichas filtradas solo por tipo
            List<Ficha> filtered = filterFichasByTipo("Docente");
            if (filtered.isEmpty()) {
                Toast.makeText(this, "No hay fichas para Docente", Toast.LENGTH_LONG).show();
                return;
            }
            showFichasListDialog(filtered);
            return;
        }

        // Si no es Docente (ej. Director) mantenemos el flujo por áreas
        String[] areas = new String[] { "AGA", "AGI", "AGP" };

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Selecciona Área Correspondiente")
                .setCancelable(true);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(16 * getResources().getDisplayMetrics().density);
        ll.setPadding(padding,padding,padding,padding);

        for (String area : areas) {
            Button btn = new Button(this);
            btn.setText(area);
            btn.setOnClickListener(v -> {
                List<Ficha> filtered = filterFichas(tipoFichaFilter, area);
                if (filtered.isEmpty()) {
                    Toast.makeText(this, "No hay fichas para " + tipoFichaFilter + " / " + area, Toast.LENGTH_LONG).show();
                } else {
                    showFichasListDialog(filtered);
                }
            });
            ll.addView(btn);
        }

        AlertDialog dlg = b.setView(ll).create();
        dlg.show();
    }

    private List<Ficha> filterFichas(String tipoFichaFilter, String areaFilter) {
        List<Ficha> out = new ArrayList<>();
        if (fichas == null) return out;
        for (Ficha f : fichas) {
            if (f == null) continue;
            String tipo = f.getTipoFicha() != null ? f.getTipoFicha().trim() : "";
            String area = f.getArea() != null ? f.getArea().trim() : "";
            if (tipo.equalsIgnoreCase(tipoFichaFilter) && area.equalsIgnoreCase(areaFilter)) {
                out.add(f);
            }
        }
        return out;
    }

    private void showFichasListDialog(List<Ficha> filteredFichas) {
        runOnUiThread(() -> {
            View popup = getLayoutInflater().inflate(R.layout.dialog_fichas, null);
            ListView lv = popup.findViewById(R.id.listFichas);
            // crear adapter con la lista filtrada
            FichasAdapter adapter = new FichasAdapter(
                    this,
                    filteredFichas,
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
    }

    private void showReportesDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reportes_especialistas, null);

        Spinner spTipo    = dialogView.findViewById(R.id.spTipoFicha);
        Spinner spArea    = dialogView.findViewById(R.id.spArea);
        Spinner spFicha   = dialogView.findViewById(R.id.spFicha);
        AutoCompleteTextView actColegio = dialogView.findViewById(R.id.actColegio);
        Spinner spMes     = dialogView.findViewById(R.id.spMes);
        Spinner spAno     = dialogView.findViewById(R.id.spAno);
        Button btnGenerate = dialogView.findViewById(R.id.btnGenerateReport);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelReport);

        // Tipo
        String[] tipos = new String[] { "Docente", "Director" };
        ArrayAdapter<String> tipoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipos);
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipo.setAdapter(tipoAdapter);

        // Area
        String[] areas = new String[] { "AGA", "AGI", "AGP" };
        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, areas);
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spArea.setAdapter(areaAdapter);

        // Mes
        String[] meses = new java.text.DateFormatSymbols(Locale.getDefault()).getMonths();
        String[] meses12 = Arrays.copyOf(meses, 12);
        ArrayAdapter<String> mesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, meses12);
        mesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMes.setAdapter(mesAdapter);
        spMes.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        // Año (últimos 6)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> anos = new ArrayList<>();
        for (int y = currentYear; y >= currentYear - 5; y--) anos.add(String.valueOf(y));
        ArrayAdapter<String> anoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, anos);
        anoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAno.setAdapter(anoAdapter);

        // Colegio (searchable)
        List<String> colegioNames = new ArrayList<>();
        for (WorkLocation w : workLocations) colegioNames.add(w.getName());
        ArrayAdapter<String> colegioAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, colegioNames);
        actColegio.setAdapter(colegioAdapter);
        actColegio.setThreshold(1);

        // Fichas initial fill (all)
        List<Ficha> allFichas = fichas != null ? fichas : new ArrayList<>();
        List<String> fichaNames = new ArrayList<>();
        for (Ficha f : allFichas) fichaNames.add(f.getNombre());
        if (fichaNames.isEmpty()) fichaNames.add("No hay fichas");
        ArrayAdapter<String> fichaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fichaNames);
        fichaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFicha.setAdapter(fichaAdapter);

        // Helper to refill fichas
        AdapterView.OnItemSelectedListener refillFichasListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tipoSel = (String) spTipo.getSelectedItem();
                String areaSel = spArea.getSelectedItem() != null ? (String) spArea.getSelectedItem() : "";
                List<Ficha> filtered = filterFichasForDialog(tipoSel, areaSel);
                List<String> names = new ArrayList<>();
                for (Ficha f : filtered) names.add(f.getNombre());
                if (names.isEmpty()) names.add("No hay fichas");
                ArrayAdapter<String> a = new ArrayAdapter<>(EspecialistaActivity.this, android.R.layout.simple_spinner_item, names);
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spFicha.setAdapter(a);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isDocente = "Docente".equalsIgnoreCase((String) spTipo.getSelectedItem());
                spArea.setEnabled(!isDocente);
                spArea.setAlpha(isDocente ? 0.5f : 1f);
                refillFichasListener.onItemSelected(spTipo, null, position, id);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spArea.setOnItemSelectedListener(refillFichasListener);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dlg.dismiss());

        btnGenerate.setOnClickListener(v -> {
            String tipoSel = (String) spTipo.getSelectedItem();
            String areaSel = spArea.isEnabled() ? (String) spArea.getSelectedItem() : "";
            String fichaSel = spFicha.getSelectedItem() != null ? (String) spFicha.getSelectedItem() : "";
            String colegioSel = actColegio.getText().toString().trim();
            String mesSel = (String) spMes.getSelectedItem();
            String anoSel = (String) spAno.getSelectedItem();

            Ficha chosenFicha = null;
            for (Ficha f : allFichas) if (f.getNombre().equalsIgnoreCase(fichaSel)) { chosenFicha = f; break; }

            WorkLocation chosenColegio = null;
            for (WorkLocation w : workLocations) if (w.getName().equalsIgnoreCase(colegioSel)) { chosenColegio = w; break; }

            Map<String,String> meta = new HashMap<>();
            meta.put("tipoReporte", "ESPECIALISTA");
            meta.put("tipoFicha", tipoSel);
            meta.put("area", areaSel == null ? "" : areaSel);
            meta.put("nombreFicha", fichaSel == null ? "" : fichaSel);
            meta.put("colegio", colegioSel == null ? "" : colegioSel);
            meta.put("mes", mesSel == null ? "" : mesSel);
            meta.put("ano", anoSel == null ? "" : anoSel);
            meta.put("generado_por", especialistaName != null ? especialistaName : "");
            meta.put("fecha_generacion", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            generateReportBackground(meta, chosenFicha, chosenColegio);
            dlg.dismiss();
        });

        dlg.show();
    }

    // EVIDENCIAS
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
    // Nuevo: busca y actualiza colegioLocalizado/colegioId/colegioInfo sin verificar MAX_DISTANCE_M
    private WorkLocation findNearestAllowAnyDistance(Location loc) {
        if (loc == null) return null;

        WorkLocation bestW = null;
        double bestD = Double.MAX_VALUE;

        for (WorkLocation w : workLocations) {
            double d = w.distanceTo(loc.getLatitude(), loc.getLongitude());
            if (d < bestD) {
                bestD = d;
                bestW = w;
            }
        }

        if (bestW != null) {
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
        }
        return bestW;
    }

    private void recaptureLocationAfterPermission() {
        if (isRecapturing) return;
        if (!hasLocationPermission()) return;
        if (!isLocationProviderEnabled()) return;
        isRecapturing = true;

        final LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        final android.location.LocationListener warmListener = new android.location.LocationListener() {
            @Override public void onLocationChanged(Location location) {}
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        try {
            try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, warmListener); } catch (SecurityException ignored) {}
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                getBestLocation(8000L, bestLoc -> {
                    try { lm.removeUpdates(warmListener); } catch (Exception ignored) {}
                    isRecapturing = false;
                    if (bestLoc != null) {
                        findNearestAllowAnyDistance(bestLoc);
                        runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "Ubicación GPS obtenida", Toast.LENGTH_SHORT).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "No se obtuvo ubicación GPS precisa. Intente nuevamente.", Toast.LENGTH_LONG).show());
                    }
                });
            }, 1200L);
        } catch (Throwable t) {
            isRecapturing = false;
            try { lm.removeUpdates(warmListener); } catch (Exception ignored) {}
        }
    }

    private boolean hasConsentForRole(String key) {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(key, false);
    }

    private void setConsentForRole(String key, boolean value) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    private List<Ficha> filterFichasByTipo(String tipoFichaFilter) {
        List<Ficha> out = new ArrayList<>();
        if (fichas == null) return out;
        for (Ficha f : fichas) {
            if (f == null) continue;
            String tipo = f.getTipoFicha() != null ? f.getTipoFicha().trim() : "";
            if (tipo.equalsIgnoreCase(tipoFichaFilter)) {
                out.add(f);
            }
        }
        return out;
    }

    private List<Ficha> filterFichasForDialog(String tipoFichaFilter, String areaFilter) {
        List<Ficha> out = new ArrayList<>();
        if (fichas == null) return out;
        for (Ficha f : fichas) {
            if (f == null) continue;
            String tipo = f.getTipoFicha() != null ? f.getTipoFicha().trim() : "";
            String area = f.getArea() != null ? f.getArea().trim() : "";
            if (tipo.equalsIgnoreCase(tipoFichaFilter)) {
                if (tipoFichaFilter.equalsIgnoreCase("Docente")) {
                    out.add(f); // area ignored
                } else {
                    if (areaFilter == null || areaFilter.isEmpty() || area.equalsIgnoreCase(areaFilter)) {
                        out.add(f);
                    }
                }
            }
        }
        return out;
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            recaptureLocationAfterPermission();
        }
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

    @Override
    protected void onStart() {
        super.onStart();

        providersChangeReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (android.location.LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)
                        || android.location.LocationManager.MODE_CHANGED_ACTION.equals(action)) {
                    boolean gpsEnabled = isLocationProviderEnabled();
                    if (gpsEnabled != lastGpsEnabledState) {
                        lastGpsEnabledState = gpsEnabled;
                        if (!gpsEnabled) {
                            showGpsProblemToast("Servicios de ubicación desactivados. Active GPS en Ajustes.");
                        } else {
                            runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "GPS activado", Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            }
        };

        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        registerReceiver(providersChangeReceiver, filter);

        lastGpsEnabledState = isLocationProviderEnabled();
        lastPermissionState = hasLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean currentPermission = hasLocationPermission();
        boolean currentGps = isLocationProviderEnabled();

        if (currentPermission != lastPermissionState) {
            if (currentPermission) {
                recaptureLocationAfterPermission();
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Permiso de ubicación revocado. Conceda en Ajustes.", Toast.LENGTH_LONG).show());
            }
            lastPermissionState = currentPermission;
        }

        if (currentGps != lastGpsEnabledState) {
            lastGpsEnabledState = currentGps;
            if (!currentGps) {
                showGpsProblemToast("Servicios de ubicación desactivados. Active GPS en Ajustes.");
            } else {
                runOnUiThread(() -> Toast.makeText(this, "GPS activado", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void stopLocationUploadService() {
        if (!LocationUploadService.isRunning) return;
        Intent i = new Intent(this, LocationUploadService.class);
        stopService(i);
        //serviceStarted = false;
    }

    @Override
    protected void onStop() {
        try {
            if (providersChangeReceiver != null) {
                unregisterReceiver(providersChangeReceiver);
                providersChangeReceiver = null;
            }
        } catch (Exception ignored) {}
        super.onStop();
    }

    private void generateReportBackground(Map<String,String> meta, Ficha ficha, WorkLocation colegio) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                final String fichaIdFilter = ficha != null ? ficha.getId() : safe(meta.get("idficha"));
                final String mesFilter = safe(meta.get("mes"));
                final String anoFilter = safe(meta.get("ano"));
                final String colegioFilter = colegio != null ? colegio.getName() : safe(meta.get("colegio"));
                final String especialistaFilter = safe(meta.get("generado_por").isEmpty() ? meta.get("especialista_nombre") : meta.get("generado_por"));
                final String dniFiltro = docIdentidad != null ? docIdentidad : "";
                final boolean isBoss = "42503725".equals(dniFiltro);

                Map<String,String> params = new HashMap<>();
                params.put("idficha", fichaIdFilter == null ? "" : fichaIdFilter);
                params.put("mes", mesFilter);
                params.put("ano", anoFilter);
                params.put("colegio", colegioFilter);
                params.put("especialista_nombre", especialistaFilter);
                params.put("docidentidad", dniFiltro);
                params.put("isBoss", String.valueOf(isBoss));

                // 1) fetch submissions
                org.json.JSONArray arr = null;
                try { arr = fetchJsonArrayWithFallback(URLPostHelper.Fichas.LIST_SUBMISSIONS, params); } catch (Throwable t) { Log.d("FETCH_ERROR","LIST_SUBMISSIONS fetch error", t); }

                List<Map<String,String>> submissions = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        Map<String,String> row = new LinkedHashMap<>();
                        Iterator<String> keys = o.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            row.put(k, o.optString(k, ""));
                        }
                        submissions.add(row);
                    }
                }

                if (submissions.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "No se obtuvo respuesta del servidor (LIST_SUBMISSIONS)", Toast.LENGTH_LONG).show());
                    return;
                }

                // 2) fetch questions catalog for ficha -> map idpregunta -> texto (and extras)
                final Map<String, org.json.JSONObject> catalogById = new LinkedHashMap<>();
                if (fichaIdFilter != null && !fichaIdFilter.isEmpty()) {
                    try {
                        Map<String,String> qParams = new HashMap<>();
                        qParams.put("idficha", fichaIdFilter);

                        org.json.JSONArray qArr = null;
                        try {
                            qArr = postJsonArrayRaw(URLPostHelper.Fichas.LIST_QUESTIONS, qParams);
                            if (qArr == null || qArr.length() == 0) qArr = null;
                        } catch (Throwable t) {
                            qArr = null;
                        }
                        if (qArr == null) {
                            try {
                                qArr = fetchJsonArrayWithFallback(URLPostHelper.Fichas.LIST_QUESTIONS, qParams);
                            } catch (Throwable t) {
                                qArr = null;
                            }
                        }
                        if (qArr != null) {
                            for (int i = 0; i < qArr.length(); i++) {
                                org.json.JSONObject qo = qArr.optJSONObject(i);
                                if (qo == null) continue;
                                String idp = safe(qo.optString("idpregunta", qo.optString("id", qo.optString("idPregunta", ""))));
                                if (idp == null) idp = "";
                                catalogById.put(idp, qo);
                            }
                        } else {
                            Log.d("FETCH_ERROR","LIST_QUESTIONS returned null for idficha=" + fichaIdFilter);
                        }
                    } catch (Throwable t) {
                        Log.d("FETCH_ERROR","Error fetching LIST_QUESTIONS for idficha=" + fichaIdFilter, t);
                    }
                } else {
                    Log.d("FETCH_ERROR","No fichaIdFilter available for LIST_QUESTIONS");
                }

                // 3) filter submissions locally (same previous rules)
                List<Map<String,String>> filtered = new ArrayList<>();
                for (Map<String,String> s : submissions) {
                    if (s == null) continue;
                    if (fichaIdFilter != null && !fichaIdFilter.isEmpty()) {
                        String v = safe(s.get("idficha"));
                        if (!fichaIdFilter.equals(String.valueOf(v))) continue;
                    }
                    if (colegioFilter != null && !colegioFilter.isEmpty()) {
                        String v = safe(s.get("colegio"));
                        if (!v.equalsIgnoreCase(colegioFilter)) continue;
                    }
                    if (mesFilter != null && !mesFilter.isEmpty() && anoFilter != null && !anoFilter.isEmpty()) {
                        String fecha = safe(s.get("fecha_envio"));
                        if (fecha.length() >= 7) {
                            String monthNum = getMonthNumber(mesFilter);
                            String prefix = anoFilter + "-" + monthNum;
                            if (!fecha.startsWith(prefix)) continue;
                        } else continue;
                    }
                    if (especialistaFilter != null && !especialistaFilter.isEmpty() && !isBoss) {
                        String v = safe(s.get("especialista_nombre"));
                        if (!v.equalsIgnoreCase(especialistaFilter)) continue;
                    }
                    if (!isBoss) {
                        String vdoc = safe(s.get("docidentidad"));
                        String vesp = safe(s.get("especialista_dni"));
                        if (!dniFiltro.equals(vdoc) && !dniFiltro.equals(vesp)) continue;
                    }
                    filtered.add(s);
                }

                if (filtered.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "No hay encabezados que cumplan los filtros aplicados", Toast.LENGTH_LONG).show());
                    return;
                }

                // 4) build blocks: use catalogById exclusively for Pregunta.textoPregunta
                List<Map<String,Object>> blocks = new ArrayList<>();
                for (Map<String,String> sub : filtered) {
                    try {
                        Map<String,String> submissionMeta = new LinkedHashMap<>();
                        if (meta != null) submissionMeta.putAll(meta);
                        submissionMeta.putAll(sub);
                        if (isBoss) {
                            String especialistaNombreEnEnc = safe(submissionMeta.get("especialista_nombre"));
                            if (!especialistaNombreEnEnc.isEmpty()) submissionMeta.put("especialista_realizo", especialistaNombreEnEnc);
                        }
                        String encabezadoId = !safe(submissionMeta.get("encabezado_id")).isEmpty() ? submissionMeta.get("encabezado_id") : safe(submissionMeta.get("id"));

                        // fetch respuestas for this encabezado
                        Map<String,String> respuestasMap = new LinkedHashMap<>();
                        if (encabezadoId != null && !encabezadoId.isEmpty()) {
                            String url = String.format(URLPostHelper.Fichas.LIST_RESPONSES, java.net.URLEncoder.encode(encabezadoId, "UTF-8"));
                            org.json.JSONArray rarr = null;
                            try { rarr = fetchJsonArrayWithFallback(url, null); } catch (Throwable t) { Log.d("FETCH_ERROR","LIST_RESPONSES fetch error for " + encabezadoId, t); }
                            if (rarr != null) {
                                for (int j = 0; j < rarr.length(); j++) {
                                    org.json.JSONObject ro = rarr.optJSONObject(j);
                                    if (ro == null) continue;

                                    // Prefer server-provided idpregunta (the real question id).
                                    // Fallback to response id if idpregunta missing.
                                    String idpreg = safe(ro.optString("idpregunta", ""));
                                    String respId = String.valueOf(ro.optInt("id", 0));
                                    String rid;
                                    if (!idpreg.isEmpty()) {
                                        rid = idpreg; // use question id as key part
                                    } else if (!"0".equals(respId) && !respId.isEmpty()) {
                                        rid = respId;
                                    } else {
                                        rid = String.valueOf(System.currentTimeMillis()) + "_" + j;
                                    }

                                    String rawPregunta = safe(ro.optString("pregunta_texto"));
                                    String rawText = safe(ro.optString("respuesta_texto"));
                                    String rawComment = safe(ro.optString("comentario"));
                                    String rawFoto = safe(ro.optString("foto"));

                                    if ("0".equals(rawText) || "null".equalsIgnoreCase(rawText)) rawText = "";
                                    if ("0".equals(rawComment) || "null".equalsIgnoreCase(rawComment)) rawComment = "";
                                    if ("0".equals(rawFoto) || "null".equalsIgnoreCase(rawFoto)) rawFoto = "";
                                    if ("0".equals(rawPregunta) || "null".equalsIgnoreCase(rawPregunta)) rawPregunta = "";

                                    // Store both by question id (preferred) and by response unique id (if available),
                                    // and include which encabezado the response belongs to so downstream consumers can trace it.
                                    respuestasMap.put("q_" + rid + "_pregunta", rawPregunta);
                                    respuestasMap.put("q_" + rid + "_text", rawText);
                                    respuestasMap.put("q_" + rid + "_comentario", rawComment);
                                    respuestasMap.put("q_" + rid + "_foto", rawFoto);

                                    // Keep original response id (if there is one) under a stable key so you can trace server row
                                    if (!respId.equals("0") && !respId.isEmpty()) {
                                        respuestasMap.put("respid_" + respId + "_encabezado", encabezadoId);
                                        respuestasMap.put("respid_" + respId + "_pregunta_id", idpreg);
                                        respuestasMap.put("respid_" + respId + "_text", rawText);
                                    }

                                    // Also record mapping from question id -> encabezado for quick lookup when idpregunta present
                                    if (!idpreg.isEmpty()) {
                                        respuestasMap.put("q_" + idpreg + "_encabezado", encabezadoId);
                                    } else {
                                        // if idpreg missing, keep a marker that this response (by respId) belongs to encabezado
                                        respuestasMap.put("q_" + rid + "_encabezado", encabezadoId);
                                    }
                                }
                            }
                        } else {
                            Log.d("FETCH_ERROR","submission without encabezadoId: " + submissionMeta);
                        }

                        // Build Pregunta list from catalogById (ONLY)
                        List<com.ugelcorongo.edugestin360.domain.models.Pregunta> preguntasList = new ArrayList<>();
                        for (Map.Entry<String, org.json.JSONObject> ent : catalogById.entrySet()) {
                            String qid = ent.getKey();
                            org.json.JSONObject qo = ent.getValue();
                            if (qo == null) continue;

                            String qtexto = safe(qo.optString("texto", qo.optString("texto_pregunta", qo.optString("pregunta_texto", ""))));
                            String seccion = safe(qo.optString("seccion", qo.optString("seccion_nombre", "")));
                            String tipoPregunta = safe(qo.optString("tipo", qo.optString("tipoPregunta", "")));
                            boolean requiereComentario = qo.has("requiereComentario")
                                    ? qo.optInt("requiereComentario", qo.optInt("requiere_comentario", 0)) == 1
                                    : qo.optInt("requiere_comentario", 0) == 1;
                            boolean requiereFoto = qo.has("requiereFoto")
                                    ? qo.optInt("requiereFoto", qo.optInt("requiere_foto", 0)) == 1
                                    : qo.optInt("requiere_foto", 0) == 1;

                            com.ugelcorongo.edugestin360.domain.models.Pregunta p = new com.ugelcorongo.edugestin360.domain.models.Pregunta();
                            p.setIdFicha(fichaIdFilter);
                            p.setTipoFicha(ficha != null ? ficha.getTipoFicha() : safe(meta.get("tipoFicha")));
                            p.setIdPregunta(qid);
                            p.setTextoPregunta(qtexto == null ? "" : qtexto);
                            p.setSeccion(seccion);
                            p.setTipoPregunta(tipoPregunta);
                            p.setRequiereComentario(requiereComentario);
                            p.setRequiereFoto(requiereFoto);
                            p.setOpciones(new ArrayList<>());

                            preguntasList.add(p);
                        }

                        // If catalog empty, optional fallback: build from respuestasMap (not preferred)
                        if (catalogById.isEmpty()) {
                            Set<String> seen = new LinkedHashSet<>();
                            for (String rk : respuestasMap.keySet()) {
                                if (!rk.endsWith("_pregunta")) continue;
                                int idx = rk.indexOf("_pregunta");
                                if (idx <= 2) continue;
                                String qid = rk.substring(2, idx);
                                if (seen.contains(qid)) continue;
                                seen.add(qid);
                                String qtextFromResp = respuestasMap.get(rk);
                                com.ugelcorongo.edugestin360.domain.models.Pregunta p = new com.ugelcorongo.edugestin360.domain.models.Pregunta();
                                p.setIdFicha(fichaIdFilter);
                                p.setIdPregunta(qid);
                                p.setTextoPregunta(qtextFromResp == null ? "" : qtextFromResp);
                                p.setOpciones(new ArrayList<>());
                                preguntasList.add(p);
                            }
                        }

                        Map<String,Object> block = new LinkedHashMap<>();
                        block.put("meta", submissionMeta);
                        block.put("preguntas", preguntasList);
                        block.put("respuestas", respuestasMap);
                        block.put("chartBitmap", null);
                        blocks.add(block);
                    } catch (Throwable t) {
                        Log.d("FETCH_ERROR","exception processing submission", t);
                    }
                }

                // 5) generar PDF
                File out = null;
                try {
                    out = com.ugelcorongo.edugestin360.managers.reports.ReportGenerator.generateMultiFichaPdfReport(EspecialistaActivity.this, meta, blocks);
                } catch (Throwable t) {
                    Log.d("FETCH_ERROR","error generating pdf", t);
                }

                if (out != null) {
                    lastGeneratedReportPath = out.getAbsolutePath();
                    if (androidx.core.app.NotificationManagerCompat.from(EspecialistaActivity.this).areNotificationsEnabled()) {
                        ensureNotificationChannel();
                        notifyReportGenerated(out);
                    }
                    File f = out;
                    runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "Reporte generado: " + f.getName(), Toast.LENGTH_LONG).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "No se pudo generar ningún PDF", Toast.LENGTH_LONG).show());
                }

            } catch (final Exception ex) {
                Log.d("FETCH_ERROR","fatal exception generateReportBackground", ex);
                runOnUiThread(() -> Toast.makeText(EspecialistaActivity.this, "Error generando reporte", Toast.LENGTH_LONG).show());
            }
        });
    }

    private org.json.JSONArray fetchJsonArrayWithFallback(String urlString, Map<String,String> postParams) throws Exception {
        if (postParams != null && !postParams.isEmpty()) {
            try {
                org.json.JSONArray arr = postJsonArrayRaw(urlString, postParams);
                if (arr != null && arr.length() > 0) return arr;
            } catch (Throwable ignored) {}
        }
        try {
            org.json.JSONArray arrGet = getJsonArrayRaw(urlString);
            if (arrGet != null && arrGet.length() > 0) return arrGet;
        } catch (Throwable ignored) {}
        // última oportunidad: GET y buscar array en body or in keys data/rows/result
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(urlString);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
            String body = s.hasNext() ? s.next() : "";
            is.close();
            if (body == null || body.trim().isEmpty()) return null;
            try { return new org.json.JSONArray(body); } catch (org.json.JSONException ex) {
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(body);
                    if (obj.has("data") && obj.get("data") instanceof org.json.JSONArray) return obj.getJSONArray("data");
                    if (obj.has("rows") && obj.get("rows") instanceof org.json.JSONArray) return obj.getJSONArray("rows");
                    if (obj.has("result") && obj.get("result") instanceof org.json.JSONArray) return obj.getJSONArray("result");
                } catch (org.json.JSONException ex2) { return null; }
            }
        } finally { if (conn != null) conn.disconnect(); }
        return null;
    }

    private JSONArray postJsonArrayRaw(String urlString, Map<String,String> params) throws Exception {
        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            StringBuilder body = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String,String> e : params.entrySet()) {
                if (!first) body.append("&");
                first = false;
                body.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8"));
                body.append("=");
                body.append(java.net.URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
            }
            byte[] out = body.toString().getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(out.length);
            java.io.OutputStream os = conn.getOutputStream();
            os.write(out);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
            String resp = s.hasNext() ? s.next() : "";
            is.close();
            if (resp == null || resp.trim().isEmpty()) return null;
            return new JSONArray(resp);
        } finally { conn.disconnect(); }
    }

    private JSONArray getJsonArrayRaw(String urlString) throws Exception {
        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
            String resp = s.hasNext() ? s.next() : "";
            is.close();
            if (resp == null || resp.trim().isEmpty()) return null;
            return new org.json.JSONArray(resp);
        } finally {
            conn.disconnect();
        }
    }

    private String getMonthNumber(String mesNombre) {
        if (mesNombre == null) return "01";
        String[] meses = new java.text.DateFormatSymbols(Locale.getDefault()).getMonths();
        for (int i = 0; i < meses.length; i++) {
            if (mesNombre.equalsIgnoreCase(meses[i])) return String.format("%02d", i + 1);
        }
        // intentar parse int
        try {
            int m = Integer.parseInt(mesNombre);
            if (m >= 1 && m <= 12) return String.format("%02d", m);
        } catch (Exception ignored) {}
        return "01";
    }

    private void ensureNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Reportes";
            String desc = "Notificaciones de reportes generados";
            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;
            android.app.NotificationChannel chan = new android.app.NotificationChannel(REPORTS_CHANNEL_ID, name, importance);
            chan.setDescription(desc);
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(chan);
        }
    }

    private void notifyReportGenerated(File file) {
        ensureNotificationChannel();

        // 1) obtener uri seguro
        android.net.Uri uri;
        try {
            uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        } catch (IllegalArgumentException e) {
            uri = null;
        }

        // 2) determinar mime por extensión
        String name = file.getName().toLowerCase(Locale.ROOT);
        String mime = "application/octet-stream";
        if (name.endsWith(".pdf")) mime = "application/pdf";
        else if (name.endsWith(".xls") || name.endsWith(".xlsx")) mime = "application/vnd.ms-excel";
        else if (name.endsWith(".csv")) mime = "text/csv";
        else if (name.endsWith(".xml")) mime = "application/xml";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (uri != null) {
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ? android.app.PendingIntent.FLAG_IMMUTABLE
                : android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(this, 0, intent, flags);

        androidx.core.app.NotificationCompat.Builder nb =
                new androidx.core.app.NotificationCompat.Builder(this, REPORTS_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Reporte generado")
                        .setContentText(file.getName())
                        .setAutoCancel(true)
                        .setContentIntent(pi)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT);

        // permiso POST_NOTIFICATIONS (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        androidx.core.app.NotificationManagerCompat.from(this).notify(REPORT_NOTIFICATION_ID, nb.build());
    }

    private void showPermissionApprovalDialog(String role, Runnable onAccepted) {
        // role: "Especialista" o "DocenteDirector"
        String url = URLPostHelper.Terminos.Info;
        String title = "Permisos necesarios";
        String message;
        String prefKey;

        if ("Especialista".equalsIgnoreCase(role)) {
            message = "Autorizo que la aplicación mantenga acceso a mi ubicación mientras la aplicación esté activa, con el fin de facilitar funciones continuas como registro de asistencia. He leído y acepto los términos y condiciones.";
            prefKey = KEY_CONSENT_ESPECIALISTA;
        } else {
            // Docente o Director
            message = "Entiendo y acepto que la aplicación solicitará acceso a mi ubicación únicamente mientras realizo las funciones relacionadas (por ejemplo: registro de asistencia o monitoreo). He leído y acepto los términos y condiciones.";
            prefKey = KEY_CONSENT_DOCENTE;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message + "\n\nPara más información, consulte los términos y condiciones.")
                .setPositiveButton("Aceptar", (d, w) -> {
                    setConsentForRole(prefKey, true);
                    onAccepted.run();
                })
                .setNeutralButton("Términos y condiciones del aplicativo", (d, w) -> {
                    // Abrir URL
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                })
                .setNegativeButton("Cancelar", (d, w) -> {
                    d.dismiss();
                    showPermissionRequiredDialog();
                });

        AlertDialog dlg = b.create();
        dlg.show();
    }

    private void showPermissionRequiredDialog() {
        String url = URLPostHelper.Terminos.Info;
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Permisos requeridos")
                .setMessage("Es necesario los permisos para el uso de la aplicación.")
                .setPositiveButton("Volver a los términos", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                })
                .setNegativeButton("Cerrar la aplicación", (d, w) -> {
                    // Redirigir al login
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                });
        b.setCancelable(false);
        b.show();
    }

    private void ensureConsentAndRequestLocation(String role) {
        String prefKey = "Docente".equalsIgnoreCase(role) || "Director".equalsIgnoreCase(role)
                ? KEY_CONSENT_DOCENTE : KEY_CONSENT_ESPECIALISTA;

        if (hasConsentForRole(prefKey)) {
            requestLocationPermission();
        } else {
            showPermissionApprovalDialog(role, () -> {
                requestLocationPermission();
            });
        }
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
            showPermissionApprovalDialog("Especialista", () -> {
                requestLocationPermission();
            });
            //showPermissionsModal("La aplicación necesita estos permisos para funcionar:\n\n- Ubicación GPS\n- Cámara\n\nAdemás active los servicios de ubicación del dispositivo.", true);
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
                    showPermissionApprovalDialog("Especialista", () -> {
                        requestLocationPermission();
                    });
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
                showPermissionApprovalDialog("Especialista", () -> {
                    requestLocationPermission();
                });
                showPermissionsModal("La aplicación necesita estos permisos para funcionar:\n\n- Ubicación GPS\n- Cámara\n\nConceda permisos en Ajustes.", false);
            } else {
                // re-request politely
                //showPermissionsModal("La aplicación necesita permisos de Ubicación y Cámara para continuar.", true);
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

    private void showGpsProblemToast(String reason) {
        runOnUiThread(() -> {
            Toast.makeText(
                    EspecialistaActivity.this,
                    "Problema GPS: " + reason,
                    Toast.LENGTH_LONG
            ).show();
        });
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

            showPermissionApprovalDialog("Especialista", () -> {
                requestLocationPermission();
            });
            showPermissionsModal("La aplicación necesita estos permisos para funcionar:\n\n- Ubicación GPS\n- Cámara\n\nDebe activarlos para continuar", true);
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private boolean isLocationProviderEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return false;
        boolean gps = false;
        boolean network = false;
        try { gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
        try { network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
        return gps || network;
    }

    @Override
    protected void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        stopLocationUploadService(); // detener servicio si la Activity se destruye
        super.onDestroy();
    }
}