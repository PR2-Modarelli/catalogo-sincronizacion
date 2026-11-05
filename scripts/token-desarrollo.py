#!/usr/bin/env python3
"""Token administrativo local de 15 minutos para demostrar el backend sin usuarios.
No usa ni genera credenciales de catedra. No usar como mecanismo de login final.
"""
import base64
import hashlib
import hmac
import json
import os
import sys
import time

def encode(value):
    return base64.urlsafe_b64encode(value).rstrip(b"=")

secret = os.environ.get("CATALOGO_JWT_SECRET", "")
try:
    key = base64.b64decode(secret, validate=True)
    if len(key) < 64:
        raise ValueError()
except ValueError:
    sys.exit("Configurar CATALOGO_JWT_SECRET en Base64 con al menos 64 bytes.")
now = int(time.time())
header = encode(json.dumps({"alg": "HS512", "typ": "JWT"}).encode())
claims = encode(json.dumps({"sub": "demo-local", "auth": "ROLE_ADMIN", "iat": now, "exp": now + 900}).encode())
unsigned = header + b"." + claims
signature = encode(hmac.new(key, unsigned, hashlib.sha512).digest())
print((unsigned + b"." + signature).decode())
