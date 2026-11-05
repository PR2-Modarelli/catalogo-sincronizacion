package ar.com.modarelli.catalogo.domain;
import jakarta.persistence.*;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="horario_semanal")
public class HorarioSemanal {
    @Id
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="profesional_id", nullable=false)
    private Profesional profesional;
    @Enumerated(EnumType.STRING)
    @Column(name="dia_semana", nullable=false)
    private DayOfWeek diaSemana;
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
    @Column(name="duracion_turno_minutos", nullable=false)
    private int duracionTurnoMinutos;
    @Column(nullable=false)
    private boolean habilitado;
    @Column(name="fecha_creacion", nullable=false)
    private Instant fechaCreacion;
    @Column(name="fecha_actualizacion", nullable=false)
    private Instant fechaActualizacion;
    public HorarioSemanal() {}
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Profesional getProfesional() { return profesional; }
    public void setProfesional(Profesional value) { this.profesional = value; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DayOfWeek value) { this.diaSemana = value; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime value) { this.horaInicio = value; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime value) { this.horaFin = value; }
    public int getDuracionTurnoMinutos() { return duracionTurnoMinutos; }
    public void setDuracionTurnoMinutos(int value) { this.duracionTurnoMinutos = value; }
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean value) { this.habilitado = value; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant value) { this.fechaCreacion = value; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Instant value) { this.fechaActualizacion = value; }
}
