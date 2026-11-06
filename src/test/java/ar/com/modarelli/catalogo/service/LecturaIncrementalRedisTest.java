package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import ar.com.modarelli.catalogo.service.sync.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

class LecturaIncrementalRedisTest {
    static class RedisFalso implements LecturaRedis {
        final Map<String,String> cadenas=new HashMap<>();
        final Map<String,String> entidades=new HashMap<>();
        long actual=8,antigua=7; int lecturas=0; boolean cambiar=false;
        public String get(String k) {return cadenas.get(k);}
        public String hget(String k,String f) {return entidades.get(k+":"+f);}
        public Map<String,String> hgetall(String k) {
            lecturas++;
            if (cambiar && lecturas==2) actual=9;
            return Map.of("currentVersion",Long.toString(actual),"oldestAvailableVersion",Long.toString(antigua),"publishedAt","2026-09-10T13:40:00Z");
        }
        void version(long v,String profesionales) {cadenas.put("catedra:sync:changes:"+v,"{\"version\":"+v+",\"changes\":{\"professionalCategories\":[],\"professionals\":"+profesionales+",\"weeklySchedules\":[]}}");}
        void profesional() {entidades.put("catedra:sync:professionals:15","""
            {"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":false,
            "createdAt":"2026-09-10T13:40:00Z","updatedAt":"2026-09-10T13:40:00Z","extra":true}
            """);}
    }
    static LecturaIncrementalRedis lector(RedisFalso redis) {
        var beans=new StaticListableBeanFactory();beans.addBean("redis",redis);
        return new LecturaIncrementalRedis(beans.getBeanProvider(LecturaRedis.class),JsonMapper.builder().findAndAddModules().build());
    }
    @Test void leeIdsDelHistorialYBajasLogicasDesdeLosHashes() {
        var r=new RedisFalso();r.version(8,"[15]");r.profesional();
        var plan=lector(r).leer(7L);
        assertThat(plan.requiereSnapshot()).isFalse();assertThat(plan.destino()).isEqualTo(8);
        assertThat(plan.cambios().getFirst().profesionales().getFirst().enabled()).isFalse();
    }
    @Test void faltaDeHistoriaRecuperaPorSnapshot() {
        var r=new RedisFalso();
        assertThat(lector(r).leer(7L).requiereSnapshot()).isTrue();
        r.antigua=8;
        assertThat(lector(r).leer(7L).requiereSnapshot()).isTrue();
    }
    @Test void entidadAusenteNoSeInterpretaComoBorrado() {
        var r=new RedisFalso();r.version(8,"[15]");
        assertThat(lector(r).leer(7L).requiereSnapshot()).isTrue();
    }
    @Test void versionCambiaDuranteLaLecturaYSeReleeElRangoCompleto() {
        var r=new RedisFalso();r.version(8,"[15]");r.version(9,"[]");r.profesional();r.cambiar=true;
        var plan=lector(r).leer(7L);
        assertThat(plan.destino()).isEqualTo(9);assertThat(plan.cambios()).hasSize(2);assertThat(r.lecturas).isEqualTo(4);
    }
    @Test void inicializacionYVersionCentralMenorPidenSnapshot() {
        var r=new RedisFalso();
        assertThat(lector(r).leer(null).requiereSnapshot()).isTrue();
        assertThat(lector(r).leer(9L).requiereSnapshot()).isTrue();
    }
    @Test void versionAlDiaNoLeeEntidadesNiFabricaCambios() {
        var r=new RedisFalso();r.actual=7;
        assertThat(lector(r).leer(7L).cambios()).isEmpty();
    }
    @Test void idDelHashDebeCoincidirConElContenido() {
        var r=new RedisFalso();r.version(8,"[16]");r.profesional();r.entidades.put("catedra:sync:professionals:16",r.entidades.values().iterator().next());
        assertThatThrownBy(()->lector(r).leer(7L)).isInstanceOf(IllegalStateException.class);
    }
}
