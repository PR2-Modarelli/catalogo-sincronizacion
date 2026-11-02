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
        if (profesional == null) {
            throw new IllegalArgumentException(
                "El profesional es obligatorio."
            );
        }

        if (diaSemana == null) {
            throw new IllegalArgumentException(
                "El día de la semana es obligatorio."
            );
        }

        if (horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException(
                "Las horas de inicio y fin son obligatorias."
            );
        }

        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                "La hora de fin debe ser posterior a la hora de inicio."
            );
        }

        if (duracionTurnoMinutos <= 0) {
            throw new IllegalArgumentException(
                "La duración del turno debe ser mayor que cero."
            );
        }

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