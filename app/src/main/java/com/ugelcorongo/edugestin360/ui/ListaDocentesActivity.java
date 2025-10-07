package com.ugelcorongo.edugestin360.ui;

import android.os.Bundle;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Especialista;
import com.ugelcorongo.edugestin360.managers.file.FileUpdaterFactory;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;
import com.ugelcorongo.edugestin360.ui.adapter.FichaQuestionsActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.ugelcorongo.edugestin360.R;

public class ListaDocentesActivity extends AppCompatActivity {
    private ListView lvDocentes;
    private Button btnNuevaFicha;

    private String idFicha;
    private String nrovisita;
    private String fichaNombre;
    private List<String> docentes = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String tipoFicha = "Docente";
    private String colegio;
    private ColegioInfo colegioInfo;
    private Especialista instaEspecialista;


    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_lista_docentes);

        // 1) Leer extras
        idFicha         = getIntent().getStringExtra("idFicha");
        nrovisita          = getIntent().getStringExtra("visita");
        colegio         = getIntent().getStringExtra("colegio");
        colegioInfo     = getIntent().getParcelableExtra("EXTRA_COLEGIO_INFO");
        instaEspecialista = getIntent().getParcelableExtra("EXTRA_ESPECIALISTA_INFO");
        fichaNombre = getIntent().getStringExtra("nombreFicha");

        TextView tvFichaName   = findViewById(R.id.tvFichaName);
        TextView tvColegioName = findViewById(R.id.tvColegioName);
        tvFichaName.setText(fichaNombre);
        tvColegioName.setText(colegio);

        lvDocentes      = findViewById(R.id.lvDocentes);
        btnNuevaFicha   = findViewById(R.id.btnNuevaFicha);


        // 3) Adaptador y click en ítem
        adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, docentes
        );
        lvDocentes.setAdapter(adapter);

        lvDocentes.setOnItemClickListener((AdapterView<?> parent, View v, int pos, long id) -> {
            String docenteName = docentes.get(pos);
            startFichaActivity(docenteName);
        });

        // 4) Botón Nueva Ficha → sin docente prefijado
        btnNuevaFicha.setOnClickListener(v -> startFichaActivity(null));

        // 2) Cargar lista desde archivo interno y filtrar
        loadDocentesFromFile();
    }

    private void loadDocentesFromFile() {
        // Construir URL con placeholders
        String url = String.format(
                URLPostHelper.Fichas.DocentesEnFichas,
                idFicha,
                nrovisita
        );

        // Volley para GET
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Parsear líneas
                        String[] lines = response.split("\\r?\\n");
                        for (String line : lines) {
                            if (line.trim().isEmpty()) continue;
                            String[] parts = line.split(";");
                            if (parts.length < 3) continue;

                            int f   = Integer.parseInt(parts[0]);
                            int vis = Integer.parseInt(parts[1]);
                            String name = parts[2];

                            if (f == Integer.parseInt(idFicha)
                                    && vis == Integer.parseInt(nrovisita)) {
                                docentes.add(name);
                            }
                        }

                        // Refrescar la lista en pantalla
                        adapter.notifyDataSetChanged();

                        if (docentes.isEmpty()) {
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        );

        queue.add(req);
    }

    private void startFichaActivity(@Nullable String docenteName) {
        Intent i = new Intent(this, FichaQuestionsActivity.class);
        i.putExtra("idFicha", idFicha);
        i.putExtra("tipoFicha", tipoFicha);
        i.putExtra("nrovisita", nrovisita);
        i.putExtra("colegio", colegio);
        i.putExtra("EXTRA_COLEGIO_INFO", colegioInfo);
        i.putExtra("EXTRA_ESPECIALISTA_INFO", instaEspecialista);
        if (docenteName != null) {
            i.putExtra("EXTRA_DOCENTE_NAME", docenteName);
        }
        startActivity(i);
    }
}