package ar.com.modarelli.catalogo;

import ar.com.modarelli.catalogo.modelo.Categoria;
import ar.com.modarelli.catalogo.modelo.Profesional;

public class PruebaCatalogo {

    public static void main(String[] args) {
        Categoria categoria = new Categoria(
            1L,
            "Clínica médica",
            "Atención clínica general",
            true
        );

        Profesional profesional1 = new Profesional(
            15L,
            "Ana",
            "Gómez",
            categoria,
            true
        );

        Profesional profesional2 = new Profesional(
            16L,
            "Luis",
            "Pérez",
            categoria,
            false
        );

        mostrarProfesional(profesional1);
        mostrarProfesional(profesional2);
    }

    public static void mostrarProfesional(Profesional profesional) {
        System.out.println(
            "Profesional: " + profesional.getNombre()
            + " " + profesional.getApellido()
        );
        System.out.println(
            "Categoría: " + profesional.getCategoria().getNombre()
        );
        System.out.println("Habilitado: " + profesional.isHabilitado());
        System.out.println();
    }
}