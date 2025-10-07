package com.ugelcorongo.edugestin360.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Docente;
import com.ugelcorongo.edugestin360.domain.models.RegistroAsistencia;
import com.ugelcorongo.edugestin360.managers.upload.ImageUploadManager;
import com.ugelcorongo.edugestin360.managers.upload.PendingUploadProcessor;
import com.ugelcorongo.edugestin360.ui.viewmodel.DirectorViewModel;
import com.ugelcorongo.edugestin360.utils.DataLoader;
import com.ugelcorongo.edugestin360.utils.LocationProvider;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DirectorActivity extends AppCompatActivity {
    // UI
    private View btcAsistenciasDirector;
    private View btcDocentesDirector;
    private TextView lbl_director;

    // ViewModel
    private DirectorViewModel viewModel;

    // Datos leídos de archivos
    private List<Docente>       docentes      = new ArrayList<>();
    private List<RegistroAsistencia> registros      = new ArrayList<>();

    // Colegio detectado
    private ColegioInfo colegioInfo;

    // Permiso ubicación
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private static final double MAX_DISTANCE_M = 50.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_director);


        // 1) Leer el DNI del director del Intent
        String directorDocIdent = getIntent()
                .getStringExtra(LoginActivity.EXTRA_DOCIDENTIDAD);
        lbl_director = findViewById(R.id.lbl_director);

        // 2) Cargar ColegioInfo **antes** de usarlo
        colegioInfo = DataLoader.loadColegioInfo(
                this,
                directorDocIdent
        );

        // 3) Verificar null y abortar si no se obtuvo
        if (colegioInfo == null) {
            Toast.makeText(
                    this,
                    "No se encontró información del colegio para el director",
                    Toast.LENGTH_LONG
            ).show();
            finish();   // o navega a otra pantalla
            return;
        }
        lbl_director.setText(colegioInfo.getNombreDirector());


        // 4) Ahora puedes usar colegioInfo.getNombre()
        docentes = DataLoader.loadDocentes(
                this,
                colegioInfo.getNombre()
        );

        registros = DataLoader.loadRegistros(
                this,
                colegioInfo.getNombre(),
                getDiaSemanaHoy()
        );

        viewModel = new ViewModelProvider(this)
                .get(DirectorViewModel.class);

        btcAsistenciasDirector = findViewById(R.id.btc_asistencias_director);
        btcDocentesDirector = findViewById(R.id.btc_docentes_director);

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
                }
        );

        btcAsistenciasDirector.setOnClickListener(this::asistencias);

        // Botón Gestión Docentes
        btcDocentesDirector.setOnClickListener(this::gestionDocentes);
        new Thread(
                () -> new PendingUploadProcessor(this)
                        .processAll()
        ).start();
    }

    public void gestionDocentes(View view) {
        Intent i = new Intent(this, DocenteListActivity.class);
        // Pasamos ColegioInfo completo como Parcelable
        i.putExtra("EXTRA_COLEGIO_INFO", colegioInfo);
        startActivity(i);
    }

    public void asistencias(View view) {
        // 1) Verificar permiso de ubicación
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
            return;
        }

        // 2) Pedir ubicación puntual
        LocationProvider.requestSingle(this, loc -> {
            // 2.1) Calcular distancia al colegio
            float[] result = new float[1];
            Location.distanceBetween(
                    loc.getLatitude(), loc.getLongitude(),
                    colegioInfo.getLat(), colegioInfo.getLon(),
                    result
            );
            if (result[0] > MAX_DISTANCE_M) {
                Toast.makeText(
                        this,
                        "Debes estar dentro de 50 m del colegio",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            // 2.2) Día y minutos actuales
            Calendar now = Calendar.getInstance();
            int today    = getDiaSemanaHoy();
            int nowMins  = now.get(Calendar.HOUR_OF_DAY) * 60
                    + now.get(Calendar.MINUTE);

            // 2.3) Construir lista de nombres y flags de habilitado
            int n = docentes.size();
            String[] items    = new String[n];
            boolean[] enabled = new boolean[n];
            for (int i = 0; i < n; i++) {
                Docente d = docentes.get(i);
                items[i] = d.getNombre();

                // habilitado si coincide el día y estamos ±30 min autour de d.getHoraEntrada()
                if (d.getDiaSemana() == today) {
                    int ent = horaATotalMinutos(d.getHoraEntrada());
                    enabled[i] = nowMins >= (ent - 30);
                } else {
                    enabled[i] = false;
                }
            }

            // 2.4) Mostrar diálogo
            AlertDialog.Builder b = new AlertDialog.Builder(this)
                    .setTitle("Registrar asistencia")
                    .setNegativeButton("Cancelar", (dlg, w) -> dlg.dismiss())
                    .setItems(items, (dlg, which) -> {
                        if (!enabled[which]) {
                            Toast.makeText(
                                    this,
                                    "Fuera de horario permitido",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        // 2.5) Registrar
                        registrarAsistencia(
                                docentes.get(which),
                                now.getTimeInMillis()
                        );
                        dlg.dismiss();
                    });

            AlertDialog dlg = b.create();
            dlg.setOnShowListener(d -> {
                ListView lv = dlg.getListView();
                for (int i = 0; i < n; i++) {
                    lv.getChildAt(i).setEnabled(enabled[i]);
                }
            });
            dlg.show();
        });
    }

    private void registrarAsistencia(Docente d, long timestampMs) {
        int today   = getDiaSemanaHoy();
        int nowMins = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                + Calendar.getInstance().get(Calendar.MINUTE);
        int entMins = horaATotalMinutos(d.getHoraEntrada());
        int salMins = horaATotalMinutos(d.getHoraSalida());

        // 1) Determinar tipo de registro
        boolean yaEntrada = registros.stream()
                .anyMatch(r -> r.getDocIdentidad().equals(d.getDocIdentidad())
                        && r.getTipoRegistro().equals("Entrada"));
        boolean yaSalida  = registros.stream()
                .anyMatch(r -> r.getDocIdentidad().equals(d.getDocIdentidad())
                        && r.getTipoRegistro().equals("Salida"));

        String tipo;
        if (!yaEntrada) {
            tipo = "Entrada";
            // validar ventana de llegada
            if (d.getDiaSemana() != today
                    || nowMins < entMins - 30) {
                Toast.makeText(this,
                        "Fuera de horario de llegada",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        } else if (!yaSalida) {
            tipo = "Salida";
            // validar ventana de salida (opcional ±30 min)
            if (d.getDiaSemana() != today
                    || nowMins > salMins + 30) {
                Toast.makeText(this,
                        "Fuera de horario de salida",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        } else {
            Toast.makeText(this,
                    "Asistencia ya completa para " + d.getNombre(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // 2) Calcular tardanza solo en Entrada
        int tardanza;
        if ("Entrada".equals(tipo) && nowMins > entMins) {
            tardanza = nowMins - entMins;
        } else {
            tardanza  = 0;
        }

        // 3) Construir parámetros
        Map<String,String> meta = new HashMap<>();
        meta.put("id_colegio",   colegioInfo.getIdcolegio());
        meta.put("colegio",      colegioInfo.getNombre());
        meta.put("docidentidad", d.getDocIdentidad());
        meta.put("directorId",   colegioInfo.getDirector());
        meta.put("tipo_registro", tipo);
        meta.put("tardanza",      String.valueOf(tardanza));
        String horaRegistro = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(new Date(timestampMs));
        meta.put("hora_registro", horaRegistro);
        meta.put("latitud",      String.valueOf(colegioInfo.getLat()));
        meta.put("longitud",     String.valueOf(colegioInfo.getLon()));

        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        meta.put("device_id",       androidId);
        meta.put("device_model",    Build.MANUFACTURER + " " + Build.MODEL);
        meta.put("android_version", Build.VERSION.RELEASE);

        // 4) Envío o encolado
        if (NetworkUtil.isOnline(this)) {
            new ImageUploadManager(this)
                    .postForm(
                            URLPostHelper.Director.Asistencia,
                            meta,
                            resp -> {
                                Toast.makeText(this,
                                        tipo + " registrada", Toast.LENGTH_SHORT
                                ).show();
                                registros.add(new RegistroAsistencia(
                                        d.getDocIdentidad(),
                                        tipo,
                                        horaRegistro,
                                        tardanza
                                ));
                            },
                            error -> {
                                viewModel.enqueueUploadTask(
                                        "ASIS_" + tipo + "_" + d.getDocIdentidad(),
                                        null, meta
                                );
                                Toast.makeText(this,
                                        "Error envío; se encoló", Toast.LENGTH_SHORT
                                ).show();
                            }
                    );
        } else {
            viewModel.enqueueUploadTask(
                    "ASIS_" + tipo + "_" + d.getDocIdentidad(),
                    null, meta
            );
            Toast.makeText(this,
                    "Sin red: asistencia encolada", Toast.LENGTH_SHORT
            ).show();
        }

        // 5) Agregar al cache local para evitar duplicados en la sesión
        registros.add(new RegistroAsistencia(
                d.getDocIdentidad(), tipo, horaRegistro, tardanza
        ));
    }

    private int horaATotalMinutos(String hhmm) {
        String[] partes = hhmm.split(":");
        int h = Integer.parseInt(partes[0]);
        int m = Integer.parseInt(partes[1]);
        return h * 60 + m;
    }

    private int getDiaSemanaHoy() {
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (dow == Calendar.SUNDAY) ? 7 : (dow - 1);
    }
}