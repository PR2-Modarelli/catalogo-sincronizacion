package ar.com.modarelli.catalogo.service;

import ar.com.modarelli.catalogo.domain.*;
import ar.com.modarelli.catalogo.repository.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SnapshotCatalogoServicio {
 private final CategoriaRepository categorias;
 private final ProfesionalRepository profesionales;
 private final HorarioSemanalRepository horarios;
 private final EstadoCatalogoRepository estados;
 public SnapshotCatalogoServicio(CategoriaRepository c, ProfesionalRepository p, HorarioSemanalRepository h, EstadoCatalogoRepository e) {
  categorias=c; profesionales=p; horarios=h; estados=e;
 }
 @Transactional
 public Long aplicar(SnapshotCatalogo snapshot) {
  validar(snapshot);
  EstadoCatalogo estado=estados.bloquear();
  if(estado.getVersion()!=null && snapshot.snapshotVersion()<estado.getVersion())
   throw new ResponseStatusException(HttpStatus.CONFLICT, "El snapshot es anterior a la version local");
  // Un snapshot completo puede reconstruir incluso la misma version.
  horarios.deleteAllInBatch(); profesionales.deleteAllInBatch(); categorias.deleteAllInBatch();
  Map<Long,Categoria> mapaCategorias=new HashMap<>();
  for(var d:snapshot.professionalCategories()) {
   Categoria c=new Categoria(); c.setId(d.id()); c.setNombre(d.name()); c.setDescripcion(d.description());
   c.setHabilitada(d.enabled()); c.setFechaCreacion(d.createdAt()); c.setFechaActualizacion(d.updatedAt());
   mapaCategorias.put(d.id(),categorias.save(c));
  }
  categorias.flush();
  Map<Long,Profesional> mapaProfesionales=new HashMap<>();
  for(var d:snapshot.professionals()) {
   Profesional v=new Profesional(); v.setId(d.id()); v.setNombre(d.firstName()); v.setApellido(d.lastName());
   v.setCategoria(mapaCategorias.get(d.categoryId())); v.setHabilitado(d.enabled());
   v.setFechaCreacion(d.createdAt()); v.setFechaActualizacion(d.updatedAt());
   mapaProfesionales.put(d.id(),profesionales.save(v));
  }
  profesionales.flush();
  for(var d:snapshot.weeklySchedules()) {
   HorarioSemanal h=new HorarioSemanal(); h.setId(d.id()); h.setProfesional(mapaProfesionales.get(d.professionalId()));
   h.setDiaSemana(d.dayOfWeek()); h.setHoraInicio(d.startTime()); h.setHoraFin(d.endTime());
   h.setDuracionTurnoMinutos(d.slotDurationMinutes()); h.setHabilitado(d.enabled());
   h.setFechaCreacion(d.createdAt()); h.setFechaActualizacion(d.updatedAt()); horarios.save(h);
  }
  horarios.flush();
  estado.setUltimoError(null); estado.setVersion(snapshot.snapshotVersion()); estado.setSincronizadoEn(Instant.now()); estados.saveAndFlush(estado);
  return estado.getVersion();
 }
 private static void exigir(boolean condicion) {
  if(!condicion) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Snapshot de catedra invalido o incompleto");
 }
 private static void registro(Long id,Boolean enabled,Instant creado,Instant actualizado,Set<Long> ids) {
  exigir(id!=null && enabled!=null && creado!=null && actualizado!=null);
  exigir(ids.add(id));
 }
 private static boolean texto(String s) { return s!=null && !s.isBlank() && s.length()<=255; }
 static void validar(SnapshotCatalogo s) {
  exigir(s!=null);
  exigir(s.snapshotVersion()!=null && s.snapshotVersion()>=0 && s.generatedAt()!=null);
  exigir(s.professionalCategories()!=null && s.professionals()!=null && s.weeklySchedules()!=null);
  Set<Long> cs=new HashSet<>(), ps=new HashSet<>(), hs=new HashSet<>();
  for(var c:s.professionalCategories()) {
   exigir(c!=null); registro(c.id(),c.enabled(),c.createdAt(),c.updatedAt(),cs);
   exigir(texto(c.name()));
  }
  for(var p:s.professionals()) {
   exigir(p!=null); registro(p.id(),p.enabled(),p.createdAt(),p.updatedAt(),ps);
   exigir(cs.contains(p.categoryId()) && texto(p.firstName()) && texto(p.lastName()));
  }
  for(var h:s.weeklySchedules()) {
   exigir(h!=null); registro(h.id(),h.enabled(),h.createdAt(),h.updatedAt(),hs);
   exigir(ps.contains(h.professionalId()) && h.dayOfWeek()!=null && h.startTime()!=null && h.endTime()!=null);
   exigir(h.endTime().isAfter(h.startTime()) && h.slotDurationMinutes()!=null && h.slotDurationMinutes()>0);
  }
 }
}
