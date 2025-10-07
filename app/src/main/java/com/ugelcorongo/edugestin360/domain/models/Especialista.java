package com.ugelcorongo.edugestin360.domain.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Especialista implements Parcelable {
    private String id_especialista;
    private String dni_especialista;
    private String nombre_especialista;

    public Especialista(String id_especialista, String dni_especialista, String nombre_especialista) {
        this.id_especialista = id_especialista;
        this.dni_especialista = dni_especialista;
        this.nombre_especialista = nombre_especialista;
    }

    protected Especialista(Parcel in) {
        id_especialista = in.readString();
        dni_especialista = in.readString();
        nombre_especialista = in.readString();
    }

    public static final Creator<Especialista> CREATOR = new Creator<Especialista>() {
        @Override
        public Especialista createFromParcel(Parcel in) {
            return new Especialista(in);
        }

        @Override
        public Especialista[] newArray(int size) {
            return new Especialista[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id_especialista);
        dest.writeString(dni_especialista);
        dest.writeString(nombre_especialista);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getId_especialista() {
        return id_especialista;
    }

    public void setId_especialista(String id_especialista) {
        this.id_especialista = id_especialista;
    }

    public String getDni_especialista() {
        return dni_especialista;
    }

    public void setDni_especialista(String dni_especialista) {
        this.dni_especialista = dni_especialista;
    }

    public String getNombre_especialista() {
        return nombre_especialista;
    }

    public void setNombre_especialista(String nombre_especialista) {
        this.nombre_especialista = nombre_especialista;
    }
}
