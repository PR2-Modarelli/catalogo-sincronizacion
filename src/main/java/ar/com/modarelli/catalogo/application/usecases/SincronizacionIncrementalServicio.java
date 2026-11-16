package ar.com.modarelli.catalogo.application.usecases;

import ar.com.modarelli.catalogo.domain.model.EventoCatalogo;
import ar.com.modarelli.catalogo.domain.ports.in.AplicarSnapshotUseCase;
import ar.com.modarelli.catalogo.domain.ports.out.DescargarSnapshotPort;
import ar.com.modarelli.catalogo.domain.ports.out.PersistenciaCatalogoPort;
import ar.com.modarelli.catalogo.domain.ports.out.PlanSincronizacionPort;

import org.springframework.stereotype.Service;

import java.util.ConcurrentModificationException;

@Service
public class SincronizacionIncrementalServicio
        implements ar.com.modarelli.catalogo.domain.ports.in.SincronizarCatalogoUseCase {
    private final PlanSincronizacionPort redis;
    private final PersistenciaCatalogoPort persistencia;
    private final DescargarSnapshotPort cliente;
    private final AplicarSnapshotUseCase snapshots;

    public SincronizacionIncrementalServicio(
            PlanSincronizacionPort r,
            PersistenciaCatalogoPort p,
            DescargarSnapshotPort c,
            AplicarSnapshotUseCase s) {
        redis = r;
        persistencia = p;
        cliente = c;
        snapshots = s;
    }

    public Long completa() {
        return snapshots.aplicar(cliente.descargar());
    }

    public Long sincronizar() {
        return ejecutar(null);
    }

    public Long procesar(EventoCatalogo evento) {
        evento.validar();
        return ejecutar(evento);
    }

    private Long ejecutar(EventoCatalogo evento) {
        try {
            if (evento != null && persistencia.procesado(evento)) return persistencia.version();
            for (int intento = 0; intento < 3; intento++) {
                Long base = persistencia.version();
                if (evento != null && base != null && base >= evento.newVersion())
                    return persistencia.confirmar(evento);
                var plan = redis.leer(base);
                long minimo =
                        evento == null
                                ? plan.destino()
                                : Math.max(plan.destino(), evento.newVersion());
                if (plan.requiereSnapshot() || plan.destino() < minimo)
                    return persistencia.recuperar(cliente.descargar(), evento, minimo, snapshots);
                if (plan.cambios().isEmpty()) return persistencia.confirmar(evento);
                try {
                    return persistencia.aplicarRango(plan.cambios(), evento);
                } catch (ConcurrentModificationException e) {
                    /* Releer si otro sincronizador guardo primero. */
                }
            }
            throw new IllegalStateException("Sincronizacion concurrente");
        } catch (RuntimeException e) {
            persistencia.registrarError();
            throw new ar.com.modarelli.catalogo.domain.exception.ErrorCatalogo(
                    503, "No se pudo sincronizar el catalogo");
        }
    }
}
