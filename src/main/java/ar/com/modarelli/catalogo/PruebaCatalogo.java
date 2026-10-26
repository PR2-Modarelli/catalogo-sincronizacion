package ar.com.modarelli.catalogo;

import ar.com.modarelli.catalogo.modelo.Categoria;
import ar.com.modarelli.catalogo.modelo.Profesional;
import ar.com.modarelli.catalogo.modelo.HorarioSemanal;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PruebaCatalogo {

    public static void main(String[] args) {

        Categoria clinica = new Categoria(
            1L,
            "Clínica médica",
            "Atención clínica general",
            true
        );

        Categoria odontologia = new Categoria(
            2L,
            "Odontología",
            "Atención odontológica general",
            true
        );

        Profesional ana = new Profesional(
            15L, "Ana", "Gómez", clinica, true
        );

        Profesional luis = new Profesional(
            16L, "Luis", "Pérez", clinica, false
        );

        Profesional carla = new Profesional(
            17L, "Carla", "López", odontologia, true
        );

        List<Profesional> profesionales = List.of(ana, luis, carla);

        System.out.println(
            "Cantidad total de profesionales: " + profesionales.size()
        );

        mostrarResultados(
            "1. Nombre Ana, sin otros filtros",
            buscarProfesionales(profesionales, "Ana", null, null)
        );

        mostrarResultados(
            "2. Categoría Odontología, sin otros filtros",
            buscarProfesionales(profesionales, null, 2L, null)
        );

        mostrarResultados(
            "3. Solo profesionales deshabilitados",
            buscarProfesionales(profesionales, null, null, false)
        );

        mostrarResultados(
            "4. Ana, de Clínica médica y habilitada",
            buscarProfesionales(profesionales, "Ana", 1L, true)
        );

        mostrarResultados(
            "5. Ana, de Odontología y habilitada",
            buscarProfesionales(profesionales, "Ana", 2L, true)
        );

        HorarioSemanal horario = new HorarioSemanal(
            21L,
            ana,
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            30,
            true
        );

        System.out.println("\n--- Ejemplo de horario semanal ---");
        System.out.println(
            "Profesional: " + horario.getProfesional().getNombre()
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

    public static List<Profesional> buscarProfesionales(
        List<Profesional> profesionales,
        String nombre,
        Long categoriaId,
        Boolean habilitado
    ) {
        List<Profesional> resultados = new ArrayList<>();

        for (Profesional profesional : profesionales) {

            boolean coincideNombre = nombre == null
                || profesional.getNombre().equalsIgnoreCase(nombre);

            boolean coincideCategoria = categoriaId == null
                || profesional.getCategoria().getId().equals(categoriaId);

            boolean coincideEstado = habilitado == null
                || profesional.isHabilitado() == habilitado;

            if (coincideNombre && coincideCategoria && coincideEstado) {
                resultados.add(profesional);
            }
        }

        return resultados;
    }

    public static void mostrarResultados(
        String titulo,
        List<Profesional> resultados
    ) {
        System.out.println("\n--- " + titulo + " ---");

        if (resultados.isEmpty()) {
            System.out.println(
                "No se encontraron profesionales con esos filtros."
            );
        } else {
            for (Profesional profesional : resultados) {
                mostrarProfesional(profesional);
            }
        }
    }

    public static void mostrarProfesional(Profesional profesional) {
        System.out.println(
            "Profesional: " + profesional.getNombre()
            + " " + profesional.getApellido()
        );
        System.out.println(
            "Categoría: " + profesional.getCategoria().getNombre()
        );
        System.out.println(
            "Habilitado: " + profesional.isHabilitado()
        );
    }
}