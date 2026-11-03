package ar.com.modarelli.catalogo.repository;

import ar.com.modarelli.catalogo.IntegrationTest;
import ar.com.modarelli.catalogo.domain.Categoria;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class CategoriaRepositoryIT {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void debeGuardarYRecuperarUnaCategoria() {
        Instant fecha = Instant.parse("2026-09-10T13:40:00Z");

        Categoria categoria = new Categoria();
        categoria.setId(900001L);
        categoria.setNombre("Clínica médica");
        categoria.setDescripcion("Atención clínica general");
        categoria.setHabilitada(true);
        categoria.setFechaCreacion(fecha);
        categoria.setFechaActualizacion(fecha);

        categoriaRepository.saveAndFlush(categoria);

        entityManager.clear();

        Categoria recuperada = categoriaRepository
            .findById(900001L)
            .orElseThrow();

        assertThat(recuperada.getId()).isEqualTo(900001L);
        assertThat(recuperada.getNombre()).isEqualTo("Clínica médica");
        assertThat(recuperada.getDescripcion())
            .isEqualTo("Atención clínica general");
        assertThat(recuperada.isHabilitada()).isTrue();
        assertThat(recuperada.getFechaCreacion()).isEqualTo(fecha);
        assertThat(recuperada.getFechaActualizacion()).isEqualTo(fecha);
    }
}