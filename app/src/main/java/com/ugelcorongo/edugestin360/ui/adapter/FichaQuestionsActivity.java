package com.ugelcorongo.edugestin360.ui.adapter;

import com.ugelcorongo.edugestin360.R;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Especialista;
import com.ugelcorongo.edugestin360.domain.models.OpcionRespuesta;
import com.ugelcorongo.edugestin360.domain.models.Pregunta;
import com.ugelcorongo.edugestin360.managers.VolleyMultipartRequest;
import com.ugelcorongo.edugestin360.managers.upload.FichaUploadManager;
import com.ugelcorongo.edugestin360.remote.ApiService;
import com.ugelcorongo.edugestin360.repository.DataRepository;
import com.ugelcorongo.edugestin360.ui.EspecialistaActivity;
import com.ugelcorongo.edugestin360.ui.viewmodel.EspecialistaViewModel;
import com.ugelcorongo.edugestin360.utils.LocationProvider;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.UriUtils;

import androidx.core.content.ContextCompat;

public class FichaQuestionsActivity extends AppCompatActivity implements SectionFragment.OnHeaderDocenteChange{
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private EspecialistaViewModel vm;
    private List<String> sectionTitles;
    private List<Pregunta> allPreguntas;
    private Map<String, List<Pregunta>> preguntasPorSeccion;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private String idFicha;
    private String nrovisita, tipoFicha, docente_enficha;
    private ColegioInfo info;
    private Especialista insta_especialista;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficha_questions);
        vm = new ViewModelProvider(this).get(EspecialistaViewModel.class);

        tabLayout = findViewById(R.id.tabSections);
        viewPager = findViewById(R.id.vpSections);

        info = getIntent().getParcelableExtra("EXTRA_COLEGIO_INFO");
        insta_especialista = getIntent().getParcelableExtra("EXTRA_ESPECIALISTA_INFO");

        tipoFicha = getIntent().getStringExtra("tipoFicha");
        nrovisita = getIntent().getStringExtra("nrovisita");
        docente_enficha   = getIntent().getStringExtra("EXTRA_DOCENTE_NAME");

        Intent receivedIntent = getIntent();
        idFicha = receivedIntent.getStringExtra("idFicha");

        // 3. Pedir permiso de cámara al iniciar
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ android.Manifest.permission.CAMERA },
                    REQUEST_CAMERA_PERMISSION
            );
        }
        loadPreguntasYRespuestas();
    }

    private void loadPreguntasYRespuestas() {
        DataRepository repo = DataRepository.getInstance(this);
        repo.fetchPreguntas(idFicha, new ApiService.ApiCallback<List<Pregunta>>() {
            @Override
            public void onSuccess(List<Pregunta> preguntas) {
                // Si no hay preguntas, termina
                if (preguntas.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(FichaQuestionsActivity.this,
                                    "No hay preguntas disponibles",
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                // 5. Por cada una, obtenemos sus respuestas
                AtomicInteger pending = new AtomicInteger(preguntas.size());
                for (Pregunta p : preguntas) {
                    repo.fetchRespuestas(p.getIdPregunta(),
                            new ApiService.ApiCallback<List<OpcionRespuesta>>() {
                                @Override
                                public void onSuccess(List<OpcionRespuesta> opciones) {
                                    if (preguntas.isEmpty()) { return; }
                                    allPreguntas = preguntas;

                                    p.setOpciones(opciones);
                                    if (pending.decrementAndGet() == 0) {
                                        runOnUiThread(() -> setupSections(preguntas));
                                    }
                                }
                                @Override
                                public void onError(Exception e) {
                                    // aunque falle, contamos como recibido
                                    if (pending.decrementAndGet() == 0) {
                                        runOnUiThread(() -> setupSections(preguntas));
                                    }
                                }
                            });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(FichaQuestionsActivity.this,
                                "Error cargando preguntas",
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void setupSections(List<Pregunta> lista) {
        // 1) Agrupar preguntas crudas por sección
        Map<String, List<Pregunta>> porSeccion = new LinkedHashMap<>();
        for (Pregunta p : lista) {
            porSeccion
                    .computeIfAbsent(p.getSeccion(), k -> new ArrayList<>())
                    .add(p);
        }

        // 2) Para cada sección, reagrupar por "clave de enunciado"
        Map<String, List<Pregunta>> mapProcesado = new LinkedHashMap<>();

        // Rellena campos para el envío:
        preguntasPorSeccion = mapProcesado;
        sectionTitles      = new ArrayList<>(mapProcesado.keySet());

        for (Map.Entry<String, List<Pregunta>> entry : porSeccion.entrySet()) {
            String seccion = entry.getKey();
            List<Pregunta> brutales = entry.getValue();

            // 2.1) Agrupar por base del texto antes de ":" o texto completo si no hay ":"
            Map<String, List<Pregunta>> grupos = new LinkedHashMap<>();
            for (Pregunta p : brutales) {
                String txt = p.getTextoPregunta();
                String base = txt.contains(":")
                        ? txt.substring(0, txt.indexOf(":")).trim()
                        : txt;
                grupos.computeIfAbsent(base, k -> new ArrayList<>()).add(p);
            }

            // 2.2) Reconstruir la lista final para esta sección
            List<Pregunta> procesadas = new ArrayList<>();
            for (Map.Entry<String, List<Pregunta>> g : grupos.entrySet()) {
                List<Pregunta> sub = g.getValue();
                if (sub.size() > 1) {
                    // Caso "por enunciado": múltiples sub‐items → creamos pregunta combinada
                    Pregunta combinada = new Pregunta();
                    combinada.setIdPregunta(seccion + "_grp_" + g.getKey());
                    combinada.setSeccion(seccion);
                    combinada.setTipoPregunta("SINGLE_CHOICE");
                    combinada.setTextoPregunta(g.getKey()); // p.ej. "NIVEL"

                    // Cada sub‐pregunta pasa a OpcionRespuesta
                    List<OpcionRespuesta> opts = new ArrayList<>();
                    for (Pregunta child : sub) {
                        OpcionRespuesta o = new OpcionRespuesta();
                        o.setIdRespuesta(child.getIdPregunta());
                        o.setDescripcion(child.getTextoPregunta());
                        opts.add(o);
                    }
                    combinada.setOpciones(opts);
                    procesadas.add(combinada);

                } else {
                    // Solo un elemento → pregunta simple, la dejamos igual
                    procesadas.add(sub.get(0));
                }
            }

            mapProcesado.put(seccion, procesadas);
        }

        // 3) Preparar títulos y adapter
        List<String> titles = new ArrayList<>(mapProcesado.keySet());
        viewPager.setAdapter(
                new SectionPagerAdapter(this, titles, mapProcesado, info, insta_especialista, tipoFicha, docente_enficha)
        );
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, pos) -> tab.setText(titles.get(pos))
        ).attach();
    }

    public void submitAll() {
        // 1) Validar SINGLE_CHOICE leyendo del ViewModel
        for (Pregunta p : allPreguntas) {
            if ("SINGLE_CHOICE".equals(p.getTipoPregunta())) {
                String ans = vm.getAnswer(p.getIdPregunta());
                if (ans == null || ans.isEmpty()) {
                    // Mover ViewPager a la sección con esa pregunta
                    int idx = sectionTitles.indexOf(p.getSeccion());
                    viewPager.setCurrentItem(idx, true);

                    Toast.makeText(this,
                                    "Debes responder: " + p.getTextoPregunta(),
                                    Toast.LENGTH_LONG)
                            .show();
                    return;
                }
            }
        }

        // 2) Armar params textuales
        Map<String,String> params = new HashMap<>();

        LocationProvider.requestSingle(this, loc -> {
            // 1) Incluye coords en params
            params.put("latitud",  String.valueOf(loc.getLatitude()));
            params.put("longitud", String.valueOf(loc.getLongitude()));
        });

        params.put("idficha",     allPreguntas.get(0).getIdFicha());
        params.put("colegio",     info.getNombre());
        params.put("codmod",      info.getCodigoModular());
        params.put("codlocal",      info.getCodigoLocal());
        params.put("docidentidad", info.getDirector());
        params.put("docente",         docente_enficha);
        params.put("nrovisita",   nrovisita);
        params.put("rol",         tipoFicha);
        params.put("especialista_nombre",  insta_especialista.getNombre_especialista());
        params.put("especialista_id",  insta_especialista.getId_especialista());
        params.put("especialista_dni",  insta_especialista.getDni_especialista());

        // TEXT + SINGLE_CHOICE + COMMENT
        for (Pregunta p : allPreguntas) {
            String idp = p.getIdPregunta();
            params.put("idpregunta", idp);
            String base = "q_" + p.getIdPregunta();
            switch(p.getTipoPregunta()) {
                case "TEXT":
                    // suponiendo que també guardas en VM o buscas el EditText…
                    String t = vm.getTextAnswer(p.getIdPregunta());
                    params.put(base + "_text", t==null?"":t);
                    break;
                case "SINGLE_CHOICE":
                    String sel = vm.getAnswer(p.getIdPregunta());
                    params.put(base + "_resp", sel);
                    break;
                case "MULTIPLE_OPTION":
                    // Une las seleccionadas con punto y coma
                    List<String> chosen = vm.getMultiAnswers(p.getIdPregunta());
                    String joined = TextUtils.join(";", chosen);
                    params.put(base + "_resp", joined);
                    break;
                // PHOTO si tienes campos de URI en VM…
            }
            // Comentario extra (idem)
            String c = vm.getComment(p.getIdPregunta());
            if (c != null && !c.isEmpty()) {
                params.put(base + "_comment", c);
                params.put("q_" + p.getIdPregunta() + "_comment", c);
            }
        }

        // 3) Armar byteData para fotos desde VM
        Map<String, VolleyMultipartRequest.DataPart> byteData = new HashMap<>();
        vm.getAllPhotoUris().forEach((qid, uri) -> {
            byte[] bytes = UriUtils.readBytesFromUri(this, uri);
            byteData.put("foto" + qid,
                    new VolleyMultipartRequest.DataPart("foto_" + qid + ".jpg", bytes, "image/jpeg")
            );
        });

        // 4) Envío offline/online
        if (NetworkUtil.isConnected(this)) {
            new FichaUploadManager(this)
                    .upload(params, byteData, new FichaUploadManager.UploadCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(
                                    FichaQuestionsActivity.this,
                                    "Ficha enviada correctamente",
                                    Toast.LENGTH_SHORT
                            ).show();
                            vm.clearPendingTask("FICHA", params);
                            vm.clearAllAnswers(); // opcional: reset UI

                            // Regresar a EspecialistaActivity
                            Intent i = new Intent(FichaQuestionsActivity.this, EspecialistaActivity.class);
                            // Estas flags reenvían a la instancia existente si está en el back-stack
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(i);

                            finish();
                        }
                        @Override public void onError(Throwable t) {
                            if (!NetworkUtil.isConnected(FichaQuestionsActivity.this)) {
                                // realmente sin red
                                vm.enqueueUploadTask("FICHA", null, params);
                                Toast.makeText(
                                        FichaQuestionsActivity.this,
                                        "Sin conexión: ficha encolada",
                                        Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                // hubo red, pero el servidor devolvió error
                                Toast.makeText(
                                        FichaQuestionsActivity.this,
                                        "Error al enviar ficha: " + t.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    });
        } else {
            vm.enqueueUploadTask("FICHA", null, params);
            Toast.makeText(this,
                    "Error: ficha encolada",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onHeaderDocenteChanged(String nuevoDocente) {
        docente_enficha = nuevoDocente;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // listo para abrir cámara cuando sea necesario
            } else {
                Toast.makeText(this,
                        "Permiso de cámara necesario para tomar fotos",
                        Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}