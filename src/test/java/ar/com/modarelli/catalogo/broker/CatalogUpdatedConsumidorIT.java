package ar.com.modarelli.catalogo.broker;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import ar.com.modarelli.catalogo.config.SincronizacionKafkaConfiguration;
import ar.com.modarelli.catalogo.service.SincronizacionIncrementalServicio;
import ar.com.modarelli.catalogo.service.dto.EventoCatalogo;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CatalogUpdatedConsumidorIT {
    static class AplicadorControlado extends SincronizacionIncrementalServicio {
        final AtomicInteger intentos=new AtomicInteger();
        final AtomicBoolean fallar=new AtomicBoolean(true);
        final AtomicLong aplicada=new AtomicLong();
        AplicadorControlado() {super(null,null,null,null);}
        @Override public Long procesar(EventoCatalogo evento) {
            intentos.incrementAndGet();
            if (fallar.get()) throw new IllegalStateException("Fallo de persistencia simulado");
            aplicada.set(evento.newVersion()); return aplicada.get();
        }
    }
    @Test void conservaOffsetTrasFalloYReprocesaAlReiniciarElConsumidor() throws Exception {
        try (var broker=new KafkaContainer("apache/kafka-native:4.3.1")) {
            broker.start();
            String topic="catalogo-prueba", grupo="catalogo-integracion";
            try (var admin=AdminClient.create(Map.of("bootstrap.servers",broker.getBootstrapServers()))) {
                admin.createTopics(List.of(new NewTopic(topic,1,(short)1))).all().get(20,TimeUnit.SECONDS);
                var aplicador=new AplicadorControlado();
                try (var context=new AnnotationConfigApplicationContext()) {
                    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba",Map.of(
                        "catalogo.sync.enabled","true","catalogo.kafka.bootstrap-servers",broker.getBootstrapServers(),
                        "catalogo.kafka.topic",topic,"catalogo.kafka.group-id",grupo)));
                    context.registerBean(ObjectMapper.class,()->JsonMapper.builder().findAndAddModules().build());
                    context.registerBean(SincronizacionIncrementalServicio.class,()->aplicador);
                    context.register(SincronizacionKafkaConfiguration.class,CatalogUpdatedConsumidor.class);
                    context.refresh();
                    var listener=context.getBean(KafkaListenerEndpointRegistry.class).getListenerContainer("catalogo-catedra");
                    assertThat(listener).isNotNull();listener.start();
                    var props=new HashMap<String,Object>();props.put("bootstrap.servers",broker.getBootstrapServers());
                    props.put("key.serializer",StringSerializer.class);props.put("value.serializer",StringSerializer.class);
                    try (var productor=new org.apache.kafka.clients.producer.KafkaProducer<String,String>(props)) {
                        productor.send(new ProducerRecord<>(topic,"8","""
                            {"eventId":"e-8","eventType":"CatalogUpdated","newVersion":8,
                            "occurredAt":"2026-09-10T13:40:00Z","source":"SCHEDULED","schemaVersion":1}
                            """)).get(20,TimeUnit.SECONDS);
                    }
                    await().atMost(Duration.ofSeconds(30)).until(()->aplicador.intentos.get()==1 && !listener.isRunning());
                    var particion=new TopicPartition(topic,0);
                    var antes=admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata().get(10,TimeUnit.SECONDS).get(particion);
                    assertThat(antes==null || antes.offset()==0).isTrue();
                    aplicador.fallar.set(false);listener.start();
                    await().atMost(Duration.ofSeconds(30)).untilAsserted(()->assertThat(aplicador.aplicada.get()).isEqualTo(8));
                    await().atMost(Duration.ofSeconds(20)).untilAsserted(()->{
                        var offset=admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata().get(10,TimeUnit.SECONDS).get(particion);
                        assertThat(offset).isNotNull();assertThat(offset.offset()).isEqualTo(1);
                    });
                    assertThat(aplicador.intentos.get()).isEqualTo(2);
                }
            }
        }
    }
}
