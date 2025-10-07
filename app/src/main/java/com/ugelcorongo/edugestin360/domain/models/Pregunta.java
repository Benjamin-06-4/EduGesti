package com.ugelcorongo.edugestin360.domain.models;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class Pregunta implements Parcelable{
    private String idFicha;
    private String tipoFicha;
    private String idPregunta;
    private String textoPregunta;
    private String tipoPregunta;
    private boolean requiereComentario;
    private boolean requiereFoto;
    private String seccion;
    private List<OpcionRespuesta> opciones;

    public Pregunta() {
        this.opciones = new ArrayList<>();
    }

    public Pregunta(String idFicha,
                    String tipoFicha,
                    String idPregunta,
                    String textoPregunta,
                    String tipoPregunta,
                    boolean requiereComentario,
                    boolean requiereFoto,
                    String seccion,
                    List<OpcionRespuesta> opciones) {
        this.idFicha            = idFicha;
        this.tipoFicha          = tipoFicha;
        this.idPregunta         = idPregunta;
        this.textoPregunta      = textoPregunta;
        this.tipoPregunta       = tipoPregunta;
        this.requiereComentario = requiereComentario;
        this.requiereFoto       = requiereFoto;
        this.seccion            = seccion;
        this.opciones           = opciones != null ? opciones : new ArrayList<>();
    }

    // Parcelable
    protected Pregunta(Parcel in) {
        idFicha            = in.readString();
        tipoFicha          = in.readString();
        idPregunta         = in.readString();
        textoPregunta      = in.readString();
        tipoPregunta       = in.readString();
        requiereComentario = in.readByte() != 0;
        requiereFoto       = in.readByte() != 0;
        seccion            = in.readString();
        opciones           = in.createTypedArrayList(OpcionRespuesta.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(idFicha);
        dest.writeString(tipoFicha);
        dest.writeString(idPregunta);
        dest.writeString(textoPregunta);
        dest.writeString(tipoPregunta);
        dest.writeString(seccion);
        dest.writeByte((byte) (requiereComentario ? 1 : 0));
        dest.writeByte((byte) (requiereFoto ? 1 : 0));
        dest.writeTypedList(opciones);
    }

    public static final Creator<Pregunta> CREATOR = new Creator<Pregunta>() {
        @Override
        public Pregunta createFromParcel(Parcel in) {
            return new Pregunta(in);
        }

        @Override
        public Pregunta[] newArray(int size) {
            return new Pregunta[size];
        }
    };

    // Getters y setters

    public String getIdFicha() {
        return idFicha;
    }

    public void setIdFicha(String idFicha) {
        this.idFicha = idFicha;
    }

    public String getTipoFicha() {
        return tipoFicha;
    }

    public void setTipoFicha(String tipoFicha) {
        this.tipoFicha = tipoFicha;
    }

    public String getIdPregunta() {
        return idPregunta;
    }

    public void setIdPregunta(String idPregunta) {
        this.idPregunta = idPregunta;
    }

    public String getTextoPregunta() {
        return textoPregunta;
    }

    public void setTextoPregunta(String textoPregunta) {
        this.textoPregunta = textoPregunta;
    }

    public String getTipoPregunta() {
        return tipoPregunta;
    }

    public void setTipoPregunta(String tipoPregunta) {
        this.tipoPregunta = tipoPregunta;
    }

    public boolean isRequiereComentario() {
        return requiereComentario;
    }

    public void setRequiereComentario(boolean requiereComentario) {
        this.requiereComentario = requiereComentario;
    }

    public boolean isRequiereFoto() {
        return requiereFoto;
    }

    public void setRequiereFoto(boolean requiereFoto) {
        this.requiereFoto = requiereFoto;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public List<OpcionRespuesta> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<OpcionRespuesta> opciones) {
        this.opciones = opciones;
    }
}