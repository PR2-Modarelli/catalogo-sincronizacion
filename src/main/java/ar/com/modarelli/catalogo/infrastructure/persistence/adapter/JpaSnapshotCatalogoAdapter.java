package ar.com.modarelli.catalogo.infrastructure.persistence.adapter;

import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.Categoria;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.EstadoCatalogo;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.HorarioSemanal;
import ar.com.modarelli.catalogo.infrastructure.persistence.entity.Profesional;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.CategoriaRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.EstadoCatalogoRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.HorarioSemanalRepository;
import ar.com.modarelli.catalogo.infrastructure.persistence.repository.ProfesionalRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class JpaSnapshotCatalogoAdapter
        implements ar.com.modarelli.catalogo.domain.ports.out.SnapshotCatalogoPort {
    private final CategoriaRepository categorias;
    private final ProfesionalRepository profesionales;
    private final HorarioSemanalRepository horarios;
    private final EstadoCatalogoRepository estados;

    public JpaSnapshotCatalogoAdapter(
            CategoriaRepository c,
            ProfesionalRepository p,
            HorarioSemanalRepository h,
            EstadoCatalogoRepository e) {
        categorias = c;
        profesionales = p;
        horarios = h;
        estados = e;
    }

    @Transactional
    public Long aplicar(SnapshotCatalogo snapshot) {
        ar.com.modarelli.catalogo.domain.service.ValidacionCatalogo.validar(snapshot);
        EstadoCatalogo estado = estados.bloquear();
        if (estado.getVersion() != null && snapshot.snapshotVersion() < estado.getVersion())
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El snapshot es anterior a la version local");
        // Un snapshot completo puede reconstruir incluso la misma version.
        horarios.deleteAllInBatch();
        profesionales.deleteAllInBatch();
        categorias.deleteAllInBatch();
        Map<Long, Categoria> mapaCategorias = new HashMap<>();
        for (var d : snapshot.professionalCategories()) {
            Categoria c = new Categoria();
            c.setId(d.id());
            c.setNombre(d.name());
            c.setDescripcion(d.description());
            c.setHabilitada(d.enabled());
            c.setFechaCreacion(d.createdAt());
            c.setFechaActualizacion(d.updatedAt());
            mapaCategorias.put(d.id(), categorias.save(c));
        }
        categorias.flush();
        Map<Long, Profesional> mapaProfesionales = new HashMap<>();
        for (var d : snapshot.professionals()) {
            Profesional v = new Profesional();
            v.setId(d.id());
            v.setNombre(d.firstName());
            v.setApellido(d.lastName());
            v.setCategoria(mapaCategorias.get(d.categoryId()));
            v.setHabilitado(d.enabled());
            v.setFechaCreacion(d.createdAt());
            v.setFechaActualizacion(d.updatedAt());
            mapaProfesionales.put(d.id(), profesionales.save(v));
        }
        profesionales.flush();
        for (var d : snapshot.weeklySchedules()) {
            HorarioSemanal h = new HorarioSemanal();
            h.setId(d.id());
            h.setProfesional(mapaProfesionales.get(d.professionalId()));
            h.setDiaSemana(d.dayOfWeek());
            h.setHoraInicio(d.startTime());
            h.setHoraFin(d.endTime());
            h.setDuracionTurnoMinutos(d.slotDurationMinutes());
            h.setHabilitado(d.enabled());
            h.setFechaCreacion(d.createdAt());
            h.setFechaActualizacion(d.updatedAt());
            horarios.save(h);
        }
        horarios.flush();
        estado.setUltimoError(null);
        estado.setVersion(snapshot.snapshotVersion());
        estado.setSincronizadoEn(Instant.now());
        estados.saveAndFlush(estado);
        return estado.getVersion();
    }
}
