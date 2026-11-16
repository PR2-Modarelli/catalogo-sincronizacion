package ar.com.modarelli.catalogo.domain.model;

import java.util.List;

public record PlanSincronizacion(
        Long base, long destino, List<CambioCatalogo> cambios, boolean requiereSnapshot) {
    public static PlanSincronizacion recuperar(Long base, long destino) {
        return new PlanSincronizacion(base, destino, List.of(), true);
    }
}
