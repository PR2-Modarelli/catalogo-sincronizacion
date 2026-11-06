package ar.com.modarelli.catalogo.service.sync;

import ar.com.modarelli.catalogo.service.dto.CambioCatalogo;
import java.util.List;

public record PlanSincronizacion(Long base,long destino,List<CambioCatalogo> cambios,boolean requiereSnapshot) {
    public static PlanSincronizacion recuperar(Long base,long destino) {return new PlanSincronizacion(base,destino,List.of(),true);}
}
