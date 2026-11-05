# Servicio de catálogo y sincronización

## Requisitos

- Java 21.
- Node.js >=24.21.0 y npm, según `package.json`.
- Docker con Docker Compose.
- Python 3 para preparar las variables de entorno.
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

## Ejecución local

Iniciar PostgreSQL y Kafka:

```bash
docker compose -f src/main/docker/postgresql.yml -f src/main/docker/kafka.yml up -d
```

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

## Pruebas automatizadas

Contrato HTTP y validaciones, sin Docker ni conexión con la cátedra:

```bash
./mvnw -Dtest=CatedraSnapshotClienteTest,SnapshotValidacionTest test
```

Persistencia y API con PostgreSQL de Testcontainers, con Docker disponible:

```bash
./mvnw -Duser.timezone=America/Argentina/Mendoza -Dtest=CategoriaRepositoryIT,CatalogoPersistenciaIT test
```

Testcontainers utiliza una base de pruebas separada de la base de desarrollo.
