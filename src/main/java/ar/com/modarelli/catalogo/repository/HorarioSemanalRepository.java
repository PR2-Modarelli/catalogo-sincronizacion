package ar.com.modarelli.catalogo.repository;
import ar.com.modarelli.catalogo.domain.HorarioSemanal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HorarioSemanalRepository extends JpaRepository<HorarioSemanal, Long> {
 List<HorarioSemanal> findByProfesionalIdOrderByDiaSemanaAscHoraInicioAsc(Long profesionalId);
}
