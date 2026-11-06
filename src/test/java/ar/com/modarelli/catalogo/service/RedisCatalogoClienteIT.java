package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import ar.com.modarelli.catalogo.service.sync.RedisCatalogoCliente;
import io.lettuce.core.*;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class RedisCatalogoClienteIT {
    @Test void autenticaLeeContratoYBloqueaClavesAjenas() {
        try (var redis=new GenericContainer<>("redis:7-alpine").withExposedPorts(6379).withCommand("redis-server","--requirepass","clave-de-prueba")) {
            redis.start();
            var uri=RedisURI.Builder.redis(redis.getHost(),redis.getMappedPort(6379)).withPassword("clave-de-prueba".toCharArray()).withTimeout(Duration.ofSeconds(5)).build();
            var semilla=RedisClient.create(uri);
            try (var conexion=semilla.connect()) {
                conexion.sync().set("catedra:sync:current-version","8");
                conexion.sync().hset("catedra:sync:metadata",Map.of("currentVersion","8"));
                var cliente=new RedisCatalogoCliente(redis.getHost(),redis.getMappedPort(6379),"default","clave-de-prueba");
                try {
                    assertThat(cliente.get("catedra:sync:current-version")).isEqualTo("8");
                    assertThat(cliente.hget("catedra:sync:metadata","currentVersion")).isEqualTo("8");
                    assertThat(cliente.hgetall("catedra:sync:metadata")).containsEntry("currentVersion","8");
                    assertThatThrownBy(()->cliente.get("catedra:holds:1")).isInstanceOf(IllegalArgumentException.class);
                } finally {cliente.cerrar();}
            } finally {semilla.shutdown();}
        }
    }
}
