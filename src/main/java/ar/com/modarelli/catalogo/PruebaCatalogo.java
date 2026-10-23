package ar.com.modarelli.catalogo;

import ar.com.modarelli.catalogo.modelo.Categoria;

public class PruebaCatalogo {

    public static void main(String[] args) {
        Categoria categoria = new Categoria(
            1L,
            "Clínica médica",
            "Atención clínica general",
            true
        );

        System.out.println("ID: " + categoria.getId());
        System.out.println("Nombre: " + categoria.getNombre());
        System.out.println("Descripción: " + categoria.getDescripcion());
        System.out.println("Habilitada: " + categoria.isHabilitada());
    }
}