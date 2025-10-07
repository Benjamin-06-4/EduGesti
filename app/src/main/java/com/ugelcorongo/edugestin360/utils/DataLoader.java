package com.ugelcorongo.edugestin360.utils;

import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Docente;
import com.ugelcorongo.edugestin360.domain.models.RegistroAsistencia;
import com.ugelcorongo.edugestin360.managers.file.FileUpdater;
import com.ugelcorongo.edugestin360.managers.file.FileUpdaterFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Carga colegios, docentes y horarios desde archivos internos,
 * actualizando automáticamente si hay Internet.
 */
public class DataLoader {
    private static ColegioInfo dataColegioInfo;
    private static final String TAG = "FETCH_ERROR";
    private static final String COLEGIO_FILE = "datacolegio.txt";
    private static final String DOCENTE_FILE = "datacolegiodocente.txt";
    private static final String REGISTRO_FILE = "director_registros.txt";

    /**
     * 1) Encuentra el ColegioInfo donde el director coincide.
     *    Filtra datacolegio.txt por columna doc_identidad.
     */
    public static ColegioInfo loadColegioInfo(
            Context ctx,
            String directorDocIdent
    ) {
        dataColegioInfo = null;
        File local = ctx.getFileStreamPath(COLEGIO_FILE);

        // 1) Copia inicial desde res/raw si no existe o está vacío
        if (!local.exists() || local.length() == 0) {
            try (
                    InputStream in = ctx.getResources().openRawResource(R.raw.datacolegio);
                    FileOutputStream out = ctx.openFileOutput(COLEGIO_FILE, Context.MODE_PRIVATE)
            ) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (Exception ex) {
            }
        }

        // 2) Construir URL con ?director=... (URLPostHelper debe apuntar al endpoint correcto)
        String url = URLPostHelper.Director.Colegio;
        try {
            String q = URLEncoder.encode(directorDocIdent, "UTF-8");
            url += "?director=" + q;
        } catch (UnsupportedEncodingException e) {
            url += "?director=" + directorDocIdent;
        }

        // 3) Permitir red en Main Thread (evita NetworkOnMainThreadException en SDK ≥ 9)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .permitAll()
                            .build()
            );
        }

        // 4) Forzar actualización síncrona del fichero local
        try {
            FileUpdaterFactory
                    .create(ctx, COLEGIO_FILE, url)
                    .update();
        } catch (Exception ex) {
        }

        // 5) Leer todas las líneas de la copia local ya actualizada
        List<String> lines;
        try {
            lines = FileUpdaterFactory
                    .create(ctx, COLEGIO_FILE, url)
                    .readLines();
        } catch (Exception ex) {
            return null;
        }

        // 6) Buscar la línea donde p[1] = directorDocIdent
        //    Campos: [0]=nombre, [1]=doc_identidad, [5]=lat, [6]=lon,
        //            [7]=codigoModular, [8]=codigoLocal, [9]=clave8, [10]=nivel
        for (String line : lines) {
            String[] p = line.split(";");
            if (p.length >= 10 && p[1].equalsIgnoreCase(directorDocIdent)) {
                try {
                    dataColegioInfo = new ColegioInfo(
                            p[0],                     // nombre
                            p[7],                     // codigoModular
                            p[8],                     // codigoLocal
                            p[9],                     // clave8
                            p[1],                     // director (doc_identidad)
                            p[10],                    // nivel
                            Double.parseDouble(p[5]), // latitud
                            Double.parseDouble(p[6]),  // longitud
                            p[4],                       // idcolegio
                            p[2]                        // usuario
                    );
                } catch (NumberFormatException nfe) {
                }
                break;
            }
        }

        return dataColegioInfo;
    }

    /**
     * 2) Carga la lista de Docente para un colegio dado.
     *    Filtra datacolegiodocente.txt por colegio y estado="activo".
     */
    public static List<Docente> loadDocentes(
            Context ctx,
            String colegioName
    ) {
        List<Docente> lista = new ArrayList<>();
        File local = ctx.getFileStreamPath(DOCENTE_FILE);

        // 1) Copia inicial desde res/raw si no existe o está vacío
        if (!local.exists() || local.length() == 0) {
            try (
                    InputStream in = ctx.getResources().openRawResource(R.raw.datacolegiodocente);
                    FileOutputStream out = ctx.openFileOutput(DOCENTE_FILE, Context.MODE_PRIVATE)
            ) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (Exception ex) {
            }
        }

        // 2) Construir URL con ?colegio=...
        String url = URLPostHelper.Director.Docentes;
        try {
            String q = URLEncoder.encode(colegioName, "UTF-8");
            url += "?colegio=" + q;
        } catch (UnsupportedEncodingException e) {
            url += "?colegio=" + colegioName;
        }

        // 3) Permitir red en Main Thread (forzar síncrono)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .permitAll()
                            .build()
            );
        }

        // 4) Forzar actualización síncrona del fichero local
        try {
            FileUpdaterFactory
                    .create(ctx, DOCENTE_FILE, url)
                    .update();
        } catch (Exception ex) {
        }

        // 5) Leer todas las líneas de la copia local ya actualizada
        List<String> lines;
        try {
            lines = FileUpdaterFactory
                    .create(ctx, DOCENTE_FILE, url)
                    .readLines();
        } catch (Exception ex) {
            return lista;
        }

        // 6) Filtrar por colegioName == p[2] y estado activo (p[13])
        for (String line : lines) {
            String[] p = line.split(";");
            if (p.length >= 15
                    && p[2].equalsIgnoreCase(colegioName)
                    && p[13].equalsIgnoreCase("activo")) {
                try {
                    Docente d = new Docente(
                            p[0],   // idcolegiodocente
                            p[1],   // idcolegio
                            p[2],   // colegio
                            p[3],   // iddocente
                            p[4],   // docidentidad
                            p[5],   // docente
                            p[6],   // nivel_educativo
                            p[7],   // cargo
                            p[8],   // condicion
                            p[9],   // jornada_laboral
                            Integer.parseInt(p[10]), // diasemana
                            p[11],  // hora_entrada
                            p[12],  // hora_salida
                            p[13],  // estado
                            p[14]   // fecha_registro
                    );
                    lista.add(d);
                } catch (Exception e) {
                }
            }
        }
        return lista;
    }

    /**
     * 3) Carga la lista de Horario para un colegio dado.
     *    Filtra datacolegiodocente.txt por colegio.
     *    (Cada entrada lleva también diasemana, hora_entrada, hora_salida)
     */
    public static List<RegistroAsistencia> loadRegistros(
            Context ctx,
            String colegioName,
            int diaSemana
    ) {
        List<RegistroAsistencia> lista = new ArrayList<>();
        File local = ctx.getFileStreamPath(REGISTRO_FILE);

        // 1) Copia inicial desde res/raw
        if (!local.exists() || local.length() == 0) {
            try (
                    InputStream in = ctx.getResources().openRawResource(R.raw.director_registros);
                    FileOutputStream out = ctx.openFileOutput(REGISTRO_FILE, Context.MODE_PRIVATE)
            ) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (Exception ex) {
            }
        }

        // 2) Construir URL con ?colegio=...&dia=...&fecha=YYYY-MM-DD
        String url = URLPostHelper.Director.Registros;
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
        try {
            String q = URLEncoder.encode(colegioName, "UTF-8");
            url += "?colegio=" + q
                    + "&dia="     + diaSemana
                    + "&fecha="   + URLEncoder.encode(hoy, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            url += "?colegio=" + colegioName
                    + "&dia="     + diaSemana
                    + "&fecha="   + hoy;
        }

        // 3) Permitir red en Main Thread
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .permitAll()
                            .build()
            );
        }

        // 4) Forzar update síncrono
        try {
            FileUpdaterFactory
                    .create(ctx, REGISTRO_FILE, url)
                    .update();
        } catch (Exception ex) {
        }

        // 5) Leer líneas
        List<String> lines;
        try {
            lines = FileUpdaterFactory
                    .create(ctx, REGISTRO_FILE, url)
                    .readLines();
        } catch (Exception ex) {
            return lista;
        }

        // 6) Parsear: docidentidad;tipo;hora;­tardanza
        for (String line : lines) {
            String[] p = line.split(";");
            if (p.length >= 4) {
                try {
                    RegistroAsistencia r = new RegistroAsistencia(
                            p[0],          // docidentidad
                            p[1],          // tipo_registro
                            p[2],          // hora_registro
                            Integer.parseInt(p[3])  // tardanza
                    );
                    lista.add(r);
                } catch (Exception e) {
                }
            }
        }
        return lista;
    }
}