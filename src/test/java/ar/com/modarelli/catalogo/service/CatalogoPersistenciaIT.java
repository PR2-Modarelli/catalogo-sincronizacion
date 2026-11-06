package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import ar.com.modarelli.catalogo.IntegrationTest;
import ar.com.modarelli.catalogo.repository.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "catalogo.catedra.base-url=",
    "catalogo.catedra.token="
})
class CatalogoPersistenciaIT {

 @Autowired SnapshotCatalogoServicio sincronizacion;
 @Autowired ConsultaCatalogoServicio consulta;
 @Autowired CategoriaRepository categorias;
 @Autowired ProfesionalRepository profesionales;
 @Autowired HorarioSemanalRepository horarios;
 @Autowired JdbcTemplate jdbc;
 @Autowired MockMvc mvc;
 static final Instant FECHA=Instant.parse("2026-09-10T13:40:00Z");
 @BeforeEach void limpiar() {
  jdbc.update("delete from horario_semanal"); jdbc.update("delete from profesional"); jdbc.update("delete from categoria");
  jdbc.update("update estado_catalogo set version_aplicada=null, sincronizado_en=null where id=1");
 }
 @AfterEach void limpiarDespues() { limpiar(); }
 static SnapshotCatalogo snapshot(long version) {
  return new SnapshotCatalogo(version,FECHA,List.of(
   new CategoriaDato(1L,"Clinica","Descripcion extensa "+"a".repeat(300),true,FECHA,FECHA),
   new CategoriaDato(2L,"Odontologia",null,false,FECHA,FECHA)), List.of(
   new ProfesionalDato(15L,1L,"Ana","Gomez",true,FECHA,FECHA),
   new ProfesionalDato(16L,1L,"Luis","Perez",false,FECHA,FECHA),
   new ProfesionalDato(17L,2L,"Carla","Lopez",true,FECHA,FECHA)), List.of(
   new HorarioDato(21L,15L,DayOfWeek.MONDAY,LocalTime.of(9,0),LocalTime.of(12,0),30,true,FECHA,FECHA)));
 }
 @Test void conservaIdsRelacionesDeshabilitadosYVersion() {
  assertThat(sincronizacion.aplicar(snapshot(7))).isEqualTo(7L);
  assertThat(categorias.count()).isEqualTo(2); assertThat(profesionales.count()).isEqualTo(3); assertThat(horarios.count()).isEqualTo(1);
  assertThat(categorias.findById(2L).orElseThrow().isHabilitada()).isFalse();
  assertThat(consulta.horarios(15L).getFirst().duracionTurnoMinutos()).isEqualTo(30);
  assertThat(consulta.estado().version()).isEqualTo(7L);
 }
 @Test void snapshotRepetidoNoDuplicaYReemplazoEliminaAusentes() {
  sincronizacion.aplicar(snapshot(7)); sincronizacion.aplicar(snapshot(7));
  assertThat(profesionales.count()).isEqualTo(3);
  sincronizacion.aplicar(new SnapshotCatalogo(8L,FECHA,List.of(),List.of(),List.of()));
  assertThat(profesionales.count()).isZero(); assertThat(categorias.count()).isZero();
  assertThat(consulta.estado().version()).isEqualTo(8L);
 }
 @Test void referenciasInvalidasNoDestruyenLaCopia() {
  sincronizacion.aplicar(snapshot(7));
  var s=snapshot(8);
  var invalido=new SnapshotCatalogo(8L,FECHA,List.of(),s.professionals(),s.weeklySchedules());
  assertThatThrownBy(()->sincronizacion.aplicar(invalido)).isInstanceOf(ResponseStatusException.class);
  assertThat(consulta.estado().version()).isEqualTo(7L); assertThat(profesionales.count()).isEqualTo(3);
 }
 @Test void falloDePersistenciaRevierteBorradosYVersion() {
  sincronizacion.aplicar(snapshot(7));
  // PostgreSQL no admite U+0000 en text: provoca un fallo real despues de borrar, dentro de la transaccion.
  var corrupto=new SnapshotCatalogo(8L,FECHA,List.of(new CategoriaDato(1L,"Clinica","\u0000",true,FECHA,FECHA)),List.of(),List.of());
  assertThatThrownBy(()->sincronizacion.aplicar(corrupto)).isInstanceOf(RuntimeException.class);
  assertThat(consulta.estado().version()).isEqualTo(7L); assertThat(profesionales.count()).isEqualTo(3);
 }
 @Test void versionViejaNoRetrocede() {
  sincronizacion.aplicar(snapshot(7));
  assertThatThrownBy(()->sincronizacion.aplicar(snapshot(6))).isInstanceOf(ResponseStatusException.class);
  assertThat(consulta.estado().version()).isEqualTo(7L);
 }
 @Test void buscaDesdePostgresConFiltrosCombinadosYPaginacion() {
  sincronizacion.aplicar(snapshot(7));
  assertThat(consulta.buscar("gOm",1L,true,true,DayOfWeek.MONDAY,0,20).contenido())
   .extracting(ConsultaCatalogoServicio.ProfesionalVista::id).containsExactly(15L);
  assertThat(consulta.buscar(null,null,false,null,null,0,20).contenido()).extracting(ConsultaCatalogoServicio.ProfesionalVista::id).containsExactly(16L);
  assertThat(consulta.buscar(null,null,null,false,null,0,20).total()).isEqualTo(2);
  assertThat(consulta.buscar("%",null,null,null,null,0,20).total()).isZero();
  assertThat(consulta.buscar(null,null,null,null,null,0,1).contenido()).hasSize(1);
  assertThat(consulta.buscar(null,null,null,null,null,0,1).total()).isEqualTo(3);
  assertThatThrownBy(()->consulta.buscar(null,null,null,null,null,0,101)).isInstanceOf(ResponseStatusException.class);
 }
 @Test void horarioInvalidoYIdsDuplicadosSeRechazan() {
  var s=snapshot(7);
  var horario=new HorarioDato(21L,15L,DayOfWeek.MONDAY,LocalTime.NOON,LocalTime.of(9,0),0,true,FECHA,FECHA);
  assertThatThrownBy(()->sincronizacion.aplicar(new SnapshotCatalogo(7L,FECHA,s.professionalCategories(),s.professionals(),List.of(horario)))).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->sincronizacion.aplicar(new SnapshotCatalogo(7L,FECHA,List.of(s.professionalCategories().getFirst(),s.professionalCategories().getFirst()),List.of(),List.of()))).isInstanceOf(ResponseStatusException.class);
  assertThat(consulta.estado().inicializado()).isFalse();
 }
 @Test void endpointsExigenIdentidadYAdministracion() throws Exception {
  mvc.perform(get("/api/catalogo/estado")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/admin/catalogo/sincronizacion-incremental")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/admin/catalogo/sincronizacion-incremental").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/catalogo/sincronizacion-completa").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))).andExpect(status().isForbidden());
  mvc.perform(get("/api/catalogo/profesionales").with(jwt())).andExpect(status().isServiceUnavailable());
  sincronizacion.aplicar(snapshot(7));
  mvc.perform(get("/api/catalogo/profesionales").param("nombre","Ana").with(jwt()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.contenido[0].id").value(15));
  mvc.perform(get("/api/catalogo/profesionales/999/horarios").with(jwt())).andExpect(status().isNotFound());
  mvc.perform(post("/api/admin/catalogo/sincronizacion-completa").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))).andExpect(status().isServiceUnavailable());
 }
 @Test
void conservaHorasLocalesDeUnaAgendaDeTarde() {
    var datos = snapshot(7);

    var horario = new HorarioDato(
        304L,
        15L,
        DayOfWeek.THURSDAY,
        LocalTime.of(18, 0),
        LocalTime.of(21, 0),
        30,
        true,
        FECHA,
        FECHA
    );

    sincronizacion.aplicar(
        new SnapshotCatalogo(
            7L,
            FECHA,
            datos.professionalCategories(),
            datos.professionals(),
            List.of(horario)
        )
    );

    
    String inicioGuardado = jdbc.queryForObject(
        "select to_char(hora_inicio, 'HH24:MI:SS') "
            + "from horario_semanal where id = 304",
        String.class
    );

    String finGuardado = jdbc.queryForObject(
        "select to_char(hora_fin, 'HH24:MI:SS') "
            + "from horario_semanal where id = 304",
        String.class
    );

    assertThat(inicioGuardado).isEqualTo("18:00:00");
    assertThat(finGuardado).isEqualTo("21:00:00");

    
    var recuperado = consulta.horarios(15L).getFirst();

    assertThat(recuperado.horaInicio()).isEqualTo(LocalTime.of(18, 0));
    assertThat(recuperado.horaFin()).isEqualTo(LocalTime.of(21, 0));
}
}
