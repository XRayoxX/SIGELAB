import base64
import os
import sys
from pathlib import Path

# Configuración de prueba ANTES de importar app.config.
os.environ.setdefault("FACIAL_ENCRYPTION_KEY", base64.b64encode(b"k" * 32).decode())
os.environ.setdefault("API_KEYS", "clave-de-prueba")
os.environ.setdefault("DUPLICATE_CHECK", "true")
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import numpy as np  # noqa: E402
import pytest  # noqa: E402

from app.config import Settings  # noqa: E402
from app.engine import FrameAnalysis  # noqa: E402


class FakeDatabase:
    def ping(self):
        return True


class FakeRepository:
    def __init__(self):
        self.database = FakeDatabase()
        self.users = {
            1: {"id_usuario": 1, "usuario": "admin", "rol": "1", "activo": True},
            2: {"id_usuario": 2, "usuario": "estudiante", "rol": "2", "activo": True},
            3: {"id_usuario": 3, "usuario": "inactivo", "rol": "ESTUDIANTE", "activo": False},
        }
        self.profiles = {}
        self.audit = []

    def get_user(self, user_id):
        return self.users.get(user_id)

    def get_profile(self, user_id):
        return self.profiles.get(user_id)

    def all_profiles(self):
        return [{"id_usuario": k, "embedding": v["embedding"], "dimension": v["dimension"]} for k, v in self.profiles.items()]

    def save_profile(self, user_id, embedding, model, dimension, samples, quality, version):
        self.profiles[user_id] = {
            "embedding": embedding, "modelo": model, "dimension": dimension, "cantidad_muestras": samples,
            "fecha_registro": None, "fecha_actualizacion": None,
        }

    def delete_profile(self, user_id):
        return self.profiles.pop(user_id, None) is not None

    def write_audit(self, *args):
        self.audit.append(args)


def person(seed: int, noise: float = 0.0, rng_seed: int = 0) -> np.ndarray:
    base = np.random.default_rng(seed).normal(size=512)
    base = base / np.linalg.norm(base)
    if noise:
        base = base + np.random.default_rng(rng_seed + 1000).normal(scale=noise / np.sqrt(512), size=512)
    return (base / np.linalg.norm(base)).astype(np.float32)


class FakeEngine:
    """Cada 'imagen' es un texto como b'2:front' (persona 2 mirando al frente)."""

    model_loaded = True
    yaw_by_pose = {"front": 0.0, "left": 20.0, "right": -20.0, "partial": 11.0}

    def analyze(self, frame, index):
        text = bytes(frame).decode()
        if text == "none":
            return FrameAnalysis(index, 0, False, "No se detectó ningún rostro.")
        who, pose = text.split(":")
        yaw = self.yaw_by_pose[pose]
        return FrameAnalysis(index, 1, True, "Rostro válido.", yaw, 0.0, pose, 0.9, person(int(who), 0.6, index))


@pytest.fixture(autouse=True)
def no_real_model(monkeypatch):
    from app import main

    monkeypatch.setattr(main, "FaceEngine", lambda s: FakeEngine())


@pytest.fixture
def settings():
    return Settings.from_env()


@pytest.fixture
def service(settings, monkeypatch):
    from app import service as service_module

    # Las "imágenes" de prueba no se decodifican: pasan directo al FakeEngine.
    monkeypatch.setattr(service_module, "decode_image", lambda data, s, i, m: np.frombuffer(data, dtype=np.uint8))
    return service_module.FaceService(FakeRepository(), FakeEngine(), settings)
