package com.ugelcorongo.edugestin360.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Docente;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class DocenteCrudActivity extends AppCompatActivity {
    private EditText etDni, etDocente, etCargoLaboral, etJornada;
    private Spinner spNivel, spSituacion, spSexo;
    private ImageView btnBuscar, btnEliminar;
    private Button btnGuardar;
    private String modo;
    private Docente docente;
    private TextView tvInstitution;
    private ColegioInfo colegioInfo;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_docente_crud);

        colegioInfo = getIntent().getParcelableExtra("EXTRA_COLEGIO_INFO");
        // referencias
        etDni = findViewById(R.id.etDni);
        btnBuscar   = findViewById(R.id.btnBuscar);
        btnEliminar= findViewById(R.id.btnEliminar);
        btnGuardar = findViewById(R.id.btnGuardar);
        etDocente  = findViewById(R.id.etDocente);
        spNivel   = findViewById(R.id.spNivelEducativo);
        etCargoLaboral   = findViewById(R.id.etCargoLaboral);
        spSituacion = findViewById(R.id.spSituacionLaboral);
        etJornada   = findViewById(R.id.etJornada);
        spSexo      = findViewById(R.id.spSexo);
        tvInstitution = findViewById(R.id.tvInstitution);

        // 2) Evitar tildes en apellidos/nombre
        InputFilter removeAccents = (source, start, end, dest, dstart, dend) -> {
            String normalized = Normalizer.normalize(
                    source.subSequence(start, end),
                    Normalizer.Form.NFD
            ).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            return normalized;
        };
        etDocente.setFilters(new InputFilter[]{ removeAccents });
        etCargoLaboral.setFilters(new InputFilter[]{ removeAccents });
        etJornada.setFilters(new InputFilter[]{ removeAccents });

        modo = getIntent().getStringExtra("modo");
        if ("edit".equals(modo)) {
            docente = getIntent().getParcelableExtra("docente");
            fillForm(docente);
            btnEliminar.setVisibility(View.VISIBLE);
        } else {
            btnEliminar.setVisibility(View.GONE);
        }

        tvInstitution.setText(colegioInfo.getNombre());

        // eventos
        btnBuscar.setOnClickListener(v -> onBuscarDni());
        btnEliminar.setOnClickListener(v -> onEliminar());
        btnGuardar.setOnClickListener(v -> onGuardar());
    }

    private void fillForm(Docente d) {
        etDni.setText(d.getDocIdentidad());
        etDocente.setText(d.getNombre().split(" ")[0]);

        etJornada.setText(""+d.getJornadaLaboral());
        etCargoLaboral.setText(""+d.getCargo());
    }

    private void onBuscarDni() {
        String dni = etDni.getText().toString().trim();
        if (dni.isEmpty()) return;
        NetworkUtil.get(
                this,
                URLPostHelper.Director.GetDocenteByDni + "?docidentidad=" + dni,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        // extraigo datos
                        String docId   = obj.getString("docidentidad");
                        String nombre  = obj.getString("docente");
                        etDni.setText(docId);
                        etDocente.setText(nombre);

                        // habilita guardar y eliminar
                        btnEliminar.setVisibility(View.VISIBLE);
                        btnGuardar.setEnabled(true);
                    } catch (JSONException e) {
                        Toast.makeText(this,
                                "Error al procesar datos",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                },
                err -> Toast.makeText(this,"No encontrado",Toast.LENGTH_SHORT).show()
        );
    }

    private void onEliminar() {
        Map<String,String> m = new HashMap<>();
        m.put("idcolegiodocente", docente.getIdColegioDocente());
        NetworkUtil.postForm(
                this,
                URLPostHelper.Director.DeleteDocente,
                m,
                r -> {
                    setResult(RESULT_OK);
                    finish();
                },
                e -> Toast.makeText(this,"Error al eliminar",Toast.LENGTH_SHORT).show()
        );
    }

    private void onGuardar() {
        Map<String,String> m = new HashMap<>();
        if ("edit".equals(modo)) {
            m.put("idcolegiodocente", docente.getIdColegioDocente());
        }
        m.put("idcolegio",   colegioInfo.getIdcolegio());
        m.put("colegio",   colegioInfo.getNombre());
        m.put("docidentidad", etDni.getText().toString());
        // campos de formulario
        m.put("docente", etDocente.getText().toString());
        m.put("nivel_educativo",  spNivel.getSelectedItem().toString());
        m.put("cargo",            etCargoLaboral.getText().toString());
        m.put("condicion",        spSituacion.getSelectedItem().toString());
        m.put("jornada_laboral",  etJornada.getText().toString());
        m.put("sexo",             spSexo.getSelectedItem().toString());

        String endpoint = ("edit".equals(modo))
                ? URLPostHelper.Director.UpdateDocente
                : URLPostHelper.Director.CreateDocente;

        NetworkUtil.postForm(
                this,
                endpoint,
                m,
                r -> {
                    setResult(RESULT_OK);
                    finish();
                },
                e -> Toast.makeText(this,"Error guardando",Toast.LENGTH_SHORT).show()
        );
    }
}