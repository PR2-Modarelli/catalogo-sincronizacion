package ar.com.modarelli.catalogo.modelo;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class HorarioSemanal {

    private Long id;
    private Profesional profesional;
    private DayOfWeek diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int duracionTurnoMinutos;
    private boolean habilitado;

    public HorarioSemanal(
        Long id,
        Profesional profesional,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        int duracionTurnoMinutos,
        boolean habilitado
    ) {
        this.id = id;
        this.profesional = profesional;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionTurnoMinutos = duracionTurnoMinutos;
        this.habilitado = habilitado;
    }

    public Long getId() {
        return id;
    }

    public Profesional getProfesional() {
        return profesional;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public int getDuracionTurnoMinutos() {
        return duracionTurnoMinutos;
    }

    public boolean isHabilitado() {
        return habilitado;
    }
}