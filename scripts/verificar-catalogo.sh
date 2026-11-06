#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 -m unittest discover -s src/test/python -p 'test_*.py'
./mvnw -Duser.timezone=America/Argentina/Mendoza \
  -Dtest=CatedraSnapshotClienteTest,SnapshotValidacionTest,IncrementalValidacionTest,LecturaIncrementalRedisTest,CatalogUpdatedConsumidorTest,CatalogoPersistenciaIT,CatalogoIncrementalIT,SincronizacionRecuperacionIT,RedisCatalogoClienteIT,CatalogUpdatedConsumidorIT,ArranqueSincronizacionIT \
  test "$@"
