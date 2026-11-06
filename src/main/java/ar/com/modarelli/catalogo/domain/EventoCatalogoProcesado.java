package ar.com.modarelli.catalogo.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evento_catalogo_procesado")
public class EventoCatalogoProcesado {
    @Id @Column(name = "event_id", length = 200) private String id;
    @Column(name = "version_evento", nullable = false) private Long version;
    @Column(name = "procesado_en", nullable = false) private Instant procesadoEn;
    public EventoCatalogoProcesado() {}
    public EventoCatalogoProcesado(String id, Long version) {
        this.id = id; this.version = version; this.procesadoEn = Instant.now();
    }
    public String getId() { return id; }
    public Long getVersion() { return version; }
}
