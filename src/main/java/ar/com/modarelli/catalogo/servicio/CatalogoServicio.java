package ar.com.modarelli.catalogo.servicio;

import ar.com.modarelli.catalogo.modelo.Profesional;

import java.util.ArrayList;
import java.util.List;

public class CatalogoServicio {

    public List<Profesional> buscarProfesionales(
        List<Profesional> profesionales,
        String nombre,
        Long categoriaId,
        Boolean habilitado
    ) {
        List<Profesional> resultados = new ArrayList<>();

        for (Profesional profesional : profesionales) {

            boolean coincideNombre = nombre == null
                || profesional.getNombre().equalsIgnoreCase(nombre);

            boolean coincideCategoria = categoriaId == null
                || profesional.getCategoria().getId().equals(categoriaId);

            boolean coincideEstado = habilitado == null
                || profesional.isHabilitado() == habilitado;

            if (coincideNombre && coincideCategoria && coincideEstado) {
                resultados.add(profesional);
            }
        }

        return resultados;
    }
}