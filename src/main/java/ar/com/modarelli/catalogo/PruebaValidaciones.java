package ar.com.modarelli.catalogo;

import ar.com.modarelli.catalogo.modelo.Categoria;
import ar.com.modarelli.catalogo.modelo.Profesional;
import ar.com.modarelli.catalogo.modelo.HorarioSemanal;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class PruebaValidaciones {

    public static void main(String[] args) {

        Categoria categoria = new Categoria(
            1L,
            "Clínica médica",
            "Atención clínica general",
            true
        );

        Profesional ana = new Profesional(
            15L, "Ana", "Gómez", categoria, true
        );

        System.out.println("Intentamos crear turnos de cero minutos.");

        try {
            new HorarioSemanal(
                21L,
                ana,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                0,
                true
            );

            System.out.println(
                "ERROR: se permitió crear un horario inválido."
            );

        } catch (IllegalArgumentException error) {
            System.out.println("Horario rechazado: " + error.getMessage());
        }

        System.out.println("El programa pudo continuar.");
    }
}