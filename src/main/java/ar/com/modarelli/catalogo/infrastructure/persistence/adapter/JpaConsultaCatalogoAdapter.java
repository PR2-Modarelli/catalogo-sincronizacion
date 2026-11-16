package ar.com.modarelli.catalogo.infrastructure.persistence.adapter;

import ar.com.modarelli.catalogo.domain.model.CatalogoVistas.*;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.EstadoCatalogo;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.HorarioSemanal;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.Profesional;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.CategoriaRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.EstadoCatalogoRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.HorarioSemanalRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.ProfesionalRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
@Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class JpaConsultaCatalogoAdapter
        implements ar.com.modarelli.catalogo.domain.ports.out.ConsultaCatalogoPort {
    private final ProfesionalRepository profesionales;
    private final HorarioSemanalRepository horarios;
    private final CategoriaRepository categorias;
    private final EstadoCatalogoRepository estados;

    public JpaConsultaCatalogoAdapter(
            ProfesionalRepository p,
            HorarioSemanalRepository h,
            CategoriaRepository c,
            EstadoCatalogoRepository e) {
        profesionales = p;
        horarios = h;
        categorias = c;
        estados = e;
    }

    public EstadoVista estado() {
        EstadoCatalogo e = estados.findById(1L).orElseThrow();
        return new EstadoVista(
                e.getVersion(),
                e.getSincronizadoEn(),
                e.getVersion() != null,
                categorias.count(),
                profesionales.count(),
                horarios.count(),
                e.getUltimoError());
    }

    private void exigirInicializado() {
        if (estados.findById(1L).orElseThrow().getVersion() == null)
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Catalogo pendiente de sincronizacion inicial");
    }

    public List<CategoriaVista> categorias() {
        exigirInicializado();
        return categorias.findAll(Sort.by("nombre").ascending().and(Sort.by("id"))).stream()
                .map(
                        c ->
                                new CategoriaVista(
                                        c.getId(),
                                        c.getNombre(),
                                        c.getDescripcion(),
                                        c.isHabilitada()))
                .toList();
    }

    public PaginaProfesionales buscar(
            String nombre,
            Long categoriaId,
            Boolean habilitado,
            Boolean conAgenda,
            DayOfWeek dia,
            int pagina,
            int tamanio) {
        exigirInicializado();
        if (pagina < 0 || tamanio < 1 || tamanio > 100)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pagina o tamanio invalido (1 a 100)");
        String texto = nombre == null ? null : nombre.strip().toLowerCase(Locale.ROOT);
        if (texto != null && texto.length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre demasiado largo");
        Specification<Profesional> filtro =
                (root, query, cb) -> {
                    List<Predicate> condiciones = new ArrayList<>();
                    if (texto != null && !texto.isEmpty()) {
                        String patron =
                                "%"
                                        + texto.replace("!", "!!")
                                                .replace("%", "!%")
                                                .replace("_", "!_")
                                        + "%";
                        condiciones.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("nombre")), patron, '!'),
                                        cb.like(cb.lower(root.get("apellido")), patron, '!')));
                    }
                    if (categoriaId != null)
                        condiciones.add(cb.equal(root.get("categoria").get("id"), categoriaId));
                    if (habilitado != null)
                        condiciones.add(cb.equal(root.get("habilitado"), habilitado));
                    if (conAgenda != null || dia != null) {
                        var sub = query.subquery(Long.class);
                        var h = sub.from(HorarioSemanal.class);
                        List<Predicate> agenda = new ArrayList<>();
                        agenda.add(cb.equal(h.get("profesional").get("id"), root.get("id")));
                        agenda.add(cb.isTrue(h.get("habilitado")));
                        if (dia != null) agenda.add(cb.equal(h.get("diaSemana"), dia));
                        sub.select(h.get("id")).where(agenda.toArray(Predicate[]::new));
                        condiciones.add(
                                Boolean.FALSE.equals(conAgenda)
                                        ? cb.not(cb.exists(sub))
                                        : cb.exists(sub));
                    }
                    return cb.and(condiciones.toArray(Predicate[]::new));
                };
        var resultado =
                profesionales.findAll(
                        filtro,
                        PageRequest.of(pagina, tamanio, Sort.by("apellido", "nombre", "id")));
        var contenido =
                resultado.getContent().stream()
                        .map(
                                p ->
                                        new ProfesionalVista(
                                                p.getId(),
                                                p.getNombre(),
                                                p.getApellido(),
                                                p.getCategoria().getId(),
                                                p.getCategoria().getNombre(),
                                                p.isHabilitado()))
                        .toList();
        return new PaginaProfesionales(contenido, resultado.getTotalElements(), pagina, tamanio);
    }

    public List<HorarioVista> horarios(Long profesionalId) {
        exigirInicializado();
        if (!profesionales.existsById(profesionalId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesional inexistente");
        return horarios.findByProfesionalIdOrderByDiaSemanaAscHoraInicioAsc(profesionalId).stream()
                .map(
                        h ->
                                new HorarioVista(
                                        h.getId(),
                                        h.getProfesional().getId(),
                                        h.getDiaSemana(),
                                        h.getHoraInicio(),
                                        h.getHoraFin(),
                                        h.getDuracionTurnoMinutos(),
                                        h.isHabilitado()))
                .toList();
    }

    public AgendaVista agenda(Long id) {
        exigirInicializado();
        Profesional p =
                profesionales
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Profesional inexistente"));
        ProfesionalVista vista =
                new ProfesionalVista(
                        p.getId(),
                        p.getNombre(),
                        p.getApellido(),
                        p.getCategoria().getId(),
                        p.getCategoria().getNombre(),
                        p.isHabilitado());
        return new AgendaVista(
                estados.findById(1L).orElseThrow().getVersion(),
                vista,
                p.getCategoria().isHabilitada(),
                horarios(id));
    }
}
