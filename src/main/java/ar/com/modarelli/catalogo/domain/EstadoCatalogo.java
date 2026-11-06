package ar.com.modarelli.catalogo.domain;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="estado_catalogo")
public class EstadoCatalogo {
    @Id
    private Long id;
    @Column(name="version_aplicada")
    private Long version;
    @Column(name="sincronizado_en")
    private Instant sincronizadoEn;
    @Column(name="ultimo_error", length=80)
    private String ultimoError;
    public String getUltimoError() { return ultimoError; }
    public void setUltimoError(String value) { ultimoError = value; }
    public EstadoCatalogo() {}
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { this.version = value; }
    public Instant getSincronizadoEn() { return sincronizadoEn; }
    public void setSincronizadoEn(Instant value) { this.sincronizadoEn = value; }
}
