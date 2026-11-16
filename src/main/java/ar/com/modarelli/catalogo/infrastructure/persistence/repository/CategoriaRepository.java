package ar.com.modarelli.catalogo.infrastructure.persistence.repository;

import ar.com.modarelli.catalogo.infrastructure.persistence.entity.Categoria;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {}
