package ar.com.modarelli.catalogo.service.sync;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="catalogo.sync.enabled",havingValue="true")
public class RedisCatalogoCliente implements LecturaRedis {
    private final RedisClient cliente;
    private StatefulRedisConnection<String,String> conexion;
    public RedisCatalogoCliente(@Value("${catalogo.redis.host}") String host,@Value("${catalogo.redis.port:6379}") int puerto,
        @Value("${catalogo.redis.username}") String usuario,@Value("${catalogo.redis.password}") String clave) {
        if (host.isBlank() || usuario.isBlank() || clave.isBlank()) throw new IllegalArgumentException("Falta configurar Redis");
        var uri=RedisURI.Builder.redis(host,puerto).withAuthentication(usuario,clave.toCharArray()).withTimeout(Duration.ofSeconds(5)).build();
        cliente=RedisClient.create(uri);
    }
    private synchronized StatefulRedisConnection<String,String> conexion() {
        if (conexion==null || !conexion.isOpen()) conexion=cliente.connect();
        return conexion;
    }
    private void permitir(String clave) {
        if (!clave.startsWith("catedra:sync:")) throw new IllegalArgumentException("Clave fuera del catalogo");
    }
    public String get(String clave) {permitir(clave);return conexion().sync().get(clave);}
    public String hget(String clave,String campo) {permitir(clave);return conexion().sync().hget(clave,campo);}
    public Map<String,String> hgetall(String clave) {permitir(clave);return conexion().sync().hgetall(clave);}
    @PreDestroy public synchronized void cerrar() {if(conexion!=null) conexion.close();cliente.shutdown();}
}
