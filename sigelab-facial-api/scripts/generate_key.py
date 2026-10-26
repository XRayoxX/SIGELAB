"""Genera los secretos que necesita la API.

    python -m scripts.generate_key

FACIAL_ENCRYPTION_KEY debe guardarse y NO cambiarse después de registrar
rostros: si cambia, los perfiles existentes ya no se pueden descifrar.
"""
import base64
import secrets


if __name__ == "__main__":
    print("FACIAL_ENCRYPTION_KEY=" + base64.b64encode(secrets.token_bytes(32)).decode("ascii"))
    print("API_KEYS=" + secrets.token_urlsafe(32))
