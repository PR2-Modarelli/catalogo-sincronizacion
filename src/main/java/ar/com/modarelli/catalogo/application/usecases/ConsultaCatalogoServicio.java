package ar.com.modarelli.catalogo.application.usecases;

import ar.com.modarelli.catalogo.domain.model.CatalogoVistas.*;
import ar.com.modarelli.catalogo.domain.ports.in.ConsultarCatalogoUseCase;
import ar.com.modarelli.catalogo.domain.ports.out.ConsultaCatalogoPort;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class ConsultaCatalogoServicio implements ConsultarCatalogoUseCase {
    private final ConsultaCatalogoPort consulta;

    public ConsultaCatalogoServicio(ConsultaCatalogoPort consulta) {
        this.consulta = consulta;
    }

    public EstadoVista estado() {
        return consulta.estado();
    }

    public List<CategoriaVista> categorias() {
        return consulta.categorias();
    }

    public PaginaProfesionales buscar(
            String nombre,
            Long categoriaId,
            Boolean habilitado,
            Boolean conAgenda,
            DayOfWeek dia,
            int pagina,
            int tamanio) {
        if (categoriaId != null && categoriaId <= 0)
            throw new ar.com.modarelli.catalogo.domain.exception.ErrorCatalogo(
                    400, "La categoria debe tener un ID positivo");
        if (pagina < 0
                || tamanio < 1
                || tamanio > 100
                || (long) pagina * tamanio > Integer.MAX_VALUE)
            throw new ar.com.modarelli.catalogo.domain.exception.ErrorCatalogo(
                    400, "Pagina o tamanio invalido");
        return consulta.buscar(nombre, categoriaId, habilitado, conAgenda, dia, pagina, tamanio);
    }

    public List<HorarioVista> horarios(Long id) {
        return consulta.horarios(id);
    }

    public AgendaVista agenda(Long id) {
        return consulta.agenda(id);
    }
}
