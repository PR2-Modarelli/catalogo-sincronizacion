package ar.com.modarelli.catalogo.domain;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="profesional")
public class Profesional {
    @Id
    private Long id;
    @Column(nullable=false)
    private String nombre;
    @Column(nullable=false)
    private String apellido;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="categoria_id", nullable=false)
    private Categoria categoria;
    @Column(nullable=false)
    private boolean habilitado;
    @Column(name="fecha_creacion", nullable=false)
    private Instant fechaCreacion;
    @Column(name="fecha_actualizacion", nullable=false)
    private Instant fechaActualizacion;
    public Profesional() {}
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getNombre() { return nombre; }
    public void setNombre(String value) { this.nombre = value; }
    public String getApellido() { return apellido; }
    public void setApellido(String value) { this.apellido = value; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria value) { this.categoria = value; }
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean value) { this.habilitado = value; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant value) { this.fechaCreacion = value; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Instant value) { this.fechaActualizacion = value; }
}
