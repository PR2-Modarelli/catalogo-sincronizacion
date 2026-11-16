package ar.com.modarelli.catalogo.infrastructure.persistence.repository;

import ar.com.modarelli.catalogo.infrastructure.persistence.entity.EventoCatalogoProcesado;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoCatalogoProcesadoRepository
        extends JpaRepository<EventoCatalogoProcesado, String> {}
