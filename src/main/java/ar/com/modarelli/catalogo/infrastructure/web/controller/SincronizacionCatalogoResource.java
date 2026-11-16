package ar.com.modarelli.catalogo.infrastructure.web.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/catalogo")
public class SincronizacionCatalogoResource {
    private final ar.com.modarelli.catalogo.domain.ports.in.SincronizarCatalogoUseCase incremental;

    public SincronizacionCatalogoResource(
            ar.com.modarelli.catalogo.domain.ports.in.SincronizarCatalogoUseCase incremental) {
        this.incremental = incremental;
    }

    @PostMapping("/sincronizacion-incremental")
    public Resultado incremental() {
        return new Resultado(incremental.sincronizar());
    }

    public record Resultado(Long version) {}

    @PostMapping("/sincronizacion-completa")
    public Resultado sincronizar() {
        return new Resultado(incremental.completa());
    }
}
