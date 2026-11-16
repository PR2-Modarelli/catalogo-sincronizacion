package ar.com.modarelli.catalogo.domain.service;

import ar.com.modarelli.catalogo.domain.model.*;

import java.util.*;

public final class ValidacionIncremental {
    public static void validarCabecera(CambioCatalogo cambio, EventoCatalogo evento) {
        if (evento == null) throw new IllegalArgumentException("Falta el evento");
        evento.validar();
        if (cambio == null || !Objects.equals(cambio.versionDestino(), evento.newVersion()))
            throw new IllegalArgumentException("Version de evento distinta");
        validarCambio(cambio);
    }

    public static void validarCambio(CambioCatalogo cambio) {
        if (cambio == null
                || cambio.versionBase() == null
                || cambio.versionDestino() == null
                || cambio.versionBase() < 0
                || cambio.versionBase() == Long.MAX_VALUE
                || cambio.versionDestino() != cambio.versionBase() + 1
                || cambio.categorias() == null
                || cambio.profesionales() == null
                || cambio.horarios() == null) {
            throw new IllegalArgumentException("Cambio de catalogo invalido o no consecutivo");
        }
        unicos(cambio.categorias().stream().map(c -> c == null ? null : c.id()).toList());
        unicos(cambio.profesionales().stream().map(p -> p == null ? null : p.id()).toList());
        unicos(cambio.horarios().stream().map(h -> h == null ? null : h.id()).toList());
    }

    private static void unicos(List<Long> ids) {
        if (ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Entidades nulas o IDs duplicados en el cambio");
        }
    }
}
