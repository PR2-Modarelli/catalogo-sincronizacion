package ar.com.modarelli.catalogo.config;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.*;

@Configuration
@EnableKafka
@ConditionalOnProperty(name="catalogo.sync.enabled",havingValue="true")
public class SincronizacionKafkaConfiguration {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,String> catalogoKafkaFactory(@Value("${catalogo.kafka.bootstrap-servers}") String servidores) {
        if (servidores.isBlank()) throw new IllegalArgumentException("Falta configurar Kafka");
        var props=Map.<String,Object>of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,servidores,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG,1,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,600000,
            ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,false
        );
        var factory=new ConcurrentKafkaListenerContainerFactory<String,String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        // Un fallo conserva el offset pendiente. El chequeo periodico recupera y reinicia el listener.
        factory.setCommonErrorHandler(new CommonContainerStoppingErrorHandler());
        return factory;
    }
}
