package ar.com.modarelli.catalogo.domain.ports.in;

import ar.com.modarelli.catalogo.domain.model.CatalogoVistas.*;

import java.time.*;
import java.util.*;

public interface ConsultarCatalogoUseCase {
    EstadoVista estado();

    List<CategoriaVista> categorias();

    PaginaProfesionales buscar(
            String nombre,
            Long categoriaId,
            Boolean habilitado,
            Boolean conAgenda,
            DayOfWeek dia,
            int pagina,
            int tamanio);

    List<HorarioVista> horarios(Long id);

    AgendaVista agenda(Long id);
}
