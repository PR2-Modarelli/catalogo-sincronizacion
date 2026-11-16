package ar.com.modarelli.catalogo.domain.service;

import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo;

import java.time.*;
import java.util.*;

public final class ValidacionCatalogo {
    private static void exigir(boolean condicion) {
        if (!condicion)
            throw new ar.com.modarelli.catalogo.domain.exception.ErrorCatalogo(
                    502, "Snapshot de catedra invalido o incompleto");
    }

    private static void registro(
            Long id, Boolean enabled, Instant creado, Instant actualizado, Set<Long> ids) {
        exigir(id != null && enabled != null && creado != null && actualizado != null);
        exigir(ids.add(id));
    }

    private static boolean texto(String s) {
        return s != null && !s.isBlank() && s.length() <= 255;
    }

    public static void validar(SnapshotCatalogo s) {
        exigir(s != null);
        exigir(s.snapshotVersion() != null && s.snapshotVersion() >= 0 && s.generatedAt() != null);
        exigir(
                s.professionalCategories() != null
                        && s.professionals() != null
                        && s.weeklySchedules() != null);
        Set<Long> cs = new HashSet<>(), ps = new HashSet<>(), hs = new HashSet<>();
        for (var c : s.professionalCategories()) {
            exigir(c != null);
            registro(c.id(), c.enabled(), c.createdAt(), c.updatedAt(), cs);
            exigir(texto(c.name()));
        }
        for (var p : s.professionals()) {
            exigir(p != null);
            registro(p.id(), p.enabled(), p.createdAt(), p.updatedAt(), ps);
            exigir(cs.contains(p.categoryId()) && texto(p.firstName()) && texto(p.lastName()));
        }
        for (var h : s.weeklySchedules()) {
            exigir(h != null);
            registro(h.id(), h.enabled(), h.createdAt(), h.updatedAt(), hs);
            exigir(
                    ps.contains(h.professionalId())
                            && h.dayOfWeek() != null
                            && h.startTime() != null
                            && h.endTime() != null);
            exigir(
                    h.endTime().isAfter(h.startTime())
                            && h.slotDurationMinutes() != null
                            && h.slotDurationMinutes() > 0);
        }
    }
}
