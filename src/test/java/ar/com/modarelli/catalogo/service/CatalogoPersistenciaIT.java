package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.modarelli.catalogo.IntegrationTest;
import ar.com.modarelli.catalogo.domain.exception.ErrorCatalogo;
import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo;
import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo.*;
import ar.com.modarelli.catalogo.infrastructure.persistence.adapter.JpaConsultaCatalogoAdapter;
import ar.com.modarelli.catalogo.infrastructure.persistence.adapter.JpaSnapshotCatalogoAdapter;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.CategoriaRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.HorarioSemanalRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.ProfesionalRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;

@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"catalogo.catedra.base-url=", "catalogo.catedra.token="})
class CatalogoPersistenciaIT {

    @Autowired JpaSnapshotCatalogoAdapter sincronizacion;
    @Autowired JpaConsultaCatalogoAdapter consulta;
    @Autowired CategoriaRepository categorias;
    @Autowired ProfesionalRepository profesionales;
    @Autowired HorarioSemanalRepository horarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager gestor;
    @Autowired MockMvc mvc;
    static final Instant FECHA = Instant.parse("2026-09-10T13:40:00Z");

    @BeforeEach
    void limpiar() {
        new org.springframework.transaction.support.TransactionTemplate(gestor)
                .executeWithoutResult(
                        tx -> {
                            jdbc.update("delete from horario_semanal");
                            jdbc.update("delete from profesional");
                            jdbc.update("delete from categoria");
                            jdbc.update(
                                    "update estado_catalogo set version_aplicada=null,"
                                            + " sincronizado_en=null where id=1");
                        });
    }

    @AfterEach
    void limpiarDespues() {
        limpiar();
    }

    static SnapshotCatalogo snapshot(long version) {
        return new SnapshotCatalogo(
                version,
                FECHA,
                List.of(
                        new CategoriaDato(
                                1L,
                                "Clinica",
                                "Descripcion extensa " + "a".repeat(300),
                                true,
                                FECHA,
                                FECHA),
                        new CategoriaDato(2L, "Odontologia", null, false, FECHA, FECHA)),
                List.of(
                        new ProfesionalDato(15L, 1L, "Ana", "Gomez", true, FECHA, FECHA),
                        new ProfesionalDato(16L, 1L, "Luis", "Perez", false, FECHA, FECHA),
                        new ProfesionalDato(17L, 2L, "Carla", "Lopez", true, FECHA, FECHA)),
                List.of(
                        new HorarioDato(
                                21L,
                                15L,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(12, 0),
                                30,
                                true,
                                FECHA,
                                FECHA)));
    }

    @Test
    void conservaIdsRelacionesDeshabilitadosYVersion() {
        assertThat(sincronizacion.aplicar(snapshot(7))).isEqualTo(7L);
        assertThat(categorias.count()).isEqualTo(2);
        assertThat(profesionales.count()).isEqualTo(3);
        assertThat(horarios.count()).isEqualTo(1);
        assertThat(categorias.findById(2L).orElseThrow().isHabilitada()).isFalse();
        assertThat(consulta.horarios(15L).getFirst().duracionTurnoMinutos()).isEqualTo(30);
        assertThat(consulta.estado().version()).isEqualTo(7L);
    }

    @Test
    void snapshotRepetidoNoDuplicaYReemplazoEliminaAusentes() {
        sincronizacion.aplicar(snapshot(7));
        sincronizacion.aplicar(snapshot(7));
        assertThat(profesionales.count()).isEqualTo(3);
        sincronizacion.aplicar(new SnapshotCatalogo(8L, FECHA, List.of(), List.of(), List.of()));
        assertThat(profesionales.count()).isZero();
        assertThat(categorias.count()).isZero();
        assertThat(consulta.estado().version()).isEqualTo(8L);
    }

    @Test
    void referenciasInvalidasNoDestruyenLaCopia() {
        sincronizacion.aplicar(snapshot(7));
        var s = snapshot(8);
        var invalido =
                new SnapshotCatalogo(8L, FECHA, List.of(), s.professionals(), s.weeklySchedules());
        assertThatThrownBy(() -> sincronizacion.aplicar(invalido))
                .isInstanceOf(ErrorCatalogo.class);
        assertThat(consulta.estado().version()).isEqualTo(7L);
        assertThat(profesionales.count()).isEqualTo(3);
    }

    @Test
    void falloDePersistenciaRevierteBorradosYVersion() {
        sincronizacion.aplicar(snapshot(7));
        // PostgreSQL no admite U+0000 en text: provoca un fallo real despues de borrar, dentro de
        // la transaccion.
        var corrupto =
                new SnapshotCatalogo(
                        8L,
                        FECHA,
                        List.of(new CategoriaDato(1L, "Clinica", "\u0000", true, FECHA, FECHA)),
                        List.of(),
                        List.of());
        assertThatThrownBy(() -> sincronizacion.aplicar(corrupto))
                .isInstanceOf(RuntimeException.class);
        assertThat(consulta.estado().version()).isEqualTo(7L);
        assertThat(profesionales.count()).isEqualTo(3);
    }

    @Test
    void versionViejaNoRetrocede() {
        sincronizacion.aplicar(snapshot(7));
        assertThatThrownBy(() -> sincronizacion.aplicar(snapshot(6)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(consulta.estado().version()).isEqualTo(7L);
    }

    @Test
    void buscaDesdePostgresConFiltrosCombinadosYPaginacion() {
        sincronizacion.aplicar(snapshot(7));
        assertThat(consulta.buscar("gOm", 1L, true, true, DayOfWeek.MONDAY, 0, 20).contenido())
                .extracting(
                        ar.com.modarelli.catalogo.domain.model.CatalogoVistas.ProfesionalVista::id)
                .containsExactly(15L);
        assertThat(consulta.buscar(null, null, false, null, null, 0, 20).contenido())
                .extracting(
                        ar.com.modarelli.catalogo.domain.model.CatalogoVistas.ProfesionalVista::id)
                .containsExactly(16L);
        assertThat(consulta.buscar(null, null, null, false, null, 0, 20).total()).isEqualTo(2);
        assertThat(consulta.buscar("%", null, null, null, null, 0, 20).total()).isZero();
        assertThat(consulta.buscar(null, null, null, null, null, 0, 1).contenido()).hasSize(1);
        assertThat(consulta.buscar(null, null, null, null, null, 0, 1).total()).isEqualTo(3);
        assertThatThrownBy(() -> consulta.buscar(null, null, null, null, null, 0, 101))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void horarioInvalidoYIdsDuplicadosSeRechazan() {
        var s = snapshot(7);
        var horario =
                new HorarioDato(
                        21L,
                        15L,
                        DayOfWeek.MONDAY,
                        LocalTime.NOON,
                        LocalTime.of(9, 0),
                        0,
                        true,
                        FECHA,
                        FECHA);
        assertThatThrownBy(
                        () ->
                                sincronizacion.aplicar(
                                        new SnapshotCatalogo(
                                                7L,
                                                FECHA,
                                                s.professionalCategories(),
                                                s.professionals(),
                                                List.of(horario))))
                .isInstanceOf(ErrorCatalogo.class);
        assertThatThrownBy(
                        () ->
                                sincronizacion.aplicar(
                                        new SnapshotCatalogo(
                                                7L,
                                                FECHA,
                                                List.of(
                                                        s.professionalCategories().getFirst(),
                                                        s.professionalCategories().getFirst()),
                                                List.of(),
                                                List.of())))
                .isInstanceOf(ErrorCatalogo.class);
        assertThat(consulta.estado().inicializado()).isFalse();
    }

    @Test
    void endpointsExigenIdentidadYAdministracion() throws Exception {
        mvc.perform(get("/api/catalogo/estado")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/catalogo/sincronizacion-incremental"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/admin/catalogo/sincronizacion-incremental")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/admin/catalogo/sincronizacion-completa")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/catalogo/profesionales").with(jwt()))
                .andExpect(status().isServiceUnavailable());
        sincronizacion.aplicar(snapshot(7));
        mvc.perform(get("/api/catalogo/profesionales").param("nombre", "Ana").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(15));
        mvc.perform(get("/api/catalogo/profesionales/999/horarios").with(jwt()))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/admin/catalogo/sincronizacion-completa")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void conservaHorasLocalesDeUnaAgendaDeTarde() {
        var datos = snapshot(7);

        var horario =
                new HorarioDato(
                        304L,
                        15L,
                        DayOfWeek.THURSDAY,
                        LocalTime.of(18, 0),
                        LocalTime.of(21, 0),
                        30,
                        true,
                        FECHA,
                        FECHA);

        sincronizacion.aplicar(
                new SnapshotCatalogo(
                        7L,
                        FECHA,
                        datos.professionalCategories(),
                        datos.professionals(),
                        List.of(horario)));

        String inicioGuardado =
                jdbc.queryForObject(
                        "select to_char(hora_inicio, 'HH24:MI:SS') "
                                + "from horario_semanal where id = 304",
                        String.class);

        String finGuardado =
                jdbc.queryForObject(
                        "select to_char(hora_fin, 'HH24:MI:SS') "
                                + "from horario_semanal where id = 304",
                        String.class);

        assertThat(inicioGuardado).isEqualTo("18:00:00");
        assertThat(finGuardado).isEqualTo("21:00:00");

        var recuperado = consulta.horarios(15L).getFirst();

        assertThat(recuperado.horaInicio()).isEqualTo(LocalTime.of(18, 0));
        assertThat(recuperado.horaFin()).isEqualTo(LocalTime.of(21, 0));
    }

    @Test
    void agendaParaTurnosExponeVersionEstadosYReglas() throws Exception {
        mvc.perform(get("/api/catalogo/profesionales/15/agenda"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/catalogo/profesionales/15/agenda").with(jwt()))
                .andExpect(status().isServiceUnavailable());
        sincronizacion.aplicar(snapshot(7));
        mvc.perform(get("/api/catalogo/profesionales/15/agenda").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(7))
                .andExpect(jsonPath("$.profesional.id").value(15))
                .andExpect(jsonPath("$.categoriaHabilitada").value(true))
                .andExpect(jsonPath("$.horarios[0].duracionTurnoMinutos").value(30));
        mvc.perform(get("/api/catalogo/profesionales/17/agenda").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaHabilitada").value(false));
        mvc.perform(get("/api/catalogo/profesionales/999/agenda").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscaNombreCompletoConEspaciosExterioresYMayusculas() throws Exception {
        sincronizacion.aplicar(snapshot(7));
        mvc.perform(get("/api/catalogo/profesionales").with(jwt()).param("nombre", "  aNa gOmEz  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(15));
    }

    @Test
    void combinaTodosLosFiltrosPorHttp() throws Exception {
        sincronizacion.aplicar(snapshot(7));
        mvc.perform(
                        get("/api/catalogo/profesionales")
                                .with(jwt())
                                .param("nombre", "Gomez")
                                .param("categoriaId", "1")
                                .param("habilitado", "true")
                                .param("conAgenda", "true")
                                .param("diaSemana", "MONDAY")
                                .param("pagina", "0")
                                .param("tamanio", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(15));
    }

    @Test
    void combinacionSinCoincidenciasEsPaginaVacia() throws Exception {
        sincronizacion.aplicar(snapshot(7));
        mvc.perform(
                        get("/api/catalogo/profesionales")
                                .with(jwt())
                                .param("nombre", "Ana")
                                .param("habilitado", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test
    void agendaSeRestringeAlDiaYNoDuplicaProfesionales() throws Exception {
        sincronizacion.aplicar(snapshot(7));
        mvc.perform(get("/api/catalogo/profesionales").with(jwt()).param("diaSemana", "MONDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/catalogo/profesionales").with(jwt()).param("diaSemana", "SUNDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(
                        get("/api/catalogo/profesionales")
                                .with(jwt())
                                .param("conAgenda", "false")
                                .param("diaSemana", "SUNDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void reglasDeshabilitadasNoCuentanComoAgenda() throws Exception {
        sincronizacion.aplicar(snapshot(7));
        new org.springframework.transaction.support.TransactionTemplate(gestor)
                .executeWithoutResult(
                        tx -> jdbc.update("update horario_semanal set habilitado=false"));
        mvc.perform(get("/api/catalogo/profesionales").with(jwt()).param("conAgenda", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void validaParametrosDeBusquedaPorHttp() throws Exception {
        sincronizacion.aplicar(snapshot(7));
        for (var par :
                List.of(
                        new String[] {"categoriaId", "0"},
                        new String[] {"categoriaId", "-1"},
                        new String[] {"pagina", "-1"},
                        new String[] {"tamanio", "0"},
                        new String[] {"tamanio", "101"},
                        new String[] {"pagina", "2147483647"},
                        new String[] {"diaSemana", "LUNES"},
                        new String[] {"habilitado", "invalido"},
                        new String[] {"conAgenda", "invalido"},
                        new String[] {"nombre", "x".repeat(256)})) {
            mvc.perform(get("/api/catalogo/profesionales").with(jwt()).param(par[0], par[1]))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void conservaTotalYOrdenEnPaginasSucesivas() throws Exception {
        sincronizacion.aplicar(snapshot(7));
        for (int pagina = 0; pagina < 3; pagina++) {
            int esperado = List.of(15, 17, 16).get(pagina);
            mvc.perform(
                            get("/api/catalogo/profesionales")
                                    .with(jwt())
                                    .param("pagina", String.valueOf(pagina))
                                    .param("tamanio", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(3))
                    .andExpect(jsonPath("$.contenido[0].id").value(esperado));
        }
        mvc.perform(
                        get("/api/catalogo/profesionales")
                                .with(jwt())
                                .param("pagina", "3")
                                .param("tamanio", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.contenido").isEmpty());
    }
}
