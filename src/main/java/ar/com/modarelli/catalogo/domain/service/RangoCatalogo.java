package ar.com.modarelli.catalogo.domain.service;

import static ar.com.modarelli.catalogo.domain.service.ValidacionIncremental.*;

import ar.com.modarelli.catalogo.domain.model.*;
import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo.*;

import java.util.*;

public final class RangoCatalogo {
    public static CambioCatalogo unir(List<CambioCatalogo> cambios, EventoCatalogo evento) {
        if (cambios == null || cambios.isEmpty() || cambios.getFirst() == null)
            throw new IllegalArgumentException("Rango vacio");
        Long base = cambios.getFirst().versionBase();
        Long siguiente = base;
        Map<Long, CategoriaDato> cs = new LinkedHashMap<>();
        Map<Long, ProfesionalDato> ps = new LinkedHashMap<>();
        Map<Long, HorarioDato> hs = new LinkedHashMap<>();
        for (var cambio : cambios) {
            validarCambio(cambio);
            if (!Objects.equals(cambio.versionBase(), siguiente))
                throw new IllegalArgumentException("Rango discontinuo");
            siguiente = cambio.versionDestino();
            for (var c : cambio.categorias()) cs.put(c.id(), c);
            for (var p : cambio.profesionales()) ps.put(p.id(), p);
            for (var h : cambio.horarios()) hs.put(h.id(), h);
        }
        if (evento != null) {
            evento.validar();
            if (evento.newVersion() > siguiente)
                throw new IllegalArgumentException("Evento posterior al rango");
        }
        return new CambioCatalogo(
                base,
                siguiente,
                List.copyOf(cs.values()),
                List.copyOf(ps.values()),
                List.copyOf(hs.values()));
    }
}
