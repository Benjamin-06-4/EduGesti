package com.ugelcorongo.edugestin360.domain.models;

public class Ficha {
    private String id;
    private String nombre;
    private String fechaInicio;
    private String fechaTermino;
    private String estado;
    private String visita;     // "si"/"no"
    private String tipoFicha;  // "Director" o "Docente"
    private String area; // "AGA" | "AGI" | "AGP"

    public Ficha(String id, String nombre, String fechaInicio, String fechaTermino,
                 String estado, String visita, String tipoFicha, String area) {
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaTermino = fechaTermino;
        this.estado = estado;
        this.visita = visita;
        this.tipoFicha = tipoFicha;
        this.area = area;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaTermino() {
        return fechaTermino;
    }

    public void setFechaTermino(String fechaTermino) {
        this.fechaTermino = fechaTermino;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getVisita() {
        return visita;
    }

    public void setVisita(String visita) {
        this.visita = visita;
    }

    public String getTipoFicha() {
        return tipoFicha;
    }

    public void setTipoFicha(String tipoFicha) {
        this.tipoFicha = tipoFicha;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }
}