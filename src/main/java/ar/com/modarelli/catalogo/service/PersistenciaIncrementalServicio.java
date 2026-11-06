package ar.com.modarelli.catalogo.service;

import ar.com.modarelli.catalogo.domain.*;
import ar.com.modarelli.catalogo.repository.*;
import ar.com.modarelli.catalogo.service.dto.*;
import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class PersistenciaIncrementalServicio {
    private final CategoriaRepository categorias;
    private final ProfesionalRepository profesionales;
    private final HorarioSemanalRepository horarios;
    private final EstadoCatalogoRepository estados;
    private final EventoCatalogoProcesadoRepository eventos;

    public PersistenciaIncrementalServicio(CategoriaRepository c, ProfesionalRepository p, HorarioSemanalRepository h,
        EstadoCatalogoRepository e, EventoCatalogoProcesadoRepository eventos) {
        categorias=c; profesionales=p; horarios=h; estados=e; this.eventos=eventos;
    }
    @Transactional
    public Long aplicar(CambioCatalogo cambio, EventoCatalogo evento) {
        validarCabecera(cambio,evento);
        EstadoCatalogo estado=estados.bloquear();
        var procesado=eventos.findById(evento.eventId());
        if (procesado.isPresent()) {
            if (!procesado.get().getVersion().equals(evento.newVersion())) throw new IllegalArgumentException("eventId reutilizado con otra version");
            return estado.getVersion();
        }
        if (!Objects.equals(estado.getVersion(),cambio.versionBase())) throw new ConcurrentModificationException("La version local no coincide con la base del cambio");
        var cs=new LinkedHashMap<Long,CategoriaDato>();
        for (var c:categorias.findAll()) cs.put(c.getId(),new CategoriaDato(c.getId(),c.getNombre(),c.getDescripcion(),c.isHabilitada(),c.getFechaCreacion(),c.getFechaActualizacion()));
        var ps=new LinkedHashMap<Long,ProfesionalDato>();
        for (var p:profesionales.findAll()) ps.put(p.getId(),new ProfesionalDato(p.getId(),p.getCategoria().getId(),p.getNombre(),p.getApellido(),p.isHabilitado(),p.getFechaCreacion(),p.getFechaActualizacion()));
        var hs=new LinkedHashMap<Long,HorarioDato>();
        for (var h:horarios.findAll()) hs.put(h.getId(),new HorarioDato(h.getId(),h.getProfesional().getId(),h.getDiaSemana(),h.getHoraInicio(),h.getHoraFin(),h.getDuracionTurnoMinutos(),h.isHabilitado(),h.getFechaCreacion(),h.getFechaActualizacion()));
        var datos=cambio;
        for (var c:datos.categorias()) cs.put(c.id(),c);
        for (var p:datos.profesionales()) ps.put(p.id(),p);
        for (var h:datos.horarios()) hs.put(h.id(),h);
        SnapshotCatalogoServicio.validar(new SnapshotCatalogo(cambio.versionDestino(),Instant.now(),List.copyOf(cs.values()),List.copyOf(ps.values()),List.copyOf(hs.values())));
        for (var c:datos.categorias()) {
            var v=categorias.findById(c.id()).orElseGet(Categoria::new);
            v.setId(c.id()); v.setNombre(c.name()); v.setDescripcion(c.description()); v.setHabilitada(c.enabled());
            v.setFechaCreacion(c.createdAt()); v.setFechaActualizacion(c.updatedAt()); categorias.save(v);
        }
        categorias.flush();
        for (var p:datos.profesionales()) {
            var v=profesionales.findById(p.id()).orElseGet(Profesional::new);
            v.setId(p.id()); v.setCategoria(categorias.getReferenceById(p.categoryId())); v.setNombre(p.firstName()); v.setApellido(p.lastName());
            v.setHabilitado(p.enabled()); v.setFechaCreacion(p.createdAt()); v.setFechaActualizacion(p.updatedAt()); profesionales.save(v);
        }
        profesionales.flush();
        for (var h:datos.horarios()) {
            var v=horarios.findById(h.id()).orElseGet(HorarioSemanal::new);
            v.setId(h.id()); v.setProfesional(profesionales.getReferenceById(h.professionalId())); v.setDiaSemana(h.dayOfWeek());
            v.setHoraInicio(h.startTime()); v.setHoraFin(h.endTime()); v.setDuracionTurnoMinutos(h.slotDurationMinutes());
            v.setHabilitado(h.enabled()); v.setFechaCreacion(h.createdAt()); v.setFechaActualizacion(h.updatedAt()); horarios.save(v);
        }
        horarios.flush();
        estado.setVersion(cambio.versionDestino()); estado.setSincronizadoEn(Instant.now());
        eventos.saveAndFlush(new EventoCatalogoProcesado(evento.eventId(),evento.newVersion()));
        estados.saveAndFlush(estado);
        return estado.getVersion();
    }
    private void validarCabecera(CambioCatalogo cambio,EventoCatalogo evento) {
        if (evento==null) throw new IllegalArgumentException("Falta el evento");
        evento.validar();
        if (cambio==null || cambio.versionBase()==null || cambio.versionDestino()==null || cambio.versionBase()<0
            || cambio.versionBase()==Long.MAX_VALUE || cambio.versionDestino()!=cambio.versionBase()+1
            || !cambio.versionDestino().equals(evento.newVersion())
            || cambio.categorias()==null || cambio.profesionales()==null || cambio.horarios()==null) {
            throw new IllegalArgumentException("Cambio de catalogo invalido o no consecutivo");
        }
        unicos(cambio.categorias().stream().map(c -> c==null ? null : c.id()).toList());
        unicos(cambio.profesionales().stream().map(p -> p==null ? null : p.id()).toList());
        unicos(cambio.horarios().stream().map(h -> h==null ? null : h.id()).toList());
    }
    private void unicos(List<Long> ids) {
        if (ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size()!=ids.size()) {
            throw new IllegalArgumentException("Entidades nulas o IDs duplicados en el cambio");
        }
    }
}
