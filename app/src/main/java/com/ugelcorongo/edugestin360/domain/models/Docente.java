package com.ugelcorongo.edugestin360.domain.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Docente  implements Parcelable{
    private String idColegioDocente;
    private String idColegio;
    private String colegio;
    private String idDocente;
    private String docIdentidad;
    private String nombre;
    private String nivelEducativo;
    private String cargo;
    private String condicion;
    private String jornadaLaboral;
    private int diaSemana;
    private String horaEntrada;
    private String horaSalida;
    private String estado;
    private String fechaRegistro;

    public Docente(String idColegioDocente, String idColegio, String colegio, String idDocente, String docIdentidad,
                   String nombre, String nivelEducativo, String cargo, String condicion, String jornadaLaboral,
                   int diaSemana, String horaEntrada, String horaSalida, String estado, String fechaRegistro) {
        this.idColegioDocente = idColegioDocente;
        this.idColegio = idColegio;
        this.colegio = colegio;
        this.idDocente = idDocente;
        this.docIdentidad = docIdentidad;
        this.nombre = nombre;
        this.nivelEducativo = nivelEducativo;
        this.cargo = cargo;
        this.condicion = condicion;
        this.jornadaLaboral = jornadaLaboral;
        this.diaSemana = diaSemana;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
    }

    protected Docente(Parcel in) {
        idColegioDocente    = in.readString();
        idColegio           = in.readString();
        colegio             = in.readString();
        idDocente           = in.readString();
        docIdentidad        = in.readString();
        nombre              = in.readString();
        nivelEducativo      = in.readString();
        cargo               = in.readString();
        condicion           = in.readString();
        jornadaLaboral      = in.readString();
        diaSemana           = in.readInt();
        horaEntrada         = in.readString();
        horaSalida          = in.readString();
        estado              = in.readString();
        fechaRegistro       = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(idColegioDocente);
        dest.writeString(idColegio);
        dest.writeString(colegio);
        dest.writeString(idDocente);
        dest.writeString(docIdentidad);
        dest.writeString(nombre);
        dest.writeString(nivelEducativo);
        dest.writeString(cargo);
        dest.writeString(condicion);
        dest.writeString(jornadaLaboral);
        dest.writeInt(diaSemana);
        dest.writeString(horaEntrada);
        dest.writeString(horaSalida);
        dest.writeString(estado);
        dest.writeString(fechaRegistro);
    }

    public static final Creator<Docente> CREATOR = new Creator<Docente>() {
        @Override
        public Docente createFromParcel(Parcel in) {
            return new Docente(in);
        }

        @Override
        public Docente[] newArray(int size) {
            return new Docente[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public String getIdColegioDocente() {
        return idColegioDocente;
    }

    public void setIdColegioDocente(String idColegioDocente) {
        this.idColegioDocente = idColegioDocente;
    }

    public String getIdColegio() {
        return idColegio;
    }

    public void setIdColegio(String idColegio) {
        this.idColegio = idColegio;
    }

    public String getColegio() {
        return colegio;
    }

    public void setColegio(String colegio) {
        this.colegio = colegio;
    }

    public String getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(String idDocente) {
        this.idDocente = idDocente;
    }

    public String getDocIdentidad() {
        return docIdentidad;
    }

    public void setDocIdentidad(String docIdentidad) {
        this.docIdentidad = docIdentidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNivelEducativo() {
        return nivelEducativo;
    }

    public void setNivelEducativo(String nivelEducativo) {
        this.nivelEducativo = nivelEducativo;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getCondicion() {
        return condicion;
    }

    public void setCondicion(String condicion) {
        this.condicion = condicion;
    }

    public String getJornadaLaboral() {
        return jornadaLaboral;
    }

    public void setJornadaLaboral(String jornadaLaboral) {
        this.jornadaLaboral = jornadaLaboral;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(String horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public String getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(String horaSalida) {
        this.horaSalida = horaSalida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}