package ar.com.modarelli.catalogo.service;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
class CatedraSnapshotClienteTest {
 @Test void leeContratoIgnoraCamposNuevosYEnviaBearer() {
  var builder=RestClient.builder(); var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("http://catedra.test/api/synchronization/snapshot"))
   .andExpect(header("Authorization","Bearer token-de-prueba"))
   .andRespond(withSuccess("""
   {"snapshotVersion":7,"generatedAt":"2026-09-10T13:44:59Z","extra":"compatible",
    "professionalCategories":[{"id":1,"name":"Clinica","description":null,"enabled":true,"createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}],
    "professionals":[{"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true,"createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}],
    "weeklySchedules":[{"id":21,"professionalId":15,"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00:00","slotDurationMinutes":30,"enabled":true,"createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}]}
   """,MediaType.APPLICATION_JSON));
  var s=new CatedraSnapshotCliente("http://catedra.test/","token-de-prueba",builder).descargar();
  assertThat(s.snapshotVersion()).isEqualTo(7);
  assertThat(s.weeklySchedules().getFirst().startTime().getHour()).isEqualTo(9);
  server.verify();
 }
 @Test void configuracionAusenteNoIntentaConectar() {
  assertThatThrownBy(()->new CatedraSnapshotCliente("", "",RestClient.builder()).descargar()).isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
 }
 @Test void errorExternoNoFiltraRespuesta() {
  var builder=RestClient.builder(); var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("http://catedra.test/api/synchronization/snapshot"))
   .andRespond(withUnauthorizedRequest().body("credencial-privada"));
  assertThatThrownBy(()->new CatedraSnapshotCliente("http://catedra.test","secreto",builder).descargar())
   .isInstanceOf(ResponseStatusException.class).hasMessageNotContaining("credencial-privada").hasMessageNotContaining("secreto");
  server.verify();
 }
 @Test void respuestaVaciaSeRechaza() {
  var builder=RestClient.builder(); var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("http://catedra.test/api/synchronization/snapshot")).andRespond(withNoContent());
  assertThatThrownBy(()->new CatedraSnapshotCliente("http://catedra.test","token",builder).descargar()).isInstanceOf(ResponseStatusException.class);
  server.verify();
 }
}
