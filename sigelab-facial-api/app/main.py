"""SIGELAB Facial API.

Endpoints (todos excepto /health requieren el header X-API-Key):

  GET    /health                          estado del servicio (sin tocar la base de datos)
  GET    /api/health/db                   prueba la conexión a Azure SQL
  GET    /api/face/challenge?userId=1     desafío aleatorio de prueba de vida
  POST   /api/face/analyze                revisa fotos (calidad y pose) sin guardar nada
  POST   /api/face/enroll                 registra el rostro de un usuario
  POST   /api/face/verify                 verifica que la persona sea el usuario indicado (1:1)
  POST   /api/face/identify               reconoce quién es entre todos los registrados (1:N)
  GET    /api/face/status/{userId}        indica si el usuario tiene rostro registrado
  DELETE /api/face/profile/{userId}?adminUserId=1   elimina el perfil facial
"""
from __future__ import annotations

import logging
import re
import threading
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, File, Form, Header, Query, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .config import settings
from .db import Database
from .engine import FaceEngine
from .errors import FacialServiceError
from .migrations import apply_migrations_safely
from .repository import FacialRepository
from .security import require_api_key
from .service import FaceService


logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    repository = FacialRepository(Database(settings), settings)
    try:
        engine = FaceEngine(settings)
    except FacialServiceError:
        logger.exception("El servicio inicia en estado degradado: modelo no disponible")
        engine = None
    if not settings.api_keys:
        logger.warning("API_KEYS está vacío: los endpoints protegidos responderán 503.")
    app.state.face_service = FaceService(repository, engine, settings)
    if settings.auto_migrate:
        # En segundo plano: si la base serverless está pausada no retrasa el arranque.
        threading.Thread(target=apply_migrations_safely, args=(settings,), daemon=True).start()
    yield


app = FastAPI(title="SIGELAB - Facial API", version="2.0.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

protected = [Depends(require_api_key)]


def service() -> FaceService:
    return app.state.face_service


def device_label(x_client: str | None) -> str:
    label = re.sub(r"[^A-Za-z0-9 ._:-]", "", x_client or "")[:80]
    return label or "api"


def read_images(images: list[UploadFile]) -> list[bytes]:
    return [image.file.read() for image in images]


@app.exception_handler(FacialServiceError)
async def facial_error_handler(_, exc: FacialServiceError):
    return JSONResponse(
        status_code=exc.status_code,
        content={"success": False, "detail": exc.message, "code": exc.code, **exc.extra},
        headers={"X-Facial-Code": exc.code},
    )


@app.exception_handler(Exception)
async def unexpected_error_handler(_, exc: Exception):
    logger.exception("Error no controlado")
    return JSONResponse(status_code=500, content={"success": False, "detail": "Error interno del servicio.", "code": "ERROR_INTERNO"})


@app.get("/health")
def health():
    return service().health()


@app.get("/api/health/db", dependencies=protected)
def health_db():
    ok = service().repository.database.ping()
    return JSONResponse(status_code=200 if ok else 503, content={"database": "ok" if ok else "unavailable"})


@app.get("/api/face/challenge", dependencies=protected)
def challenge(userId: int | None = Query(default=None, gt=0)):
    return service().challenges.issue(userId)


@app.post("/api/face/analyze", dependencies=protected)
def analyze(images: list[UploadFile] = File(...), mirrored: bool = Form(False)):
    return service().analyze(read_images(images), mirrored)


@app.post("/api/face/enroll", dependencies=protected)
def enroll(
    userId: int = Form(..., gt=0),
    images: list[UploadFile] = File(...),
    replace: bool = Form(False),
    mirrored: bool = Form(False),
    x_client: str | None = Header(default=None, alias="X-Client"),
):
    return service().enroll(userId, replace, read_images(images), mirrored, device_label(x_client))


@app.post("/api/face/verify", dependencies=protected)
def verify(
    userId: int = Form(..., gt=0),
    images: list[UploadFile] = File(...),
    challengeToken: str | None = Form(None),
    mirrored: bool = Form(False),
    x_client: str | None = Header(default=None, alias="X-Client"),
):
    return service().verify(userId, read_images(images), challengeToken, mirrored, device_label(x_client))


@app.post("/api/face/identify", dependencies=protected)
def identify(
    images: list[UploadFile] = File(...),
    challengeToken: str | None = Form(None),
    mirrored: bool = Form(False),
    x_client: str | None = Header(default=None, alias="X-Client"),
):
    return service().identify(read_images(images), challengeToken, mirrored, device_label(x_client))


@app.get("/api/face/status/{user_id}", dependencies=protected)
def face_status(user_id: int):
    return service().status(user_id)


@app.delete("/api/face/profile/{user_id}", dependencies=protected)
def face_delete(
    user_id: int,
    adminUserId: int = Query(..., gt=0),
    x_client: str | None = Header(default=None, alias="X-Client"),
):
    return service().delete_profile(user_id, adminUserId, device_label(x_client))
