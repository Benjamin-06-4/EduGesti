package com.ugelcorongo.edugestin360.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Especialista;
import com.ugelcorongo.edugestin360.domain.models.Ficha;
import com.ugelcorongo.edugestin360.ui.adapter.FichaQuestionsActivity;
import com.ugelcorongo.edugestin360.ui.ListaDocentesActivity;
import com.ugelcorongo.edugestin360.R;
public class FichasAdapter extends ArrayAdapter<Ficha> {
    private Context context;
    private String colegio;
    private ColegioInfo colegioInfo;
    private String idcolegio;
    private String docente;
    private String rol;
    private String visita;
    private String especialistaId;
    private String docIdentidad;
    private Especialista insta_especialista;

    public FichasAdapter(@NonNull Context context, @NonNull List<Ficha> fichas, @NonNull ColegioInfo colegioInfo,
                         String colegio, String idcolegio, String docente, String rol,
                         String especialistaId, String docIdentidad, @NonNull Especialista especialista) {
        super(context, 0, fichas);
        this.context = context;
        this.colegioInfo = colegioInfo;
        this.colegio = colegio;
        this.idcolegio = idcolegio;
        this.docente = docente;
        this.rol = rol;
        this.especialistaId = especialistaId;
        this.docIdentidad = docIdentidad;;
        this.insta_especialista = especialista;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.row_add_fichas_especialistas, parent, false);
        }

        // Recuperar la fila (row) y sus componentes
        EditText txtFichaTitle = convertView.findViewById(R.id.edit_docente_name);
        ImageButton btnHorario = convertView.findViewById(R.id.btn_horario);

        // String ficha = fichas.get(position);
        Ficha ficha = getItem(position);
        if (ficha == null) return convertView;

        txtFichaTitle.setText(ficha.getNombre());
        // Hacemos que el EditText actúe como etiqueta (no editable)
        // Texto no editable txtFichaTitle.setKeyListener(null);
        txtFichaTitle.setKeyListener((KeyListener) null);

        // Listener para el ImageButton de cada fila

        btnHorario.setOnClickListener(v -> {
            String tipo = ficha.getTipoFicha();
            visita = ficha.getVisita(); // "si" o "no"

            if ("Director".equalsIgnoreCase(tipo)) {
                // Solo si visita == "no"
                Intent i = new Intent(context, FichaQuestionsActivity.class);
                i.putExtra("idFicha", ficha.getId());
                i.putExtra("tipoFicha", tipo);
                i.putExtra("colegio", colegio);
                i.putExtra("idcolegio", 999);
                i.putExtra("nrovisita", ficha.getVisita());
                i.putExtra("rol", rol);
                i.putExtra("especialistaNombre", docente);
                i.putExtra("EXTRA_COLEGIO_INFO",  colegioInfo);
                i.putExtra("EXTRA_ESPECIALISTA_INFO", insta_especialista);
                context.startActivity(i);
            } else {
                // Tipo Docente → Lista de docentes que ya llenaron ficha
                Intent i = new Intent(context, ListaDocentesActivity.class);
                i.putExtra("idFicha", ficha.getId());
                i.putExtra("nombreFicha", ficha.getNombre());
                i.putExtra("tipoFicha", tipo);
                i.putExtra("colegio", colegio);
                i.putExtra("idcolegio", 999);
                i.putExtra("docente", docente);
                i.putExtra("rol", rol);
                i.putExtra("especialistaId",  especialistaId);
                i.putExtra("docIdentidad",  docIdentidad);
                i.putExtra("visita",  ficha.getVisita());

                i.putExtra("EXTRA_COLEGIO_INFO",  colegioInfo);
                i.putExtra("EXTRA_ESPECIALISTA_INFO", insta_especialista);
                context.startActivity(i);
            }
        });

        return convertView;
    }
}