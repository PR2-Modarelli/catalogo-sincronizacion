package ar.com.modarelli.catalogo.repository;
import ar.com.modarelli.catalogo.domain.Profesional;
import org.springframework.data.jpa.repository.*;
public interface ProfesionalRepository extends JpaRepository<Profesional, Long>, JpaSpecificationExecutor<Profesional> {}
