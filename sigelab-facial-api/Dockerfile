# SIGELAB Facial API - imagen para Azure Container Apps (linux/amd64)
#
#   docker build --platform linux/amd64 -t sigelab-facial-api .
#
# ---------- Etapa 1: compilar dependencias y descargar el modelo ----------
FROM python:3.12-slim-bookworm AS build

RUN apt-get update \
 && apt-get install -y --no-install-recommends build-essential \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /build
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# El modelo queda dentro de la imagen: el contenedor arranca sin descargar nada.
ENV PYTHONPATH=/install/lib/python3.12/site-packages \
    INSIGHTFACE_HOME=/models \
    FACE_MODEL=buffalo_l
COPY scripts/download_model.py ./download_model.py
RUN python download_model.py

# ---------- Etapa 2: imagen final ----------
FROM python:3.12-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    INSIGHTFACE_HOME=/models \
    FACE_MODEL=buffalo_l \
    PORT=8000

# Driver ODBC 18 de Microsoft para conectarse a Azure SQL.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl gnupg ca-certificates libgomp1 libglib2.0-0 libstdc++6 \
 && curl -fsSL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor -o /usr/share/keyrings/microsoft-prod.gpg \
 && curl -fsSL https://packages.microsoft.com/config/debian/12/prod.list -o /etc/apt/sources.list.d/mssql-release.list \
 && apt-get update \
 && ACCEPT_EULA=Y apt-get install -y --no-install-recommends msodbcsql18 unixodbc \
 && apt-get purge -y curl gnupg \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /install /usr/local
COPY --from=build /models /models

RUN useradd --create-home --uid 10001 appuser
WORKDIR /srv
COPY app ./app
COPY scripts ./scripts
COPY sql ./sql
RUN chown -R appuser /srv && chmod -R a+rX /models
USER appuser

EXPOSE 8000
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000} --workers 1 --proxy-headers --forwarded-allow-ips='*'"]
