package ar.com.modarelli.catalogo.infrastructure.kafka;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "spring.cloud.stream.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class KafkaProducer implements Supplier<String> {

    @Override
    public String get() {
        return "kafka_producer";
    }
}
