package ar.com.modarelli.catalogo.application.usecases;

import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo;
import ar.com.modarelli.catalogo.domain.ports.in.AplicarSnapshotUseCase;
import ar.com.modarelli.catalogo.domain.ports.out.SnapshotCatalogoPort;
import ar.com.modarelli.catalogo.domain.service.ValidacionCatalogo;

import org.springframework.stereotype.Service;

@Service
public class SnapshotCatalogoServicio implements AplicarSnapshotUseCase {
    private final SnapshotCatalogoPort persistencia;

    public SnapshotCatalogoServicio(SnapshotCatalogoPort persistencia) {
        this.persistencia = persistencia;
    }

    public Long aplicar(SnapshotCatalogo snapshot) {
        ValidacionCatalogo.validar(snapshot);
        return persistencia.aplicar(snapshot);
    }
}
