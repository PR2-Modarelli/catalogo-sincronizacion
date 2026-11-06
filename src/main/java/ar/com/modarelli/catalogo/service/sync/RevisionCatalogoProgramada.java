package ar.com.modarelli.catalogo.service.sync;

import ar.com.modarelli.catalogo.service.SincronizacionIncrementalServicio;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="catalogo.sync.enabled",havingValue="true")
public class RevisionCatalogoProgramada {
    private static final Logger LOG=LoggerFactory.getLogger(RevisionCatalogoProgramada.class);
    private final SincronizacionIncrementalServicio servicio;
    private final KafkaListenerEndpointRegistry listeners;
    public RevisionCatalogoProgramada(SincronizacionIncrementalServicio servicio,KafkaListenerEndpointRegistry listeners) {this.servicio=servicio;this.listeners=listeners;}
    @Scheduled(initialDelayString="${catalogo.sync.initial-delay-ms:5000}",fixedDelayString="${catalogo.sync.interval-ms:60000}")
    public void revisar() {
        try {
            servicio.sincronizar();
            var listener=listeners.getListenerContainer("catalogo-catedra");
            if (listener!=null && !listener.isRunning()) listener.start();
        } catch (RuntimeException e) {
            LOG.warn("No se pudo actualizar el catalogo; se conserva la copia local y se revisara en el proximo intervalo");
        }
    }
}
