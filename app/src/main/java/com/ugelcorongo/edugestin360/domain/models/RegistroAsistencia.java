package com.ugelcorongo.edugestin360.domain.models;

public class RegistroAsistencia {
    private String docIdentidad;
    private String tipoRegistro;
    private String horaRegistro;
    private int tardanza;

    public RegistroAsistencia(String docIdentidad, String tipoRegistro, String horaRegistro, int tardanza) {
        this.docIdentidad = docIdentidad;
        this.tipoRegistro = tipoRegistro;
        this.horaRegistro = horaRegistro;
        this.tardanza = tardanza;
    }

    public String getDocIdentidad() {
        return docIdentidad;
    }

    public void setDocIdentidad(String docIdentidad) {
        this.docIdentidad = docIdentidad;
    }

    public String getTipoRegistro() {
        return tipoRegistro;
    }

    public void setTipoRegistro(String tipoRegistro) {
        this.tipoRegistro = tipoRegistro;
    }

    public String getHoraRegistro() {
        return horaRegistro;
    }

    public void setHoraRegistro(String horaRegistro) {
        this.horaRegistro = horaRegistro;
    }

    public int getTardanza() {
        return tardanza;
    }

    public void setTardanza(int tardanza) {
        this.tardanza = tardanza;
    }
}
