package ar.com.modarelli.catalogo.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoCatalogo(String eventId, String eventType, Long newVersion, Instant occurredAt, String source, Integer schemaVersion) {
    public void validar() {
        if (eventId == null || eventId.isBlank() || eventId.length() > 200 || !"CatalogUpdated".equals(eventType)
            || newVersion == null || newVersion < 0 || occurredAt == null || !Integer.valueOf(1).equals(schemaVersion)) {
            throw new IllegalArgumentException("Evento de catalogo invalido");
        }
    }
}
