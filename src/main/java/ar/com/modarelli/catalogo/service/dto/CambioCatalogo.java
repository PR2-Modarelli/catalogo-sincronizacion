package ar.com.modarelli.catalogo.service.dto;

import ar.com.modarelli.catalogo.service.dto.SnapshotCatalogo.*;
import java.util.List;

/** Entidades afectadas en una unica version, no un snapshot ni una lista de bajas fisicas. */
public record CambioCatalogo(Long versionBase, Long versionDestino, List<CategoriaDato> categorias,
    List<ProfesionalDato> profesionales, List<HorarioDato> horarios) {}
