package ar.com.modarelli.catalogo.repository;
import ar.com.modarelli.catalogo.domain.EstadoCatalogo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface EstadoCatalogoRepository extends JpaRepository<EstadoCatalogo, Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select e from EstadoCatalogo e where e.id = 1")
 EstadoCatalogo bloquear();
}
