package ar.com.modarelli.catalogo.service.sync;

import java.util.Map;

/** Solo expone las lecturas necesarias del espacio contractual de catalogo. */
public interface LecturaRedis {
    String get(String clave);
    String hget(String clave, String campo);
    Map<String, String> hgetall(String clave);
}
