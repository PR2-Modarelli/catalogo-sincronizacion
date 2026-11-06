package ar.com.modarelli.catalogo.service.sync;

import ar.com.modarelli.catalogo.service.dto.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class LecturaIncrementalRedis {
    private static final String PREFIJO="catedra:sync:";
    private final ObjectProvider<LecturaRedis> proveedor;
    private final ObjectMapper json;
    public LecturaIncrementalRedis(ObjectProvider<LecturaRedis> proveedor,ObjectMapper json) {this.proveedor=proveedor;this.json=json;}
    public record Metadata(long actual,long masAntigua,Instant publicada) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Cambios(Long version,Ids changes) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Ids(List<Long> professionalCategories,List<Long> professionals,List<Long> weeklySchedules) {}
    public PlanSincronizacion leer(Long base) {
        LecturaRedis redis=proveedor.getIfAvailable();
        if (redis==null) throw new IllegalStateException("Sincronizacion automatica desactivada");
        for (int intento=0;intento<3;intento++) {
            Metadata antes=metadata(redis);
            if (base==null || base<antes.masAntigua() || antes.actual()<base || antes.actual()-base>1000) return PlanSincronizacion.recuperar(base,antes.actual());
            List<CambioCatalogo> resultado=new ArrayList<>();
            for (long version=base;version<antes.actual();) {
                long anterior=version++;
                String contenido=redis.get(PREFIJO+"changes:"+version);
                if (contenido==null) return PlanSincronizacion.recuperar(base,antes.actual());
                Cambios cambio=json.readValue(contenido,Cambios.class);
                if (cambio==null || !Long.valueOf(version).equals(cambio.version()) || cambio.changes()==null) return PlanSincronizacion.recuperar(base,antes.actual());
                var cs=entidades(redis,"professional-categories",cambio.changes().professionalCategories(),CategoriaDato.class);
                var ps=entidades(redis,"professionals",cambio.changes().professionals(),ProfesionalDato.class);
                var hs=entidades(redis,"weekly-schedules",cambio.changes().weeklySchedules(),HorarioDato.class);
                if (cs==null || ps==null || hs==null) return PlanSincronizacion.recuperar(base,antes.actual());
                resultado.add(new CambioCatalogo(anterior,version,cs,ps,hs));
            }
            if (antes.equals(metadata(redis))) return new PlanSincronizacion(base,antes.actual(),List.copyOf(resultado),false);
        }
        // Un catalogo que cambia continuamente se reconstruye desde una respuesta REST coherente.
        return PlanSincronizacion.recuperar(base,metadata(redis).actual());
    }
    private Metadata metadata(LecturaRedis redis) {
        var m=redis.hgetall(PREFIJO+"metadata");
        long actual=Long.parseLong(m.get("currentVersion")), antigua=Long.parseLong(m.get("oldestAvailableVersion"));
        if (actual<0 || antigua<0 || antigua>actual) throw new IllegalStateException("Metadata invalida");
        return new Metadata(actual,antigua,Instant.parse(m.get("publishedAt")));
    }
    private <T> List<T> entidades(LecturaRedis redis,String hash,List<Long> ids,Class<T> tipo) {
        if (ids==null || ids.stream().anyMatch(Objects::isNull)) throw new IllegalStateException("IDs invalidos");
        List<T> datos=new ArrayList<>();
        for (Long id:new LinkedHashSet<>(ids)) {
            String texto=redis.hget(PREFIJO+hash,id.toString());
            if (texto==null) return null;
            T dato=json.readValue(texto,tipo);
            Long recibido=switch(dato) {
                case CategoriaDato c -> c.id();
                case ProfesionalDato p -> p.id();
                case HorarioDato h -> h.id();
                default -> null;
            };
            if (!id.equals(recibido)) throw new IllegalStateException("ID inconsistente en Redis");
            datos.add(dato);
        }
        return List.copyOf(datos);
    }
}
