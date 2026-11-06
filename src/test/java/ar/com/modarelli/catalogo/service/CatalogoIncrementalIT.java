package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import ar.com.modarelli.catalogo.IntegrationTest;
import ar.com.modarelli.catalogo.repository.*;
import ar.com.modarelli.catalogo.service.dto.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties={"catalogo.catedra.base-url=","catalogo.catedra.token="})
class CatalogoIncrementalIT {
    @Autowired SnapshotCatalogoServicio snapshots;
    @Autowired PersistenciaIncrementalServicio incremental;
    @Autowired ConsultaCatalogoServicio consultas;
    @Autowired ProfesionalRepository profesionales;
    @Autowired CategoriaRepository categorias;
    @Autowired HorarioSemanalRepository horarios;
    @Autowired EventoCatalogoProcesadoRepository eventos;
    @Autowired JdbcTemplate jdbc;
    private static final Instant FECHA=Instant.parse("2026-09-10T13:40:00Z");
    @BeforeEach void preparar() { limpiar(); snapshots.aplicar(CatalogoPersistenciaIT.snapshot(7)); }
    @AfterEach void limpiar() {
        jdbc.update("delete from evento_catalogo_procesado");
        jdbc.update("delete from horario_semanal"); jdbc.update("delete from profesional"); jdbc.update("delete from categoria");
        jdbc.update("update estado_catalogo set version_aplicada=null, sincronizado_en=null where id=1");
    }
    private EventoCatalogo evento(String id,long version) {return new EventoCatalogo(id,"CatalogUpdated",version,FECHA,"SCHEDULED",1);}
    private CambioCatalogo cambioProfesional(long base,boolean habilitado) {
        return new CambioCatalogo(base,base+1,List.of(),List.of(new ProfesionalDato(15L,1L,"Ana","Gomez",habilitado,FECHA,FECHA)),List.of());
    }
    @Test void actualizaUnProfesionalSinBorrarLosDemas() {
        assertThat(incremental.aplicar(cambioProfesional(7,false),evento("evento-8",8))).isEqualTo(8L);
        assertThat(jdbc.queryForObject("select habilitado from profesional where id=15",Boolean.class)).isFalse();
        assertThat(profesionales.count()).isEqualTo(3);
        assertThat(categorias.count()).isEqualTo(2);
        assertThat(horarios.count()).isEqualTo(1);
        assertThat(eventos.count()).isEqualTo(1);
    }
    @Test void eventoDuplicadoNoVuelveAAplicarSusDatos() {
        var e=evento("evento-8",8);
        incremental.aplicar(cambioProfesional(7,false),e);
        // Un segundo payload con el mismo eventId no debe producir efectos nuevos.
        assertThat(incremental.aplicar(cambioProfesional(7,true),e)).isEqualTo(8L);
        assertThat(jdbc.queryForObject("select habilitado from profesional where id=15",Boolean.class)).isFalse();
        assertThat(eventos.count()).isEqualTo(1);
    }
    @Test void insertaPadresEHijosYConservaHorasLocales() {
        var c=new CategoriaDato(3L,"Neurologia",null,true,FECHA,FECHA);
        var p=new ProfesionalDato(18L,3L,"Rita","Diaz",true,FECHA,FECHA);
        var h=new HorarioDato(22L,18L,DayOfWeek.THURSDAY,LocalTime.of(18,0),LocalTime.of(21,0),30,true,FECHA,FECHA);
        incremental.aplicar(new CambioCatalogo(7L,8L,List.of(c),List.of(p),List.of(h)),evento("evento-8",8));
        assertThat(categorias.count()).isEqualTo(3); assertThat(profesionales.count()).isEqualTo(4);
        assertThat(consultas.horarios(18L).getFirst().horaInicio()).isEqualTo(LocalTime.of(18,0));
        assertThat(jdbc.queryForObject("select to_char(hora_fin,'HH24:MI:SS') from horario_semanal where id=22",String.class)).isEqualTo("21:00:00");
    }
    @Test void versionInesperadaNoModificaDatosNiRegistraEvento() {
        assertThatThrownBy(()->incremental.aplicar(cambioProfesional(8,false),evento("evento-9",9))).isInstanceOf(ConcurrentModificationException.class);
        assertThat(consultas.estado().version()).isEqualTo(7L);
        assertThat(eventos.count()).isZero();
        assertThat(jdbc.queryForObject("select habilitado from profesional where id=15",Boolean.class)).isTrue();
    }
    @Test void referenciaInvalidaConservaVersionYCopia() {
        var p=new ProfesionalDato(15L,999L,"Ana","Gomez",true,FECHA,FECHA);
        assertThatThrownBy(()->incremental.aplicar(new CambioCatalogo(7L,8L,List.of(),List.of(p),List.of()),evento("evento-8",8))).isInstanceOf(RuntimeException.class);
        assertThat(consultas.estado().version()).isEqualTo(7L); assertThat(eventos.count()).isZero();
        assertThat(jdbc.queryForObject("select categoria_id from profesional where id=15",Long.class)).isEqualTo(1L);
    }
    @Test void falloRealDeEscrituraRevierteTodoYPermiteReintentarElEvento() {
        var c=new CategoriaDato(1L,"Nombre transitorio",null,true,FECHA,FECHA);
        var corrupto=new ProfesionalDato(15L,1L,"Ana","\u0000",false,FECHA,FECHA);
        assertThatThrownBy(()->incremental.aplicar(new CambioCatalogo(7L,8L,List.of(c),List.of(corrupto),List.of()),evento("evento-8",8))).isInstanceOf(RuntimeException.class);
        assertThat(consultas.estado().version()).isEqualTo(7L); assertThat(eventos.count()).isZero();
        assertThat(jdbc.queryForObject("select habilitado from profesional where id=15",Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("select nombre from categoria where id=1",String.class)).isEqualTo("Clinica");
        assertThat(incremental.aplicar(cambioProfesional(7,false),evento("evento-8",8))).isEqualTo(8L);
    }
    @Test void eventIdReutilizadoConOtraVersionSeRechaza() {
        incremental.aplicar(cambioProfesional(7,false),evento("evento",8));
        assertThatThrownBy(()->incremental.aplicar(cambioProfesional(8,true),evento("evento",9))).isInstanceOf(IllegalArgumentException.class);
        assertThat(consultas.estado().version()).isEqualTo(8L); assertThat(eventos.count()).isEqualTo(1);
    }
    @Test void versionSinEntidadesAfectadasTambienSeRegistra() {
        incremental.aplicar(new CambioCatalogo(7L,8L,List.of(),List.of(),List.of()),evento("evento-8",8));
        assertThat(consultas.estado().version()).isEqualTo(8L); assertThat(eventos.count()).isEqualTo(1);
        assertThat(profesionales.count()).isEqualTo(3);
    }
}
