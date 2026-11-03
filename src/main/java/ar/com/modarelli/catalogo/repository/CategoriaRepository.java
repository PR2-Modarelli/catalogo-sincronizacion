package ar.com.modarelli.catalogo.repository;

import ar.com.modarelli.catalogo.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}