# Servicio de catálogo y sincronización

## Requisitos

- Java 21.
- Node.js >=24.21.0 y npm, según `package.json`.
- Docker con Docker Compose.
- Python 3 para preparar las variables de entorno.
- Redis y Kafka asignados por la cátedra para habilitar sincronización automática.
- Conectividad con la cátedra y cuenta técnica para la sincronización real.

## Configuración

Desde la raíz de este repositorio:

```bash
npm ci
python3 scripts/preparar-entorno.py
```

El script crea `.env` si no existe. Completar las variables de integración en ese archivo. Consultar `.env.example` como referencia. No incluir credenciales reales en archivos versionados.

| Variable | Descripción |
| --- | --- |
| `CATALOGO_JWT_SECRET` | Clave Base64 propia para los JWT de la aplicación, generada por el script de preparación. |
| `CATALOGO_CATEDRA_BASE_URL` | URL base REST de la cátedra, sin `/api` al final. |
| `CATALOGO_CATEDRA_TOKEN` | JWT técnico de la cátedra, sin el prefijo `Bearer`. |
| `CATALOGO_DB_USER` | Usuario de PostgreSQL. Valor local predeterminado: `catalogo`. |
| `CATALOGO_DB_PASSWORD` | Contraseña de PostgreSQL. El Compose local actual utiliza autenticación `trust`. |

El JWT técnico se utiliza únicamente para la integración del backend con la cátedra.

## Sincronización automática

Con la cuenta técnica provisionada, completar `.env` a partir del archivo privado que contiene la respuesta de registro o de integración:

```bash
python3 scripts/configurar-sincronizacion.py /ruta/privada/integracion-catedra.json
```

El script conserva las variables JWT y REST existentes, agrega Redis y Kafka, habilita `CATALOGO_SYNC_ENABLED` y desactiva las funciones Kafka de ejemplo mediante `SPRING_CLOUD_STREAM_ENABLED=false`. No muestra credenciales. Cargar el entorno y reiniciar el backend después de ejecutarlo.

| Variable | Descripción |
| --- | --- |
| `CATALOGO_SYNC_ENABLED` | `true` habilita revisión inicial, periódica y consumidor de catálogo; predeterminado `false`. |
| `CATALOGO_SYNC_INTERVAL_MS` | Intervalo entre revisiones; predeterminado `60000`. |
| `CATALOGO_REDIS_HOST`, `CATALOGO_REDIS_PORT` | Servidor Redis asignado. |
| `CATALOGO_REDIS_USERNAME`, `CATALOGO_REDIS_PASSWORD` | Credenciales Redis de la integración. |
| `CATALOGO_KAFKA_BOOTSTRAP_SERVERS` | Servidores Kafka asignados. |
| `CATALOGO_KAFKA_CATALOG_TOPIC` | Topic de catálogo asignado. |
| `CATALOGO_KAFKA_CONSUMER_GROUP` | Grupo de consumidores de la integración. |

## Ejecución local

Iniciar PostgreSQL y Kafka:

```bash
docker compose -f src/main/docker/postgresql.yml -f src/main/docker/kafka.yml up -d
```

Con la integración real habilitada, Kafka se utiliza en el servidor asignado por la cátedra; el contenedor Kafka local de ejemplo no interviene.

Cargar la configuración e iniciar el servicio desde Bash:

```bash
set -a
source .env
set +a
./mvnw
```

El backend escucha en `http://localhost:8081`. PostgreSQL utiliza el puerto local `5433` y Kafka el `9092`. Liquibase aplica las migraciones durante el inicio del backend.

Desde otra terminal, comprobar que responde:

```bash
curl --max-time 10 http://localhost:8081/management/health
```

Para descargar el catálogo completo, con el servicio iniciado, cargar el entorno en la segunda terminal y generar un JWT administrativo local de desarrollo:

```bash
set -a
source .env
set +a
export TOKEN_DEMO="$(python3 scripts/token-desarrollo.py)"
curl -i --max-time 30 -X POST \
  -H "Authorization: Bearer $TOKEN_DEMO" \
  http://localhost:8081/api/admin/catalogo/sincronizacion-completa
```

Este token local dura 15 minutos y es una herramienta de desarrollo. La descarga requiere las variables de integración configuradas. Para consultar el estado:

```bash
curl --max-time 10 -H "Authorization: Bearer $TOKEN_DEMO" \
  http://localhost:8081/api/catalogo/estado
```

Detener el backend con `Ctrl+C`. Para detener los contenedores:

```bash
docker compose -f src/main/docker/postgresql.yml -f src/main/docker/kafka.yml stop
```

La revisión automática comienza a los cinco segundos del arranque y luego se repite según el intervalo configurado. Se conserva la copia local ante errores. Para solicitar una revisión manual, con el JWT administrativo local:

```bash
curl -i --max-time 60 -X POST -H "Authorization: Bearer $TOKEN_DEMO" \
  http://localhost:8081/api/admin/catalogo/sincronizacion-incremental
```

## Pruebas automatizadas

Contrato HTTP y validaciones, sin Docker ni conexión con la cátedra:

```bash
./mvnw -Dtest=CatedraSnapshotClienteTest,SnapshotValidacionTest,IncrementalValidacionTest,LecturaIncrementalRedisTest,CatalogUpdatedConsumidorTest test
```

Persistencia y API con PostgreSQL de Testcontainers, con Docker disponible:

```bash
./mvnw -Duser.timezone=America/Argentina/Mendoza -Dtest=CategoriaRepositoryIT,CatalogoPersistenciaIT,CatalogoIncrementalIT test
```

Testcontainers utiliza una base de pruebas separada de la base de desarrollo.

Verificación del catálogo, incluyendo PostgreSQL, Redis y Kafka de Testcontainers y el script de configuración, con Docker disponible:

```bash
bash scripts/verificar-catalogo.sh
```

Las pruebas usan contenedores y credenciales de prueba; no requieren la conexión con la cátedra. Las pruebas de rollback y de fallo del consumidor generan errores intencionales en los logs. El resultado válido es cero fallos y cero errores en el resumen final.
