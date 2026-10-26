# SIGELAB Facial API (Azure)

Versión en la nube del `facial-service` para usarla desde **la app de escritorio Java** y **la app móvil** de SIGELAB, conectada a la misma base **Azure SQL** (`sigelabumg` / `sigelabadministrador`).

## Qué cambió respecto al facial-service local

| facial-service (local) | sigelab-facial-api (Azure) |
|---|---|
| El servidor abre la webcam y una ventana OpenCV | **El cliente** (Java o celular) toma las fotos y las envía por HTTP |
| Solo escucha en `127.0.0.1` | API pública HTTPS protegida con **API key** (`X-API-Key`) |
| Prueba de vida mirando la ventana | **Desafío aleatorio firmado**: la API dice "gire a su izquierda/derecha" y valida las fotos |
| Solo verificación 1:1 | 1:1 (`verify`) y además **identificación 1:N** (`identify`) para marcar ingreso sin escribir usuario |
| SQL Server local (`UniversidadAsistenciaDB`) | Azure SQL, tabla de usuarios **configurable** |
| Pose estimada con solvePnP | Pose geométrica que no se confunde con la cabeza inclinada; izquierda/derecha **desde el punto de vista de la persona** |

Lo que se mantiene: InsightFace `buffalo_l` (ArcFace), los embeddings cifrados con AES-GCM en `PerfilFacial`, la auditoría en `AuditoriaFacial` y el umbral `0.55`. **No se guardan fotos**: solo viven en memoria durante la petición.

```
 App Java (webcam)  ─┐                      ┌──────────────────────────────┐
                     ├─ HTTPS + X-API-Key ─▶│ Azure Container Apps          │
 App móvil (cámara) ─┘   fotos JPEG         │  FastAPI + InsightFace (CPU) │──▶ Azure SQL
                                            └──────────────────────────────┘    PerfilFacial / AuditoriaFacial
```

## Estructura

```
app/            código de la API (FastAPI)
sql/            script de tablas para Azure SQL
scripts/        generate_key, migrate, download_model
tests/          pruebas (pytest)
deploy/         deploy-azure.ps1  → despliegue completo en Azure
clients/java/   FacialApiClient + ventana de captura para SIGELAB (Swing)
Dockerfile
```

---

## 1. Desplegar en Azure (Windows)

**Requisitos**
- Azure CLI: `winget install -e --id Microsoft.AzureCLI`
- Docker Desktop (recomendado). Las suscripciones con créditos gratuitos/estudiante suelen tener bloqueado `az acr build`, así que el script construye la imagen en su PC con Docker y la sube.
- Saber el usuario y la contraseña administrador de su servidor Azure SQL.

**Pasos**

```powershell
cd D:\Escritorio\universidadAsistenciaWithFaceRecon\universidadAsistencia\facial-service\sigelab-facial-api
powershell -ExecutionPolicy Bypass -File .\deploy\deploy-azure.ps1
```

El script:
1. Inicia sesión en Azure y registra los proveedores necesarios.
2. Encuentra `sigelabumg` y usa **la misma región** (menor latencia hacia la BD).
3. Activa en el firewall de SQL *"Permitir que los servicios de Azure accedan"*.
4. Crea un Container Registry, construye y sube la imagen (el modelo ya va dentro).
5. Crea la Container App (1 vCPU, 2 GiB, escala a cero) con los secretos.
6. Al arrancar, la API **crea sola las tablas** `PerfilFacial` y `AuditoriaFacial` (`AUTO_MIGRATE=true`).
7. Prueba `/health` y la conexión a la BD, y muestra la **URL** y la **API key**.

Los secretos quedan en `deploy\.secrets.local.json`. **Respalde `encryptionKey`**: si se pierde o cambia, los rostros registrados ya no se pueden verificar. No suba ese archivo a GitHub.

Ya viene configurado para su tabla `dbo.Usuario`: `IdUsuario`, `NombreUsuario`, `IdRol` y `Estado` (solo los usuarios con `Estado = 'ACTIVO'` pueden registrar o verificar su rostro). Los administradores, que son los únicos que pueden eliminar perfiles faciales, se indican por `IdRol` (por defecto `1`):

```powershell
.\deploy\deploy-azure.ps1 -AdminRoles "1"
```

La columna `RutaFotoRostro` no se usa: la API no guarda fotos, solo el vector facial cifrado en `PerfilFacial`.

**Después de cambiar el código Python:**

```powershell
.\deploy\deploy-azure.ps1 -SoloActualizarCodigo
```

**Día de la presentación** (evitar el arranque en frío):

```powershell
.\deploy\deploy-azure.ps1 -SoloEscalar -MinReplicas 1   # siempre encendida
.\deploy\deploy-azure.ps1 -SoloEscalar -MinReplicas 0   # volver a modo ahorro
```

**Ver logs:** `az containerapp logs show -n sigelab-facial-api -g rg-sigelab-facial --follow`

### Costos (Azure for Students)
- **Container Apps**: cada mes son gratis los primeros 180 000 vCPU-segundos, 360 000 GiB-segundos y 2 millones de peticiones. Con 1 vCPU y escala a cero equivale a unas **50 horas encendida al mes sin costo**. Con `MinReplicas 1` la réplica queda encendida y consume crédito todo el mes (del orden de USD 20–30 si pasa casi siempre inactiva); úselo solo cuando lo necesite.
- **Container Registry Basic**: unos USD 5/mes, descontados del crédito de estudiante.
- Con escala a cero, la **primera petición** después de un rato sin uso tarda ~30–60 s (Azure enciende el contenedor). Las apps llaman a `/health` al abrirse para "despertarla".
- La base serverless gratuita también se pausa sola; la API reintenta la conexión mientras se reanuda.

---

## 2. Endpoints

Todos menos `/health` requieren el header `X-API-Key`. Opcional: `X-Client` (ej. `sigelab-desktop`, `sigelab-android`), que se guarda en la auditoría. Documentación interactiva en `https://<su-url>/docs`.

| Método | Ruta | Uso |
|---|---|---|
| GET | `/health` | Estado (no toca la BD, así no la mantiene despierta) |
| GET | `/api/health/db` | Prueba la conexión a Azure SQL |
| GET | `/api/face/challenge?userId=5` | Desafío de prueba de vida (vence en 120 s, un solo uso) |
| POST | `/api/face/enroll` | Registrar rostro |
| POST | `/api/face/verify` | ¿Es el usuario X? (1:1) |
| POST | `/api/face/identify` | ¿Quién es? (1:N) |
| POST | `/api/face/analyze` | Revisa calidad y pose de fotos, sin guardar nada |
| GET | `/api/face/status/{userId}` | ¿Tiene rostro registrado? |
| DELETE | `/api/face/profile/{userId}?adminUserId=1` | Eliminar perfil (solo admin) |

Los POST son `multipart/form-data`, con una o varias fotos JPEG/PNG en el campo **`images`** (repetido).

**Registrar** — campos `userId`, `replace` (true para reemplazar), `mirrored`, e `images`:
mínimo 5 fotos válidas: al menos 2 de frente, 1 girando a la izquierda y 1 a la derecha. La API comprueba que todas sean de la misma persona y que ese rostro no esté registrado para otro usuario.

**Verificar / Identificar** — flujo con prueba de vida:
1. `GET /api/face/challenge` → `{ challengeToken, challenge: "turn_left"|"turn_right", instruction }`
2. Mostrar `instruction`, tomar 1–2 fotos de frente y 1–2 girando hacia ese lado.
3. `POST /api/face/verify` con `userId`, `challengeToken`, `mirrored` e `images` (o `/identify` sin `userId`).

Respuesta correcta de verify:
```json
{"success":true,"matched":true,"userId":5,"username":"jperez","score":0.7812,"threshold":0.55,
 "livenessPassed":true,"framesEvaluated":4,"message":"Identidad verificada correctamente.","frames":[...]}
```
Errores (siempre con `code` y `detail` para mostrar al usuario):
```json
{"success":false,"code":"LIVENESS_FALLIDO","detail":"No se detectó el giro de cabeza hacia su IZQUIERDA.",
 "frames":[{"index":1,"accepted":false,"reason":"Hay poca iluminación en el rostro.","pose":"front","yaw":2.1}, ...]}
```

| code | HTTP | Significado |
|---|---|---|
| `NO_AUTORIZADO` | 401 | Falta o es incorrecta la API key |
| `USUARIO_NO_EXISTE` / `USUARIO_INACTIVO` | 404 / 403 | |
| `PERFIL_NO_REGISTRADO` / `PERFIL_YA_EXISTE` | 404 / 409 | |
| `ROSTRO_YA_REGISTRADO` | 409 | Ese rostro ya pertenece a otro usuario |
| `CALIDAD_INSUFICIENTE` | 422 | Ninguna foto sirve (ver `frames[].reason`) |
| `CAPTURA_INCOMPLETA` / `PERSONAS_DISTINTAS` | 422 | Registro sin las poses requeridas o con fotos de otra persona |
| `DESAFIO_REQUERIDO` / `DESAFIO_VENCIDO` / `DESAFIO_USADO` / `DESAFIO_INVALIDO` | 400 | Pedir un desafío nuevo |
| `LIVENESS_FALLIDO` | 422 | No se vio la foto de frente o el giro pedido |
| `BD_NO_DISPONIBLE` / `MODELO_NO_DISPONIBLE` | 503 | Reintentar en unos segundos |

### Sobre `mirrored` (izquierda/derecha)
Izquierda y derecha se miden **desde la persona**. Si la cámara entrega la foto **en espejo** (pasa con la cámara frontal de muchos celulares), envíe `mirrored=true`. Webcam en Java (webcam-capture) → `mirrored=false`. Si la prueba de vida siempre dice *"Se detectó el giro hacia el lado contrario"*, cambie ese valor. Puede comprobarlo con `/api/face/analyze`: girando a su izquierda debe salir `"pose":"left"`.

### Probar desde la terminal (Windows trae curl.exe)
```powershell
$U="https://<su-url>"; $K="<su-api-key>"
curl.exe "$U/health"
curl.exe -H "X-API-Key: $K" "$U/api/health/db"
curl.exe -H "X-API-Key: $K" -F "images=@frente.jpg" -F "images=@izquierda.jpg" "$U/api/face/analyze"
```

---

## 3. App de escritorio Java (SIGELAB)

Archivos en `clients/java/sigelab/facial/`:
- `FacialApiClient.java`: cliente HTTP **sin dependencias** (Java 11+).
- `CapturaFacialDialog.java`: ventana Swing que abre la webcam, guía al usuario (frente / izquierda / derecha), toma las fotos y llama a la API.
- `EjemploSigelabFacial.java`: ejemplo con botones Registrar / Verificar / Marcar ingreso.

La ventana usa **webcam-capture 0.3.12** (`com.github.sarxos:webcam-capture:0.3.12`). En Maven:
```xml
<dependency>
  <groupId>com.github.sarxos</groupId>
  <artifactId>webcam-capture</artifactId>
  <version>0.3.12</version>
</dependency>
```
En un proyecto NetBeans con Ant, agregue a *Libraries* los jars `webcam-capture-0.3.12.jar`, `bridj-0.7.0.jar` y `slf4j-api-1.7.2.jar`.

Uso típico después del login con usuario/contraseña:
```java
FacialApiClient api = new FacialApiClient(URL_API, API_KEY, "sigelab-desktop");
FacialApiClient.Respuesta r = CapturaFacialDialog.verificar(this, api, usuario.getId());
if (r != null && r.esExitosa() && r.getBoolean("matched")) {
    abrirMenuPrincipal();
} else if (r != null) {
    JOptionPane.showMessageDialog(this, r.mensaje());
}
```
Guarde la URL y la API key en un `config.properties` en lugar de escribirlas en el código.

---

## 4. App móvil (Android)

Archivos listos en `clients/android/` (Java, funcionan también en proyectos Kotlin). Instrucciones en `clients/android/INSTALAR.md`:
- `FacialApiClient.java`: cliente con OkHttp.
- `CapturaFacialActivity.java` + `activity_captura_facial.xml`: pantalla con la cámara frontal (CameraX) que guía, toma las fotos y llama a la API.
- `EjemploLoginFacialActivity.java`: cómo abrirla después del login, para registrar o para marcar ingreso.
- `facial_api.xml`: ponga ahí la URL y la API key.

En la primera prueba en su teléfono, si la verificación dice *"Se detectó el giro hacia el lado contrario"*, cambie `FOTOS_EN_ESPEJO` a `true` en `CapturaFacialActivity.java`.

**Recomendación de seguridad:** la API key dentro del APK se puede extraer. Para el proyecto académico es aceptable; lo más seguro es que el celular envíe las fotos a **su API actual** (que ya autentica al usuario) y que esa API llame a la Facial API con la key guardada en el servidor. Puede usar una key distinta para cada cliente: `API_KEYS=clave-escritorio,clave-movil`.

---

## 5. Calibración

| Variable | Defecto | Para qué |
|---|---|---|
| `FACE_MATCH_THRESHOLD` | 0.55 | Mismo valor que el servicio local. Súbalo si acepta a otra persona; bájelo si rechaza al usuario correcto |
| `FRONT_MAX_YAW` / `TURN_MIN_YAW` | 10° / 15° | Qué cuenta como "de frente" y como "girado" |
| `MAX_YAW` / `MAX_PITCH` | 40° / 25° | Giro máximo aceptado / cabeza muy arriba o abajo |
| `FACE_MIN_SIZE` | 90 px | Rostro demasiado lejos |
| `BLUR_THRESHOLD` | 60 | Foto borrosa |
| `REQUIRE_LIVENESS` | true | Exigir desafío en verify/identify |
| `DUPLICATE_CHECK` | true | Impedir el mismo rostro en dos usuarios |

Se cambian sin reconstruir la imagen:
`az containerapp update -n sigelab-facial-api -g rg-sigelab-facial --set-env-vars FACE_MATCH_THRESHOLD=0.6`

La prueba de vida es una mitigación académica (una foto impresa no puede girar la cabeza cuando se le pide), no un anti-spoofing comercial.

---

## 6. Desarrollo local (opcional)

```powershell
py -3.12 -m venv .venv
.venv\Scripts\pip install -r requirements-dev.txt
copy .env.example .env      # complete DB_*, API_KEYS, FACIAL_ENCRYPTION_KEY (python -m scripts.generate_key)
.venv\Scripts\python -m pytest
.venv\Scripts\uvicorn app.main:app --port 8000
```
Para conectarse a Azure SQL desde su PC necesita el **ODBC Driver 18** y que su IP esté permitida en el firewall de `sigelabumg`.

## Problemas comunes
- **401 NO_AUTORIZADO**: revise el header `X-API-Key`.
- **503 BD_NO_DISPONIBLE**: la BD serverless se estaba reanudando (reintente) o usuario/contraseña/firewall incorrectos. `GET /api/health/db` lo confirma.
- **La primera petición tarda o da timeout**: arranque en frío con escala a cero. Use timeouts de 90 s o `-MinReplicas 1`.
- **`TasksOperationsNotAllowed` al construir**: la suscripción no permite `az acr build`; abra Docker Desktop y use `-BuildMode local`.
- **AUTO_MIGRATE falló** (en los logs): la tabla de usuarios o su llave primaria no coinciden; use los parámetros `-UsersTable` / `-UserIdColumn` o ejecute `sql/001_sigelab_facial_tables.sql` en el editor de consultas del portal.
- **La prueba de vida falla por el lado contrario**: cambie `mirrored`.
