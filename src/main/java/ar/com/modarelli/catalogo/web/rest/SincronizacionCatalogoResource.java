package ar.com.modarelli.catalogo.web.rest;
import ar.com.modarelli.catalogo.service.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/catalogo")
public class SincronizacionCatalogoResource {
 private final CatedraSnapshotCliente cliente;
 private final SnapshotCatalogoServicio servicio;
 private final SincronizacionIncrementalServicio incremental;
 public SincronizacionCatalogoResource(CatedraSnapshotCliente c,SnapshotCatalogoServicio s,SincronizacionIncrementalServicio i) {cliente=c;servicio=s;incremental=i;}
 @PostMapping("/sincronizacion-incremental")
 public Resultado incremental() {return new Resultado(incremental.sincronizar());}
 public record Resultado(Long version) {}
 @PostMapping("/sincronizacion-completa")
 public Resultado sincronizar() {return new Resultado(servicio.aplicar(cliente.descargar()));}
}
