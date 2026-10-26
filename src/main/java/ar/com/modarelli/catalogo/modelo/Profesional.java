package ar.com.modarelli.catalogo.modelo;

public class Profesional {

    private Long id;
    private String nombre;
    private String apellido;
    private Categoria categoria;
    private boolean habilitado;

    public Profesional(
        Long id,
        String nombre,
        String apellido,
        Categoria categoria,
        boolean habilitado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.categoria = categoria;
        this.habilitado = habilitado;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public boolean isHabilitado() {
        return habilitado;
    }
}