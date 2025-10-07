package com.ugelcorongo.edugestin360.storage;

import android.location.Location;
import android.util.Log;

public class WorkLocation {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String idcolegio;
    private final String director;
    private final String codigoModular;
    private final String codigoLocal;
    private final String clave8;
    private final String nivel;
    private final String nombreDirector;

    public WorkLocation(String name, double latitude, double longitude, String idcolegio,
                        String director, String codigoModular, String codigoLocal, String clave8,
                        String nivel, String nombreDirector) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.idcolegio = idcolegio;

        this.director = director;
        this.codigoModular = codigoModular;
        this.codigoLocal = codigoLocal;
        this.clave8 = clave8;
        this.nivel = nivel;
        this.nombreDirector = nombreDirector;
    }

    // Getter para resolver selectedWorkLoc.getName()
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getIdcolegio() { return idcolegio; }
    public String getDirector() { return director; }
    public String getCodigoModular() { return codigoModular; }
    public String getCodigoLocal() { return codigoLocal; }
    public String getClave8() { return clave8; }
    public String getNivel() { return nivel; }
    public String getNombreDirector() { return nombreDirector; }

    public double distanceTo(double otherLat, double otherLon) {
        float[] result = new float[1];
        Location.distanceBetween(
                this.latitude, this.longitude,
                otherLat, otherLon,
                result
        );
        return result[0];
    }
}