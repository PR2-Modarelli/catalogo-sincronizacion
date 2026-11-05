package ar.com.modarelli.catalogo.service.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.*;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown=true)
public record SnapshotCatalogo(Long snapshotVersion, Instant generatedAt,
 List<CategoriaDato> professionalCategories, List<ProfesionalDato> professionals,
 List<HorarioDato> weeklySchedules) {
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record CategoriaDato(Long id, String name, String description, Boolean enabled, Instant createdAt, Instant updatedAt) {}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record ProfesionalDato(Long id, Long categoryId, String firstName, String lastName, Boolean enabled, Instant createdAt, Instant updatedAt) {}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record HorarioDato(Long id, Long professionalId, DayOfWeek dayOfWeek, LocalTime startTime,
  LocalTime endTime, Integer slotDurationMinutes, Boolean enabled, Instant createdAt, Instant updatedAt) {}
}
