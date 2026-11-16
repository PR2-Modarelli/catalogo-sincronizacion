package ar.com.modarelli.catalogo.infrastructure.persistence.adapter;

import static ar.com.modarelli.catalogo.domain.service.ValidacionIncremental.*;

import ar.com.modarelli.catalogo.domain.model.CambioCatalogo;
import ar.com.modarelli.catalogo.domain.model.EventoCatalogo;
import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo;
import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo.*;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.Categoria;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.EstadoCatalogo;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.EventoCatalogoProcesado;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.HorarioSemanal;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.Profesional;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.CategoriaRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.EstadoCatalogoRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.EventoCatalogoProcesadoRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.HorarioSemanalRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.ProfesionalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.Instant;
import java.util.*;

@Service
public class JpaIncrementalCatalogoAdapter
        implements ar.com.modarelli.catalogo.domain.ports.out.PersistenciaCatalogoPort {
    private final CategoriaRepository categorias;
    private final ProfesionalRepository profesionales;
    private final HorarioSemanalRepository horarios;
    private final EstadoCatalogoRepository estados;
    private final EventoCatalogoProcesadoRepository eventos;

    public JpaIncrementalCatalogoAdapter(
            CategoriaRepository c,
            ProfesionalRepository p,
            HorarioSemanalRepository h,
            EstadoCatalogoRepository e,
            EventoCatalogoProcesadoRepository eventos) {
        categorias = c;
        profesionales = p;
        horarios = h;
        estados = e;
        this.eventos = eventos;
    }

    @Transactional
    public Long aplicar(CambioCatalogo cambio, EventoCatalogo evento) {
        validarCabecera(cambio, evento);
        EstadoCatalogo estado = estados.bloquear();
        var procesado = eventos.findById(evento.eventId());
        if (procesado.isPresent()) {
            if (!procesado.orElseThrow().getVersion().equals(evento.newVersion()))
                throw new IllegalArgumentException("eventId reutilizado con otra version");
            return estado.getVersion();
        }
        if (!Objects.equals(estado.getVersion(), cambio.versionBase()))
            throw new ConcurrentModificationException(
                    "La version local no coincide con la base del cambio");
        return guardar(cambio, evento, estado);
    }

    @Transactional
    public Long aplicarRango(List<CambioCatalogo> cambios, EventoCatalogo evento) {
        CambioCatalogo consolidado =
                ar.com.modarelli.catalogo.domain.service.RangoCatalogo.unir(cambios, evento);
        Long base = consolidado.versionBase();
        EstadoCatalogo estado = estados.bloquear();
        if (procesado(evento)) return estado.getVersion();
        if (!Objects.equals(estado.getVersion(), base))
            throw new ConcurrentModificationException("La version local cambio");
        // Redis publica entidades vigentes. Solo el estado final del rango queda visible al hacer
        // commit.
        return guardar(consolidado, evento, estado);
    }

    private Long guardar(CambioCatalogo cambio, EventoCatalogo evento, EstadoCatalogo estado) {
        var cs = new LinkedHashMap<Long, CategoriaDato>();
        for (var c : categorias.findAll())
            cs.put(
                    c.getId(),
                    new CategoriaDato(
                            c.getId(),
                            c.getNombre(),
                            c.getDescripcion(),
                            c.isHabilitada(),
                            c.getFechaCreacion(),
                            c.getFechaActualizacion()));
        var ps = new LinkedHashMap<Long, ProfesionalDato>();
        for (var p : profesionales.findAll())
            ps.put(
                    p.getId(),
                    new ProfesionalDato(
                            p.getId(),
                            p.getCategoria().getId(),
                            p.getNombre(),
                            p.getApellido(),
                            p.isHabilitado(),
                            p.getFechaCreacion(),
                            p.getFechaActualizacion()));
        var hs = new LinkedHashMap<Long, HorarioDato>();
        for (var h : horarios.findAll())
            hs.put(
                    h.getId(),
                    new HorarioDato(
                            h.getId(),
                            h.getProfesional().getId(),
                            h.getDiaSemana(),
                            h.getHoraInicio(),
                            h.getHoraFin(),
                            h.getDuracionTurnoMinutos(),
                            h.isHabilitado(),
                            h.getFechaCreacion(),
                            h.getFechaActualizacion()));
        var datos = cambio;
        for (var c : datos.categorias()) cs.put(c.id(), c);
        for (var p : datos.profesionales()) ps.put(p.id(), p);
        for (var h : datos.horarios()) hs.put(h.id(), h);
        ar.com.modarelli.catalogo.domain.service.ValidacionCatalogo.validar(
                new SnapshotCatalogo(
                        cambio.versionDestino(),
                        Instant.now(),
                        List.copyOf(cs.values()),
                        List.copyOf(ps.values()),
                        List.copyOf(hs.values())));
        for (var c : datos.categorias()) {
            var v = categorias.findById(c.id()).orElseGet(Categoria::new);
            v.setId(c.id());
            v.setNombre(c.name());
            v.setDescripcion(c.description());
            v.setHabilitada(c.enabled());
            v.setFechaCreacion(c.createdAt());
            v.setFechaActualizacion(c.updatedAt());
            categorias.save(v);
        }
        categorias.flush();
        for (var p : datos.profesionales()) {
            var v = profesionales.findById(p.id()).orElseGet(Profesional::new);
            v.setId(p.id());
            v.setCategoria(categorias.getReferenceById(p.categoryId()));
            v.setNombre(p.firstName());
            v.setApellido(p.lastName());
            v.setHabilitado(p.enabled());
            v.setFechaCreacion(p.createdAt());
            v.setFechaActualizacion(p.updatedAt());
            profesionales.save(v);
        }
        profesionales.flush();
        for (var h : datos.horarios()) {
            var v = horarios.findById(h.id()).orElseGet(HorarioSemanal::new);
            v.setId(h.id());
            v.setProfesional(profesionales.getReferenceById(h.professionalId()));
            v.setDiaSemana(h.dayOfWeek());
            v.setHoraInicio(h.startTime());
            v.setHoraFin(h.endTime());
            v.setDuracionTurnoMinutos(h.slotDurationMinutes());
            v.setHabilitado(h.enabled());
            v.setFechaCreacion(h.createdAt());
            v.setFechaActualizacion(h.updatedAt());
            horarios.save(v);
        }
        horarios.flush();
        estado.setVersion(cambio.versionDestino());
        estado.setSincronizadoEn(Instant.now());
        estado.setUltimoError(null);
        registrar(estado, evento);
        estados.saveAndFlush(estado);
        return estado.getVersion();
    }

    @Transactional(readOnly = true)
    public Long version() {
        return estados.findById(1L).orElseThrow().getVersion();
    }

    @Transactional(readOnly = true)
    public boolean procesado(EventoCatalogo evento) {
        if (evento == null) return false;
        return eventos.findById(evento.eventId())
                .map(
                        e -> {
                            if (!e.getVersion().equals(evento.newVersion()))
                                throw new IllegalArgumentException(
                                        "eventId reutilizado con otra version");
                            return true;
                        })
                .orElse(false);
    }

    @Transactional
    public Long confirmar(EventoCatalogo evento) {
        EstadoCatalogo estado = estados.bloquear();
        registrar(estado, evento);
        estado.setUltimoError(null);
        return estado.getVersion();
    }

    @Transactional
    public Long recuperar(
            SnapshotCatalogo snapshot,
            EventoCatalogo evento,
            long minimo,
            ar.com.modarelli.catalogo.domain.ports.in.AplicarSnapshotUseCase servicio) {
        EstadoCatalogo estado = estados.bloquear();
        if (snapshot == null
                || snapshot.snapshotVersion() == null
                || snapshot.snapshotVersion() < minimo)
            throw new IllegalStateException("Snapshot aun no publicado");
        if (estado.getVersion() == null || estado.getVersion() <= snapshot.snapshotVersion())
            servicio.aplicar(snapshot);
        registrar(estado, evento);
        estado.setUltimoError(null);
        return estado.getVersion();
    }

    private void registrar(EstadoCatalogo estado, EventoCatalogo evento) {
        if (evento == null) return;
        evento.validar();
        if (estado.getVersion() == null || estado.getVersion() < evento.newVersion())
            throw new IllegalStateException("Evento no aplicado");
        if (!procesado(evento))
            eventos.saveAndFlush(
                    new EventoCatalogoProcesado(evento.eventId(), evento.newVersion()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarError() {
        estados.bloquear().setUltimoError("SINCRONIZACION_NO_DISPONIBLE");
    }
}
