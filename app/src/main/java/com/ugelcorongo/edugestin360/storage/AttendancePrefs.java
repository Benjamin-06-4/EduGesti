package com.ugelcorongo.edugestin360.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AttendancePrefs {
    private static final String PREF_FILE = "attendance_prefs";
    private static final String KEY_PREFIX = "att_"; // + yyyy-MM-dd + "_" + tipo

    private final SharedPreferences prefs;

    public AttendancePrefs(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Devuelve la fecha de hoy en yyyy-MM-dd.
     */
    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }

    /**
     * Indica si ya se registró hoy un tipo dado ("Entrada" o "Salida").
     */
    public boolean isRegistered(String tipo, String colegio) {
        String key = KEY_PREFIX + today() + "_" + colegio + "_" + tipo;
        return prefs.getBoolean(key, false);
    }

    /**
     * Marca como registrado hoy el tipo dado ("Entrada" o "Salida").
     */
    public void setRegistered(String tipo, String colegio) {
        String key = KEY_PREFIX + today() + "_" + colegio + "_" + tipo;
        prefs.edit().putBoolean(key, true).apply();
    }
}