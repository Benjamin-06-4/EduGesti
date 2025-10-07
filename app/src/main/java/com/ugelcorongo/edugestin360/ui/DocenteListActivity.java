package com.ugelcorongo.edugestin360.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Docente;
import com.ugelcorongo.edugestin360.ui.adapter.DocenteAdapter;
import com.ugelcorongo.edugestin360.utils.DataLoader;

import java.util.List;

public class DocenteListActivity extends AppCompatActivity {

    private ListView lvDocentes;
    private DocenteAdapter adapter;
    private ColegioInfo colegioInfo;
    private TextView tvInstitution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docente_list);

        tvInstitution = findViewById(R.id.tvInstitution);
        // 1) Recibir ColegioInfo
        colegioInfo = getIntent().getParcelableExtra("EXTRA_COLEGIO_INFO");
        tvInstitution.setText(colegioInfo.getNombre());

        // 2) Cargar lista
        List<Docente> lista = DataLoader.loadDocentes(
                this,
                colegioInfo.getNombre()
        );

        // 3) Configurar ListView y Adapter
        lvDocentes = findViewById(R.id.lvDocentes);
        adapter = new DocenteAdapter(this, lista);
        lvDocentes.setAdapter(adapter);

        // 4) Botón “Agregar Docente” en la última fila
        adapter.setOnAddClick(v -> {
            startActivityForResult(
                    new Intent(this, DocenteCrudActivity.class)
                            .putExtra("modo","create")
                            .putExtra("EXTRA_COLEGIO_INFO", colegioInfo),
                    100
            );
        });

        // 5) Editar / Eliminar callbacks
        adapter.setOnEditClick(doc -> {
            Intent i = new Intent(this, DocenteCrudActivity.class);
            i.putExtra("modo", "edit");
            i.putExtra("docente", doc);
            i.putExtra("EXTRA_COLEGIO_INFO", colegioInfo);
            startActivity(i);
        });
        adapter.setOnDeleteClick(doc -> {
            adapter.deleteDocente(doc);
        });
    }
}