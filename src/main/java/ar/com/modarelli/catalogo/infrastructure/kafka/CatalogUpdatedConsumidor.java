package ar.com.modarelli.catalogo.infrastructure.kafka;

import ar.com.modarelli.catalogo.domain.model.EventoCatalogo;
import ar.com.modarelli.catalogo.domain.ports.in.SincronizarCatalogoUseCase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "catalogo.sync.enabled", havingValue = "true")
public class CatalogUpdatedConsumidor {
    private final ObjectMapper json;
    private final SincronizarCatalogoUseCase servicio;

    public CatalogUpdatedConsumidor(ObjectMapper json, SincronizarCatalogoUseCase servicio) {
        this.json =
                json.rebuild()
                        .disable(
                                tools.jackson.databind.DeserializationFeature
                                        .FAIL_ON_UNKNOWN_PROPERTIES)
                        .build();
        this.servicio = servicio;
    }

    @KafkaListener(
            id = "catalogo-catedra",
            topics = "${catalogo.kafka.topic}",
            groupId = "${catalogo.kafka.group-id}",
            containerFactory = "catalogoKafkaFactory",
            autoStartup = "false")
    public void recibir(
            String texto, @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        EventoCatalogo evento;
        try {
            evento = json.readValue(texto, EventoCatalogo.class);
            evento.validar();
            if (!String.valueOf(evento.newVersion()).equals(key))
                throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Aviso de catalogo invalido");
        }
        servicio.procesar(evento);
    }
}
