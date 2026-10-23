package ar.com.modarelli.catalogo.modelo;

public class Categoria {

    private Long id;
    private String nombre;
    private String descripcion;
    private boolean habilitada;

    public Categoria(Long id, String nombre, String descripcion, boolean habilitada) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.habilitada = habilitada;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isHabilitada() {
        return habilitada;
    }
}