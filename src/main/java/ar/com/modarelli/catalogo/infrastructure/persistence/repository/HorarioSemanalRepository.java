package ar.com.modarelli.catalogo.infrastructure.persistence.repository;

import ar.com.modarelli.catalogo.infrastructure.persistence.entity.HorarioSemanal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioSemanalRepository extends JpaRepository<HorarioSemanal, Long> {
    List<HorarioSemanal> findByProfesionalIdOrderByDiaSemanaAscHoraInicioAsc(Long profesionalId);
}
