#!/usr/bin/env python3
"""Completa solo las variables de sincronizacion a partir de la respuesta privada de integration."""
import json
import os
from pathlib import Path
import shlex
import sys
import tempfile


def configurar(origen, destino):
    documento = json.loads(origen.read_text())
    datos = documento.get('integration', documento)
    campos = {
        'CATALOGO_REDIS_HOST': 'redisHost',
        'CATALOGO_REDIS_PORT': 'redisPort',
        'CATALOGO_REDIS_USERNAME': 'redisUsername',
        'CATALOGO_REDIS_PASSWORD': 'redisPassword',
        'CATALOGO_KAFKA_BOOTSTRAP_SERVERS': 'kafkaBootstrapServers',
        'CATALOGO_KAFKA_CATALOG_TOPIC': 'kafkaCatalogTopic',
        'CATALOGO_KAFKA_CONSUMER_GROUP': 'kafkaConsumerGroupId',
    }
    if datos.get('provisioningStatus') != 'PROVISIONED':
        raise ValueError('La cuenta tecnica no figura como PROVISIONED')
    valores = {}
    for variable, campo in campos.items():
        valor = datos.get(campo)
        if isinstance(valor, list):
            valor = ','.join(valor)
        if not isinstance(valor, (str, int)) or not str(valor).strip() or '\n' in str(valor) or '\r' in str(valor):
            raise ValueError('Falta un campo valido de integracion: ' + campo)
        valores[variable] = str(valor)
    puerto = int(valores['CATALOGO_REDIS_PORT'])
    if not 1 <= puerto <= 65535:
        raise ValueError('Puerto Redis invalido')
    if not destino.is_file():
        raise ValueError('Primero preparar el archivo .env con las claves JWT y REST existentes')
    lineas = destino.read_text().splitlines()
    claves = {line.split('=', 1)[0].strip().removeprefix('export ').strip() for line in lineas if '=' in line}
    if not {'CATALOGO_JWT_SECRET', 'CATALOGO_CATEDRA_BASE_URL', 'CATALOGO_CATEDRA_TOKEN'} <= claves:
        raise ValueError('Faltan las variables JWT o REST en .env')
    valores['CATALOGO_SYNC_ENABLED'] = 'true'
    valores['SPRING_CLOUD_STREAM_ENABLED'] = 'false'
    nuevas = [line for line in lineas if line.split('=', 1)[0].strip().removeprefix('export ').strip() not in valores]
    nuevas += [''] + [key + '=' + shlex.quote(value) for key, value in valores.items()]
    fd, nombre = tempfile.mkstemp(prefix='.env-sync-', dir=destino.parent)
    try:
        with os.fdopen(fd, 'w') as archivo:
            archivo.write('\n'.join(nuevas) + '\n')
        os.chmod(nombre, 0o600)
        os.replace(nombre, destino)
    finally:
        Path(nombre).unlink(missing_ok=True)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise SystemExit('Uso: python3 scripts/configurar-sincronizacion.py /ruta/privada/integracion-catedra.json')
    try:
        configurar(Path(sys.argv[1]).expanduser(), Path(__file__).resolve().parents[1] / '.env')
    except (ValueError, OSError, TypeError):
        raise SystemExit('No se modifico .env. Revisar el archivo privado, el estado PROVISIONED y sus campos de integracion.')
    print('Sincronizacion configurada en .env sin mostrar credenciales. Cargar el entorno y reiniciar el backend.')
