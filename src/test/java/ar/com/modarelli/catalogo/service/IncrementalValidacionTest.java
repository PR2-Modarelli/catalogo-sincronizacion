package ar.com.modarelli.catalogo.service;

import static org.assertj.core.api.Assertions.*;
import ar.com.modarelli.catalogo.service.dto.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IncrementalValidacionTest {
    private final PersistenciaIncrementalServicio servicio=new PersistenciaIncrementalServicio(null,null,null,null,null);
    private static final Instant FECHA=Instant.parse("2026-09-10T13:40:00Z");
    private EventoCatalogo evento(long version) { return new EventoCatalogo("evento-"+version,"CatalogUpdated",version,FECHA,"MANUAL",1); }
    private CambioCatalogo cambio(Long base,Long destino) { return new CambioCatalogo(base,destino,List.of(),List.of(),List.of()); }
    @Test void rechazaSaltosDeVersion() {
        assertThatThrownBy(()->servicio.aplicar(cambio(7L,9L),evento(9))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rechazaVersionDeEventoDistinta() {
        assertThatThrownBy(()->servicio.aplicar(cambio(7L,8L),evento(9))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rechazaCambioSinColecciones() {
        assertThatThrownBy(()->servicio.aplicar(new CambioCatalogo(7L,8L,null,List.of(),List.of()),evento(8))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rechazaCategoriasRepetidas() {
        var categoria=new CategoriaDato(1L,"Clinica",null,true,FECHA,FECHA);
        assertThatThrownBy(()->servicio.aplicar(new CambioCatalogo(7L,8L,List.of(categoria,categoria),List.of(),List.of()),evento(8))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rechazaEntidadesNulas() {
        assertThatThrownBy(()->servicio.aplicar(new CambioCatalogo(7L,8L,Arrays.asList((CategoriaDato)null),List.of(),List.of()),evento(8))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rechazaEventoSinIdentificadorYEsquemaDesconocido() {
        assertThatThrownBy(()->servicio.aplicar(cambio(7L,8L),new EventoCatalogo(" ","CatalogUpdated",8L,FECHA,"MANUAL",1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->servicio.aplicar(cambio(7L,8L),new EventoCatalogo("id","CatalogUpdated",8L,FECHA,"MANUAL",2))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rechazaVersionNegativaYDesbordamiento() {
        assertThatThrownBy(()->servicio.aplicar(cambio(-1L,0L),evento(0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->servicio.aplicar(cambio(Long.MAX_VALUE,0L),evento(0))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void aceptaCamposNuevosDelContratoDelEvento() {
        var json=JsonMapper.builder().findAndAddModules().build();
        var e=json.readValue("""
          {"eventId":"id-8","eventType":"CatalogUpdated","newVersion":8,
          "occurredAt":"2026-09-10T13:40:00Z","source":"SCHEDULED","schemaVersion":1,"nuevoCampo":"compatible"}
          """,EventoCatalogo.class);
        e.validar();
        assertThat(e.newVersion()).isEqualTo(8L);
    }
}
