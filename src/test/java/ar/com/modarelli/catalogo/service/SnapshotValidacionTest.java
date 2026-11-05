package ar.com.modarelli.catalogo.service;
import static org.assertj.core.api.Assertions.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class SnapshotValidacionTest {
 private final SnapshotCatalogoServicio servicio=new SnapshotCatalogoServicio(null,null,null,null);
 private final Instant fecha=Instant.parse("2026-09-10T13:40:00Z");
 private CategoriaDato categoria() {return new CategoriaDato(1L,"Clinica",null,true,fecha,fecha);}
 private ProfesionalDato profesional() {return new ProfesionalDato(15L,1L,"Ana","Gomez",true,fecha,fecha);}
 private void rechaza(SnapshotCatalogo s) {
  assertThatThrownBy(()->servicio.aplicar(s)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("502");
 }
 @Test void rechazaSnapshotNulo() {rechaza(null);}
 @Test void rechazaColeccionAusente() {rechaza(new SnapshotCatalogo(7L,fecha,null,List.of(),List.of()));}
 @Test void rechazaVersionNegativa() {rechaza(new SnapshotCatalogo(-1L,fecha,List.of(),List.of(),List.of()));}
 @Test void rechazaCategoriasDuplicadas() {rechaza(new SnapshotCatalogo(7L,fecha,List.of(categoria(),categoria()),List.of(),List.of()));}
 @Test void rechazaReferenciaInexistente() {rechaza(new SnapshotCatalogo(7L,fecha,List.of(),List.of(profesional()),List.of()));}
 @Test void rechazaDuracionCero() {
  var h=new HorarioDato(21L,15L,DayOfWeek.MONDAY,LocalTime.of(9,0),LocalTime.NOON,0,true,fecha,fecha);
  rechaza(new SnapshotCatalogo(7L,fecha,List.of(categoria()),List.of(profesional()),List.of(h)));
 }
 @Test void rechazaHorarioInvertido() {
  var h=new HorarioDato(21L,15L,DayOfWeek.MONDAY,LocalTime.NOON,LocalTime.of(9,0),30,true,fecha,fecha);
  rechaza(new SnapshotCatalogo(7L,fecha,List.of(categoria()),List.of(profesional()),List.of(h)));
 }
 @Test void rechazaCampoObligatorioAusente() {
  var c=new CategoriaDato(1L,"Clinica",null,null,fecha,fecha);
  rechaza(new SnapshotCatalogo(7L,fecha,List.of(c),List.of(),List.of()));
 }
}
