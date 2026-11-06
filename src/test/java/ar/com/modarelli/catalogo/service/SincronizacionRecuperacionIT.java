package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ar.com.modarelli.catalogo.IntegrationTest;
import ar.com.modarelli.catalogo.repository.EventoCatalogoProcesadoRepository;
import ar.com.modarelli.catalogo.service.dto.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import ar.com.modarelli.catalogo.service.sync.LecturaRedis;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@Import(SincronizacionRecuperacionIT.FuenteDePrueba.class)
class SincronizacionRecuperacionIT {
    @TestConfiguration static class FuenteDePrueba {
        @Bean LecturaIncrementalRedisTest.RedisFalso redisFalso() {return new LecturaIncrementalRedisTest.RedisFalso();}
    }
    @Autowired SincronizacionIncrementalServicio servicio;
    @Autowired SnapshotCatalogoServicio snapshots;
    @Autowired ConsultaCatalogoServicio consulta;
    @Autowired EventoCatalogoProcesadoRepository eventos;
    @Autowired LecturaIncrementalRedisTest.RedisFalso redis;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean CatedraSnapshotCliente cliente;
    @BeforeEach void preparar() {limpiar();redis.actual=8;redis.antigua=7;redis.cambiar=false;redis.lecturas=0;redis.cadenas.clear();redis.entidades.clear();}
    @AfterEach void limpiar() {
        jdbc.update("delete from evento_catalogo_procesado");jdbc.update("delete from horario_semanal");jdbc.update("delete from profesional");jdbc.update("delete from categoria");
        jdbc.update("update estado_catalogo set version_aplicada=null,sincronizado_en=null,ultimo_error=null where id=1");
    }
    private EventoCatalogo evento(long v) {return new EventoCatalogo("e-"+v,"CatalogUpdated",v,CatalogoPersistenciaIT.FECHA,"SCHEDULED",1);}
    @Test void inicializaConSnapshotYRegistraElAvisoEnLaMismaTransaccion() {
        when(cliente.descargar()).thenReturn(CatalogoPersistenciaIT.snapshot(8));
        assertThat(servicio.procesar(evento(8))).isEqualTo(8L);
        assertThat(consulta.estado().profesionales()).isEqualTo(3);assertThat(eventos.existsById("e-8")).isTrue();
    }
    @Test void aplicaRangoPendienteConEntidadesVigentesYConfirmaSoloLaVersionFinal() {
        snapshots.aplicar(CatalogoPersistenciaIT.snapshot(7));redis.actual=9;
        redis.version(8,"[15]");redis.version(9,"[15]");redis.profesional();
        assertThat(servicio.procesar(evento(8))).isEqualTo(9L);
        assertThat(consulta.estado().version()).isEqualTo(9L);
        assertThat(jdbc.queryForObject("select habilitado from profesional where id=15",Boolean.class)).isFalse();
        assertThat(eventos.count()).isEqualTo(1);verifyNoInteractions(cliente);
    }
    @Test void eventoRepetidoNoNecesitaRedisNiSnapshot() {
        snapshots.aplicar(CatalogoPersistenciaIT.snapshot(7));redis.version(8,"[15]");redis.profesional();
        servicio.procesar(evento(8));redis.cadenas.clear();redis.entidades.clear();
        assertThat(servicio.procesar(evento(8))).isEqualTo(8L);assertThat(eventos.count()).isEqualTo(1);verifyNoInteractions(cliente);
    }
    @Test void huecoDeHistorialRecuperaElSnapshot() {
        snapshots.aplicar(CatalogoPersistenciaIT.snapshot(7));redis.actual=9;redis.antigua=8;
        when(cliente.descargar()).thenReturn(CatalogoPersistenciaIT.snapshot(9));
        assertThat(servicio.sincronizar()).isEqualTo(9L);assertThat(consulta.estado().categorias()).isEqualTo(2);
    }
    @Test void snapshotAtrasadoNoConfirmaElEventoYPermiteReintentar() {
        snapshots.aplicar(CatalogoPersistenciaIT.snapshot(7));
        when(cliente.descargar()).thenReturn(CatalogoPersistenciaIT.snapshot(7));
        assertThatThrownBy(()->servicio.procesar(evento(8))).isInstanceOf(ResponseStatusException.class);
        assertThat(eventos.count()).isZero();assertThat(consulta.estado().version()).isEqualTo(7L);
        assertThat(consulta.estado().ultimoError()).isEqualTo("SINCRONIZACION_NO_DISPONIBLE");
        when(cliente.descargar()).thenReturn(CatalogoPersistenciaIT.snapshot(8));
        assertThat(servicio.procesar(evento(8))).isEqualTo(8L);assertThat(consulta.estado().ultimoError()).isNull();
    }
    @Test void avisoTardioSoloSeRegistraSiLaVersionYaFueAplicada() {
        snapshots.aplicar(CatalogoPersistenciaIT.snapshot(9));
        assertThat(servicio.procesar(evento(8))).isEqualTo(9L);assertThat(eventos.existsById("e-8")).isTrue();verifyNoInteractions(cliente);
    }
    @Test void falloDentroDelRangoRevierteEntidadesVersionYEvento() {
        snapshots.aplicar(CatalogoPersistenciaIT.snapshot(7));redis.actual=9;
        redis.cadenas.put("catedra:sync:changes:8","{\"version\":8,\"changes\":{\"professionalCategories\":[1],\"professionals\":[],\"weeklySchedules\":[]}}");
        redis.version(9,"[15]");
        redis.entidades.put("catedra:sync:professional-categories:1",json.writeValueAsString(new CategoriaDato(1L,"Transitorio",null,true,CatalogoPersistenciaIT.FECHA,CatalogoPersistenciaIT.FECHA)));
        redis.entidades.put("catedra:sync:professionals:15",json.writeValueAsString(new ProfesionalDato(15L,1L,"Ana","\u0000",true,CatalogoPersistenciaIT.FECHA,CatalogoPersistenciaIT.FECHA)));
        assertThatThrownBy(()->servicio.procesar(evento(9))).isInstanceOf(ResponseStatusException.class);
        assertThat(consulta.estado().version()).isEqualTo(7L);assertThat(eventos.count()).isZero();
        assertThat(jdbc.queryForObject("select nombre from categoria where id=1",String.class)).isEqualTo("Clinica");
    }
    @Test void falloDeRedNoFiltraLaCausaNiDestruyeLaCopia() {
        snapshots.aplicar(CatalogoPersistenciaIT.snapshot(7));when(cliente.descargar()).thenThrow(new IllegalStateException("secreto-externo"));
        assertThatThrownBy(()->servicio.sincronizar()).hasMessageNotContaining("secreto-externo");
        assertThat(consulta.estado().version()).isEqualTo(7L);assertThat(consulta.estado().profesionales()).isEqualTo(3);
    }
}
