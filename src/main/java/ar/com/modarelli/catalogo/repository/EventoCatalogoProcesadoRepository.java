package ar.com.modarelli.catalogo.repository;

import ar.com.modarelli.catalogo.domain.EventoCatalogoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoCatalogoProcesadoRepository extends JpaRepository<EventoCatalogoProcesado, String> {}
