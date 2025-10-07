package com.ugelcorongo.edugestin360.domain.models;

public class Horario {
    private String docIdent;
    private int diaSemana;
    private String horaLlegada;
    private String horaSalida;

    public Horario(String docIdent, int diaSemana, String horaLlegada, String horaSalida) {
        this.docIdent = docIdent;
        this.diaSemana = diaSemana;
        this.horaLlegada = horaLlegada;
        this.horaSalida = horaSalida;
    }

    public String getDocIdent() {
        return docIdent;
    }

    public void setDocIdent(String docIdent) {
        this.docIdent = docIdent;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getHoraLlegada() {
        return horaLlegada;
    }

    public void setHoraLlegada(String horaLlegada) {
        this.horaLlegada = horaLlegada;
    }

    public String getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(String horaSalida) {
        this.horaSalida = horaSalida;
    }
}
