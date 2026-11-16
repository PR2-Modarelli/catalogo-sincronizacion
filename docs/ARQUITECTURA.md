# Organización y arquitectura

La estructura sigue el ejemplo docente `dqmdz/hexagonal` (archivo de referencia `hexagonal-main.zip`, commit `6def11da98237b34312b3a5de201e01c351c4644`). Cada backend constituye un contexto independiente y conserva su propia base y configuración.

```text
ar/com/modarelli/catalogo/
  domain/
    model/           Modelos propios e información de los procesos
    ports/in/        Interfaces de los casos de uso
    ports/out/       Interfaces de persistencia e integración
    service/         Reglas que trabajan con modelos propios
    exception/       Errores propios, sin dependencias de Spring
  application/
    usecases/        Coordinación de los casos de uso a través de puertos
  infrastructure/
    web/             Controladores, validación HTTP y traducción de errores
    persistence/     Adaptadores de almacenamiento y mapeo
    http/, redis/, kafka/, scheduling/, config/, security/
```

## Dirección de las dependencias

Los modelos y puertos del dominio no importan Spring, JPA, SQL, Jackson ni implementaciones de infraestructura. Los casos de uso dependen de esos puertos. Los adaptadores implementan puertos de salida o llaman puertos de entrada; la inyección de Spring conecta las implementaciones.

Como en el ejemplo docente, la aplicación usa anotaciones de Spring para registrar servicios y delimitar transacciones cuando es necesario. Eso no introduce JWT, consultas SQL, entidades JPA ni serialización JSON en los modelos o contratos del dominio. No se requiere un módulo Maven por capa para mantener estas reglas.

El controlador valida el formato de una solicitud y la adapta a los modelos de entrada. Las reglas de negocio y la coordinación viven en dominio/aplicación; HTTP, credenciales, representación JSON y acceso a datos se resuelven en infraestructura. Las entidades de persistencia no se usan como modelos de los casos de uso.

## Comprobación de los límites

Las reglas de ArchUnit verifican que:

- El dominio no dependa de aplicación, infraestructura ni frameworks.
- La aplicación no dependa de adaptadores, JDBC/JPA, HTTP, JWT o Jackson.
- Los controladores y tareas programadas no dependan de implementaciones concretas de casos de uso.
- Los tipos superiores de los paquetes de puertos sean interfaces.

Las pruebas funcionales e integración siguen verificando el comportamiento público y los contratos de persistencia. Las pruebas usan recursos aislados de la cátedra.

## Recorridos de catálogo

| Operación | Entrada | Aplicación | Puertos de salida / adaptadores |
| --- | --- | --- | --- |
| Consultar | `ConsultaCatalogoResource` → `ConsultarCatalogoUseCase` | `ConsultaCatalogoServicio` | `ConsultaCatalogoPort` → `JpaConsultaCatalogoAdapter` |
| Snapshot completo | administración → `SincronizarCatalogoUseCase.completa` | `SincronizacionIncrementalServicio`, `SnapshotCatalogoServicio` | `DescargarSnapshotPort` → `CatedraSnapshotCliente`; `SnapshotCatalogoPort` → `JpaSnapshotCatalogoAdapter` |
| Cambios incrementales | `CatalogUpdatedConsumidor` o `RevisionCatalogoProgramada` → `SincronizarCatalogoUseCase` | `SincronizacionIncrementalServicio` | `PlanSincronizacionPort` → `LecturaIncrementalRedis`; `PersistenciaCatalogoPort` → `JpaIncrementalCatalogoAdapter` |

`ValidacionCatalogo` valida snapshots y referencias entre categorías, profesionales y horarios. `ValidacionIncremental` valida cambios y eventos. `RangoCatalogo` combina rangos consecutivos utilizando modelos propios. Las garantías de versión, bloqueo y actualización atómica se implementan en las transacciones de los adaptadores de persistencia.

Las entidades JPA están en `infrastructure.persistence.entity`; los repositorios Spring Data en `infrastructure.persistence.repository`. `domain.model` contiene snapshots, cambios, eventos, planes y vistas independientes de Hibernate. El escaneo de repositorios y las referencias de configuración se actualizaron sin cambiar tablas ni migraciones.

La compatibilidad JSON se configura en los adaptadores HTTP, Redis y Kafka. Los errores propios se convierten a HTTP en `CatalogoErrorHandler`.

Las funciones Kafka/SSE de ejemplo generadas por JHipster se mantienen como infraestructura y respetan `spring.cloud.stream.enabled`. Con la integración real desactivando ese ejemplo, no deben intentar conectar al broker local. La prueba de esas funciones utiliza el binder de prueba, separado del consumidor de avisos de la cátedra.
