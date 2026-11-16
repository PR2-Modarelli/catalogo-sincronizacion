package ar.com.modarelli.catalogo.domain.model;

import java.time.*;
import java.util.*;

public final class CatalogoVistas {
    public record ProfesionalVista(
            Long id,
            String nombre,
            String apellido,
            Long categoriaId,
            String categoria,
            boolean habilitado) {}

    public record HorarioVista(
            Long id,
            Long profesionalId,
            DayOfWeek diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            int duracionTurnoMinutos,
            boolean habilitado) {}

    public record CategoriaVista(Long id, String nombre, String descripcion, boolean habilitada) {}

    public record EstadoVista(
            Long version,
            Instant sincronizadoEn,
            boolean inicializado,
            long categorias,
            long profesionales,
            long horarios,
            String ultimoError) {}

    public record PaginaProfesionales(
            List<ProfesionalVista> contenido, long total, int pagina, int tamanio) {}

    public record AgendaVista(
            Long version,
            ProfesionalVista profesional,
            boolean categoriaHabilitada,
            List<HorarioVista> horarios) {}
}
