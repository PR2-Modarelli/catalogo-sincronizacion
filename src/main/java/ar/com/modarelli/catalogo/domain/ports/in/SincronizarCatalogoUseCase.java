package ar.com.modarelli.catalogo.domain.ports.in;

import ar.com.modarelli.catalogo.domain.model.EventoCatalogo;

public interface SincronizarCatalogoUseCase {
    Long sincronizar();

    Long procesar(EventoCatalogo evento);

    Long completa();
}
