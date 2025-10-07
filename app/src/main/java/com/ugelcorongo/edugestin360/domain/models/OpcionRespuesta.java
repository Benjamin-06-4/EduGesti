package com.ugelcorongo.edugestin360.domain.models;

import android.os.Parcel;
import android.os.Parcelable;

public class OpcionRespuesta implements Parcelable {
    private String idRespuesta;
    private String idPregunta;
    private String tipo;          // e.g. "SINGLE_CHOICE", "TEXT"
    private String descripcion;   // e.g. "Sí", "No", "Destacado", …

    public OpcionRespuesta(String idRespuesta,
                           String idPregunta,
                           String tipo,
                           String descripcion) {
        this.idRespuesta   = idRespuesta;
        this.idPregunta    = idPregunta;
        this.tipo          = tipo;
        this.descripcion   = descripcion;
    }

    public OpcionRespuesta() { }

    protected OpcionRespuesta(Parcel in) {
        idRespuesta   = in.readString();
        idPregunta    = in.readString();
        tipo          = in.readString();
        descripcion   = in.readString();
    }

    public static final Creator<OpcionRespuesta> CREATOR =
            new Creator<OpcionRespuesta>() {
                @Override
                public OpcionRespuesta createFromParcel(Parcel in) {
                    return new OpcionRespuesta(in);
                }
                @Override
                public OpcionRespuesta[] newArray(int size) {
                    return new OpcionRespuesta[size];
                }
            };

    public String getIdRespuesta() { return idRespuesta; }
    public String getIdPregunta()  { return idPregunta; }
    public String getTipo()        { return tipo; }
    public String getDescripcion() { return descripcion; }


    public void setIdRespuesta(String idRespuesta) { this.idRespuesta = idRespuesta; }
    public void setIdPregunta(String idPregunta) { this.idPregunta = idPregunta; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }


    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(idRespuesta);
        dest.writeString(idPregunta);
        dest.writeString(tipo);
        dest.writeString(descripcion);
    }
}