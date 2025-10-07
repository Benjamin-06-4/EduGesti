package com.ugelcorongo.edugestin360.managers.file;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
public class DocenteInfoParser {
    public final String colegio;
    public final String docidentidad;
    public final String docente;
    public final String nivel;
    public final String estado;

    private DocenteInfoParser(String c, String doc, String d, String n, String e) {
        colegio = c; docidentidad = doc; docente = d; nivel = n; estado = e;
    }

    public static DocenteInfoParser fromLine(String line) {
        String[] p = line.split(";");
        Log.d("FETCH_ERROR", "DocenteInfoParser: " + p.length);
        if (p.length != 5) throw new IllegalArgumentException("Formato inválido");
        return new DocenteInfoParser(
                p[0].trim(),
                p[1].trim(),
                p[2].trim(),
                p[3].trim(),
                p[4].trim()
        );
    }

    public static List<DocenteInfoParser> parseLines(List<String> lines) {
        List<DocenteInfoParser> out = new ArrayList<>();
        for (String L : lines) {
            out.add(fromLine(L));
        }
        return out;
    }
}
