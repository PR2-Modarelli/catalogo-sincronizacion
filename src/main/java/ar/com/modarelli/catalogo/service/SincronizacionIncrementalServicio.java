package ar.com.modarelli.catalogo.service;

import ar.com.modarelli.catalogo.service.dto.EventoCatalogo;
import ar.com.modarelli.catalogo.service.sync.LecturaIncrementalRedis;
import java.util.ConcurrentModificationException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SincronizacionIncrementalServicio {
    private final LecturaIncrementalRedis redis;
    private final PersistenciaIncrementalServicio persistencia;
    private final CatedraSnapshotCliente cliente;
    private final SnapshotCatalogoServicio snapshots;
    public SincronizacionIncrementalServicio(LecturaIncrementalRedis r,PersistenciaIncrementalServicio p,CatedraSnapshotCliente c,SnapshotCatalogoServicio s) {redis=r;persistencia=p;cliente=c;snapshots=s;}
    public Long sincronizar() {return ejecutar(null);}
    public Long procesar(EventoCatalogo evento) {evento.validar();return ejecutar(evento);}
    private Long ejecutar(EventoCatalogo evento) {
        try {
            if (evento!=null && persistencia.procesado(evento)) return persistencia.version();
            for (int intento=0;intento<3;intento++) {
                Long base=persistencia.version();
                if (evento!=null && base!=null && base>=evento.newVersion()) return persistencia.confirmar(evento);
                var plan=redis.leer(base);
                long minimo=evento==null ? plan.destino() : Math.max(plan.destino(),evento.newVersion());
                if (plan.requiereSnapshot() || plan.destino()<minimo) return persistencia.recuperar(cliente.descargar(),evento,minimo,snapshots);
                if (plan.cambios().isEmpty()) return persistencia.confirmar(evento);
                try {return persistencia.aplicarRango(plan.cambios(),evento);}
                catch (ConcurrentModificationException e) { /* Releer si otro sincronizador guardo primero. */ }
            }
            throw new IllegalStateException("Sincronizacion concurrente");
        } catch (RuntimeException e) {
            persistencia.registrarError();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo sincronizar el catalogo");
        }
    }
}
