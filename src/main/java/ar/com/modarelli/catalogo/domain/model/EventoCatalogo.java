package ar.com.modarelli.catalogo.domain.model;

import java.time.Instant;

public record EventoCatalogo(
        String eventId,
        String eventType,
        Long newVersion,
        Instant occurredAt,
        String source,
        Integer schemaVersion) {
    public void validar() {
        if (eventId == null
                || eventId.isBlank()
                || eventId.length() > 200
                || !"CatalogUpdated".equals(eventType)
                || newVersion == null
                || newVersion < 0
                || occurredAt == null
                || !Integer.valueOf(1).equals(schemaVersion)) {
            throw new IllegalArgumentException("Evento de catalogo invalido");
        }
    }
}
