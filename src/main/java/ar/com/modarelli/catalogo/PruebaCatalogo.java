package ar.com.modarelli.catalogo;

import ar.com.modarelli.catalogo.modelo.Categoria;
import ar.com.modarelli.catalogo.modelo.Profesional;
import ar.com.modarelli.catalogo.modelo.HorarioSemanal;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class PruebaCatalogo {

    public static void main(String[] args) {

        // Creamos una categoría.
        Categoria categoria = new Categoria(
            1L,
            "Clínica médica",
            "Atención clínica general",
            true
        );

        // Creamos dos profesionales de esa categoría.
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

        // Creamos un horario semanal para Ana.
        HorarioSemanal horario = new HorarioSemanal(
            21L,
            profesional1,
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            30,
            true
        );

        // Mostramos los datos del horario.
        System.out.println(
            "Horario de: " + horario.getProfesional().getNombre()
        );
        System.out.println("Día: " + horario.getDiaSemana());
        System.out.println("Desde: " + horario.getHoraInicio());
        System.out.println("Hasta: " + horario.getHoraFin());
        System.out.println(
            "Duración por turno: "
            + horario.getDuracionTurnoMinutos()
            + " minutos"
        );
        System.out.println(
            "Horario habilitado: " + horario.isHabilitado()
        );
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