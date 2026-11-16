package ar.com.modarelli.catalogo.domain.ports.in;

import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo;

public interface AplicarSnapshotUseCase {
    Long aplicar(SnapshotCatalogo snapshot);
}
