# Sincronización del catálogo

## Datos y responsabilidades

PostgreSQL conserva la copia local, la versión aplicada y los identificadores de eventos procesados. Redis es una fuente externa de lectura del catálogo publicado por la cátedra; no reemplaza la persistencia local. El consumidor escucha únicamente el topic configurado de catálogo. El servicio de turnos accede al catálogo mediante la API propia, sin leer estas tablas.

## Inicio y revisión periódica

Con `CATALOGO_SYNC_ENABLED=true`, se realiza una revisión inicial a los cinco segundos y revisiones posteriores con intervalo configurable, de 60 segundos por defecto. Permiten recuperar cambios aunque no haya llegado una notificación Kafka. Sin configuración habilitada no se abren conexiones a Redis ni al consumidor contractual.

Si no existe versión local, se descarga un snapshot REST. Si existe, se compara contra `currentVersion` y `oldestAvailableVersion` del hash `catedra:sync:metadata`. Si falta historia, una entidad o una versión, se reconstruye desde snapshot. Un snapshot más antiguo que la versión requerida no se considera recuperación exitosa.

## Lectura consistente y avance de versión

Los incrementos contienen IDs, mientras que los hashes contienen entidades vigentes. El lector recorre las versiones consecutivas pendientes, obtiene sus entidades y vuelve a consultar metadata. Si cambió durante la lectura, repite el recorrido hasta tres veces. Si no obtiene una lectura estable, recurre al snapshot.

Cuando hay varias versiones pendientes, se acumulan los cambios y se valida su continuidad. El resultado completo se guarda en una transacción, bajo el mismo bloqueo de estado que utiliza el snapshot. Se valida la combinación de datos locales y recibidos, se actualizan primero las categorías, luego profesionales y horarios y finalmente la versión. Las versiones intermedias no quedan visibles con entidades posteriores. Las entidades no afectadas y las bajas lógicas se conservan.

Un rango de más de 1000 versiones se reconstruye por snapshot para acotar el trabajo de una revisión. La versión publicada localmente debe corresponder al rango completo aplicado, nunca al aviso que simplemente se recibió.

## Kafka e idempotencia

El evento se valida como `CatalogUpdated`, esquema 1. Su key debe coincidir con `newVersion`; los campos nuevos compatibles se ignoran. El identificador real `eventId` se guarda junto con el efecto y la versión en PostgreSQL. Un duplicado no vuelve a escribir datos. Reutilizar el mismo identificador con otra versión es un error.

Un aviso tardío, cuya versión ya se obtuvo mediante snapshot o revisión periódica, se registra sin retroceder la copia. No se inventan eventos para las versiones intermedias recuperadas por revisión.

El consumidor usa `enable.auto.commit=false` y confirmación por registro. Solo retorna correctamente después del commit de la base de datos. Ante error se detiene el consumidor sin confirmar el registro fallido. Cuando la siguiente revisión programada consigue recuperar el catálogo, reinicia el consumidor; el registro pendiente se entrega otra vez y se resuelve de forma idempotente. Un mensaje inválido persistente conserva su offset pendiente y requiere revisar el contrato o el mensaje; no se descarta silenciosamente ni se envía a un destino inventado.

## Fallos y observabilidad

Las lecturas de Redis y REST tienen tiempos máximos. La lectura de Redis y la descarga HTTP ocurren fuera de la transacción de escritura. Si otro sincronizador guardó primero, se relee el estado con un máximo de tres intentos por ejecución. Las revisiones programadas separan nuevos intentos por el intervalo configurado.

Una escritura fallida revierte entidades, versión y registro del evento. Se conserva el catálogo anterior. El código `SINCRONIZACION_NO_DISPONIBLE` se persiste en una transacción independiente y se muestra como `ultimoError`; una revisión o snapshot exitoso lo limpia. No se guardan contraseñas, JWT, URLs privadas ni respuestas externas dentro de ese código.

## Pruebas

`bash scripts/verificar-catalogo.sh` ejecuta validaciones, lecturas con fuente controlada, persistencia y rollback en PostgreSQL, recuperación por snapshot, un cliente contra Redis autenticado de Testcontainers y entrega Kafka con un fallo seguido de reinicio. Los tests de arranque comprueban que las conexiones esperan la revisión inicial. Las credenciales y los contenedores utilizados son de prueba.
