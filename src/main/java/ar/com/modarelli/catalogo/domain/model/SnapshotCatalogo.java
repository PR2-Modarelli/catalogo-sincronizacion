package ar.com.modarelli.catalogo.domain.model;

import java.time.*;
import java.util.List;

public record SnapshotCatalogo(
        Long snapshotVersion,
        Instant generatedAt,
        List<CategoriaDato> professionalCategories,
        List<ProfesionalDato> professionals,
        List<HorarioDato> weeklySchedules) {
    public record CategoriaDato(
            Long id,
            String name,
            String description,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt) {}

    public record ProfesionalDato(
            Long id,
            Long categoryId,
            String firstName,
            String lastName,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt) {}

    public record HorarioDato(
            Long id,
            Long professionalId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Integer slotDurationMinutes,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt) {}
}
