package ar.com.modarelli.catalogo.infrastructure.persistence.repository;

import ar.com.modarelli.catalogo.infrastructure.persistence.entity.Profesional;

import org.springframework.data.jpa.repository.*;

public interface ProfesionalRepository
        extends JpaRepository<Profesional, Long>, JpaSpecificationExecutor<Profesional> {}
