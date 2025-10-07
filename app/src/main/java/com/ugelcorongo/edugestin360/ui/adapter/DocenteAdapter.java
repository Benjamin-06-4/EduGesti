package com.ugelcorongo.edugestin360.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.managers.upload.ImageUploadManager;
import com.ugelcorongo.edugestin360.domain.models.Docente;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocenteAdapter extends BaseAdapter {
    public interface OnItemClick { void onClick(Docente d); }
    private final Context ctx;
    private List<Docente> data;
    private OnItemClick onAdd, onEdit, onDelete;

    public DocenteAdapter(Context ctx, List<Docente> list) {
        this.ctx = ctx;
        this.data = new ArrayList<>(list);
    }
    public void setOnAddClick(OnItemClick o)    { onAdd = o; }
    public void setOnEditClick(OnItemClick o)   { onEdit = o; }
    public void setOnDeleteClick(OnItemClick o) { onDelete = o; }

    @Override public int getCount() { return data.size() + 1; }
    @Override public Object getItem(int pos) {
        return pos < data.size() ? data.get(pos) : null;
    }
    @Override public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        if (pos == data.size()) {
            // fila “Agregar Docente”
            View v = LayoutInflater.from(ctx)
                    .inflate(R.layout.row_add_docente, parent, false);
            v.setOnClickListener(x -> {
                if (onAdd != null) onAdd.onClick(null);
            });
            return v;
        }
        // fila docente normal
        Docente d = data.get(pos);
        View v = convert != null
                ? convert
                : LayoutInflater.from(ctx)
                .inflate(R.layout.row_docente, parent, false);
        ((TextView)v.findViewById(R.id.tvNombre))
                .setText(d.getNombre());
        // editar
        v.findViewById(R.id.icon_editar)
                .setOnClickListener(x -> onEdit.onClick(d));
        // eliminar
        v.findViewById(R.id.icon_eliminar)
                .setOnClickListener(x -> onDelete.onClick(d));
        return v;
    }

    /** Borra local y remueve de listado */
    public void deleteDocente(Docente d) {
        // llamar endpoint delete
        NetworkUtil.postForm(
                ctx,
                URLPostHelper.Director.DeleteDocente,
                Map.of("idcolegiodocente", d.getIdColegioDocente()),
                resp -> {
                    data.remove(d);
                    notifyDataSetChanged();
                },
                err -> Toast.makeText(ctx,"No pudo eliminar",Toast.LENGTH_SHORT).show()
        );
    }

    /** Refresca con nueva lista */
    public void refresh(List<Docente> nueva) {
        data.clear();
        data.addAll(nueva);
        notifyDataSetChanged();
    }
}