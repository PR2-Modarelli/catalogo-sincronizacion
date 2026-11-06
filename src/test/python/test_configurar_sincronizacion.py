import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[3] / 'scripts' / 'configurar-sincronizacion.py'
spec = importlib.util.spec_from_file_location('configuracion', SCRIPT)
modulo = importlib.util.module_from_spec(spec)
spec.loader.exec_module(modulo)


class ConfiguracionTest(unittest.TestCase):
    def setUp(self):
        self.carpeta = tempfile.TemporaryDirectory()
        self.addCleanup(self.carpeta.cleanup)
        self.raiz = Path(self.carpeta.name)
        self.env = self.raiz / '.env'
        self.original = 'CATALOGO_JWT_SECRET=clave-local\nCATALOGO_CATEDRA_BASE_URL=http://catedra.test\nCATALOGO_CATEDRA_TOKEN=token-prueba\nOTRA_VARIABLE=conservar\n'
        self.env.write_text(self.original)
        self.integracion = self.raiz / 'integracion.json'
        self.datos = dict(provisioningStatus='PROVISIONED', redisHost='redis.test', redisPort=6379,
                          redisUsername='usuario', redisPassword="clave con espacios y 'comillas'",
                          kafkaBootstrapServers='kafka.test:9094', kafkaCatalogTopic='catalogo-prueba', kafkaConsumerGroupId='grupo-prueba')

    def guardar(self):
        self.integracion.write_text(json.dumps({'integration': self.datos}))

    def test_conserva_jwt_y_otras_variables_y_es_idempotente(self):
        self.guardar()
        modulo.configurar(self.integracion, self.env)
        primera = self.env.read_text()
        self.assertIn(self.original, primera)
        self.assertIn('CATALOGO_SYNC_ENABLED=true', primera)
        modulo.configurar(self.integracion, self.env)
        self.assertEqual(self.env.read_text().count('CATALOGO_REDIS_PASSWORD='), 1)
        self.assertEqual(os.stat(self.env).st_mode & 0o777, 0o600)

    def test_campo_ausente_no_toca_el_entorno(self):
        del self.datos['redisPassword']
        self.guardar()
        with self.assertRaises(ValueError):
            modulo.configurar(self.integracion, self.env)
        self.assertEqual(self.env.read_text(), self.original)

    def test_estado_no_provisionado_no_toca_el_entorno(self):
        self.datos['provisioningStatus'] = 'PENDING'
        self.guardar()
        with self.assertRaises(ValueError):
            modulo.configurar(self.integracion, self.env)
        self.assertEqual(self.env.read_text(), self.original)

    def test_puerto_invalido_no_toca_el_entorno(self):
        self.datos['redisPort'] = 70000
        self.guardar()
        with self.assertRaises(ValueError):
            modulo.configurar(self.integracion, self.env)
        self.assertEqual(self.env.read_text(), self.original)
