package ar.com.modarelli.catalogo.web.rest;
import ar.com.modarelli.catalogo.service.*;
import ar.com.modarelli.catalogo.service.ConsultaCatalogoServicio.*;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/catalogo")
public class ConsultaCatalogoResource {
 private final ConsultaCatalogoServicio consulta;
 public ConsultaCatalogoResource(ConsultaCatalogoServicio c) {consulta=c;}
 @GetMapping("/estado") public EstadoVista estado() {return consulta.estado();}
 @GetMapping("/categorias") public List<CategoriaVista> categorias() {return consulta.categorias();}
 @GetMapping("/profesionales")
 public PaginaProfesionales buscar(@RequestParam(required=false) String nombre,
  @RequestParam(required=false) Long categoriaId,@RequestParam(required=false) Boolean habilitado,
  @RequestParam(required=false) Boolean conAgenda,@RequestParam(required=false) DayOfWeek diaSemana,
  @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanio) {
  return consulta.buscar(nombre,categoriaId,habilitado,conAgenda,diaSemana,pagina,tamanio);
 }
 @GetMapping("/profesionales/{id}/horarios") public List<HorarioVista> horarios(@PathVariable Long id) {return consulta.horarios(id);}
 @GetMapping("/profesionales/{id}/agenda") public AgendaVista agenda(@PathVariable Long id) {return consulta.agenda(id);}
}
