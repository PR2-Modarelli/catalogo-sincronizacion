package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import ar.com.modarelli.catalogo.IntegrationTest;
import ar.com.modarelli.catalogo.service.sync.RedisCatalogoCliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@DirtiesContext
@TestPropertySource(properties={
    "catalogo.sync.enabled=true",
    "catalogo.sync.initial-delay-ms=3600000",
    "catalogo.redis.host=localhost",
    "catalogo.redis.username=usuario-prueba",
    "catalogo.redis.password=clave-prueba",
    "catalogo.kafka.bootstrap-servers=localhost:19092",
    "catalogo.kafka.topic=catalogo-prueba",
    "catalogo.kafka.group-id=grupo-prueba",
    "spring.cloud.stream.enabled=false",
    "spring.cloud.function.definition=kafkaConsumer;kafkaProducer"
})
class ArranqueSincronizacionIT {
    @Autowired RedisCatalogoCliente redis;
    @Autowired KafkaListenerEndpointRegistry listeners;
    @Test void configuraIntegracionSinConectarAntesDeLaRevisionInicial() {
        assertThat(redis).isNotNull();
        var consumidor=listeners.getListenerContainer("catalogo-catedra");
        assertThat(consumidor).isNotNull();assertThat(consumidor.isRunning()).isFalse();
    }
}
