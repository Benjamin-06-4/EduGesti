package com.ugelcorongo.edugestin360.domain.models;

import android.os.Parcel;
import android.os.Parcelable;

public class ColegioInfo implements Parcelable {
    private final String nombre;
    private final String codigoModular;
    private final String codigoLocal;
    private final String clave8;
    private final String director;
    private final String nivel;
    private final double lat;
    private final double lon;
    private final String idcolegio;
    private final String nombreDirector;

    public ColegioInfo(String nombre,
                       String codigoModular,
                       String codigoLocal,
                       String clave8,
                       String director,
                       String nivel,
                       double lat,
                       double lon,
                       String idcolegio,
                       String nombreDirector) {
        this.nombre = nombre;
        this.codigoModular = codigoModular;
        this.codigoLocal = codigoLocal;
        this.clave8 = clave8;
        this.director = director;
        this.nivel = nivel;
        this.lat = lat;
        this.lon = lon;
        this.idcolegio = idcolegio;
        this.nombreDirector = nombreDirector;
    }

    protected ColegioInfo(Parcel in) {
        nombre = in.readString();
        codigoModular = in.readString();
        codigoLocal = in.readString();
        clave8 = in.readString();
        director = in.readString();
        nivel = in.readString();
        if (in.readByte() == 1) lat = in.readDouble(); else lat = 0.00;
        if (in.readByte() == 1) lon = in.readDouble(); else lon = 0.00;
        idcolegio = in.readString();
        nombreDirector = in.readString();
    }

    public static final Creator<ColegioInfo> CREATOR = new Creator<ColegioInfo>() {
        @Override
        public ColegioInfo createFromParcel(Parcel in) {
            return new ColegioInfo(in);
        }

        @Override
        public ColegioInfo[] newArray(int size) {
            return new ColegioInfo[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nombre);
        dest.writeString(codigoModular);
        dest.writeString(codigoLocal);
        dest.writeString(clave8);
        dest.writeString(director);
        dest.writeString(nivel);
        if (lat != 0.00) {
            dest.writeByte((byte)1);
            dest.writeDouble(lat);
        } else {
            dest.writeByte((byte)0);
        }
        if (lon != 0.00) {
            dest.writeByte((byte)1);
            dest.writeDouble(lon);
        } else {
            dest.writeByte((byte)0);
        }
        dest.writeString(idcolegio);
        dest.writeString(nombreDirector);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters
    public String getNombre() {
        return nombre;
    }

    public String getCodigoModular() {
        return codigoModular;
    }

    public String getCodigoLocal() {
        return codigoLocal;
    }

    public String getClave8() {
        return clave8;
    }

    public String getDirector() {
        return director;
    }

    public String getNivel() {
        return nivel;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getIdcolegio() {
        return idcolegio;
    }

    public String getNombreDirector() {
        return nombreDirector;
    }
}