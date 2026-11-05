# Contratos propios del servicio de catálogo

Todos los endpoints listados exigen un JWT propio de la aplicación válido. El token técnico de cátedra no autentica a usuarios de esta API. El endpoint de salud permanece público por la configuración de JHipster.

## Consulta

### GET /api/catalogo/estado

Devuelve version (null antes de inicializar), sincronizadoEn, inicializado y cantidades de categorias, profesionales y horarios. La lectura usa una transacción con aislamiento REPEATABLE_READ para no mezclar datos de dos snapshots.

### GET /api/catalogo/categorias

Lista categorías locales, incluidas deshabilitadas, ordenadas por nombre e ID. Campos: id, nombre, descripcion, habilitada. Devuelve 503 si no hay un snapshot aplicado.

### GET /api/catalogo/profesionales

| Parámetro opcional | Significado |
| --- | --- |
| nombre | Parte de nombre o apellido, sin distinguir mayúsculas. Espacios exteriores se quitan. %, _ y ! se buscan literalmente. No normaliza tildes. |
| categoriaId | ID exacto de categoría |
| habilitado | true o false; ausente no filtra |
| conAgenda | true exige al menos una regla habilitada; false exige que no exista |
| diaSemana | MONDAY a SUNDAY. Restringe las reglas consideradas. Sin conAgenda equivale a exigir agenda ese día. |
| pagina | Índice desde 0; por defecto 0 |
| tamanio | 1 a 100; por defecto 20 |

Filtros combinables mediante AND. Las reglas para agenda deben estar habilitadas, pero el estado del profesional se controla con habilitado. No hay exclusión implícita de categorías deshabilitadas.

Respuesta: contenido (id, nombre, apellido, categoriaId, categoria, habilitado), total, pagina, tamanio. Orden: apellido, nombre, id. Cero coincidencias devuelve 200 con contenido vacío. Parámetros inválidos devuelven 400. Antes de sincronizar, 503.

**Este filtro de agenda no verifica ocupaciones: no es disponibilidad real de turnos.**

### GET /api/catalogo/profesionales/{id}/horarios

Devuelve reglas semanales del profesional, incluidas deshabilitadas. Campos: id, profesionalId, diaSemana, horaInicio, horaFin, duracionTurnoMinutos, habilitado. ID inexistente: 404; catálogo sin inicializar: 503.

## Sincronización administrativa

### POST /api/admin/catalogo/sincronizacion-completa

Sin body. Requiere ROLE_ADMIN además de JWT válido. Descarga GET /api/synchronization/snapshot de la cátedra, con su JWT técnico configurado, y devuelve {"version":7} al aplicar.

Flujo:

1. Descarga fuera de la transacción de escritura, con tiempos máximos acotados.
2. Valida campos obligatorios, colecciones presentes, IDs únicos, relaciones y horarios.
3. Bloquea la fila única de estado y rechaza una versión anterior a la local.
4. Borra horarios, profesionales y categorías; recrea en orden inverso para respetar referencias.
5. Guarda la versión exacta y la fecha local de aplicación en la misma transacción.
6. Si falla una escritura, PostgreSQL revierte todo, incluidos borrados y versión.

Un snapshot vacío con las tres listas presentes es válido. Reaplicar la misma versión reconstruye la copia y no duplica entidades. Se preservan entidades deshabilitadas. Campos JSON nuevos compatibles se ignoran; horas HH:mm y HH:mm:ss se aceptan.

| Estado HTTP | Caso |
| --- | --- |
| 401 | Sin autenticación válida |
| 403 | Usuario autenticado sin ROLE_ADMIN |
| 409 | Snapshot más antiguo que la versión aplicada |
| 502 | Fallo HTTP externo, cuerpo incompleto o validaciones de snapshot |
| 503 | Falta URL o JWT técnico de cátedra |

Errores inesperados de persistencia se propagan como error del servidor y revierten la transacción. Las respuestas externas y los secretos no se incluyen en el mensaje del cliente HTTP. Aún no se define un code funcional propio para cada error: pendiente para estabilizar el contrato entre servicios.

## Propiedad de datos

Las tablas categoria, profesional, horario_semanal y estado_catalogo pertenecen exclusivamente a este servicio. El backend de turnos deberá usar sus APIs; no leer estas tablas ni crear una réplica de búsqueda propia.

Los IDs de catálogo son asignados por la cátedra. Las fechas de creación y actualización vienen del snapshot; sincronizadoEn registra la aplicación local. No se crean categorías, profesionales ni horarios mediante endpoints públicos de escritura.
