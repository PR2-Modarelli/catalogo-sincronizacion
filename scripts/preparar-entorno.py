#!/usr/bin/env python3
"""Crea .env local con clave propia; nunca sobrescribe una configuracion existente."""
import base64
import os
from pathlib import Path
import secrets

root = Path(__file__).resolve().parent.parent
path = root / ".env"
if path.exists():
    print(".env ya existe; no se modifico. Comprobar sus variables antes de arrancar.")
else:
    key = base64.b64encode(secrets.token_bytes(64)).decode()
    content = (
        "CATALOGO_JWT_SECRET=" + key + "\n"
        "CATALOGO_CATEDRA_BASE_URL=\n"
        "CATALOGO_CATEDRA_TOKEN=\n"
        "CATALOGO_DB_USER=catalogo\n"
        "CATALOGO_DB_PASSWORD=\n"
    )
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as output:
        output.write(content)
    print(".env creado con permisos privados. No compartir ni subir este archivo.")
