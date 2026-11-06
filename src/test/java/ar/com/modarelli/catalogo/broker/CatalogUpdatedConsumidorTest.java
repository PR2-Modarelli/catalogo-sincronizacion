package ar.com.modarelli.catalogo.broker;

import static org.assertj.core.api.Assertions.*;
import ar.com.modarelli.catalogo.service.SincronizacionIncrementalServicio;
import ar.com.modarelli.catalogo.service.dto.EventoCatalogo;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CatalogUpdatedConsumidorTest {
    static class Receptor extends SincronizacionIncrementalServicio {
        EventoCatalogo recibido;
        Receptor() {super(null,null,null,null);}
        @Override public Long procesar(EventoCatalogo e) {recibido=e;return e.newVersion();}
    }
    private static final String JSON="""
        {"eventId":"e-8","eventType":"CatalogUpdated","newVersion":8,
        "occurredAt":"2026-09-10T13:40:00Z","source":"SCHEDULED","schemaVersion":1,"nuevo":true}
        """;
    @Test void entregaEventoValidoAlServicio() {
        var receptor=new Receptor();var consumidor=new CatalogUpdatedConsumidor(JsonMapper.builder().findAndAddModules().build(),receptor);
        consumidor.recibir(JSON,"8");assertThat(receptor.recibido.eventId()).isEqualTo("e-8");
    }
    @Test void rechazaClaveInconsistenteAntesDeAplicar() {
        var receptor=new Receptor();var consumidor=new CatalogUpdatedConsumidor(JsonMapper.builder().findAndAddModules().build(),receptor);
        assertThatThrownBy(()->consumidor.recibir(JSON,"9")).isInstanceOf(IllegalArgumentException.class);
        assertThat(receptor.recibido).isNull();
    }
    @Test void rechazaMensajeInvalidoSinPropagarSuContenido() {
        var consumidor=new CatalogUpdatedConsumidor(JsonMapper.builder().findAndAddModules().build(),new Receptor());
        assertThatThrownBy(()->consumidor.recibir("contenido-externo","8")).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("contenido-externo");
    }
}
