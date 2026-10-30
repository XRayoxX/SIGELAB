# SIGELAB — Sistema de Gestión y Control de Laboratorios Universitarios

Proyecto Final del curso **017 - Programación II**, Universidad Mariano Gálvez de Guatemala
(Ing. Alejandro Díaz, segundo semestre 2026).

Aplicación de escritorio en **Java Swing** que controla el ciclo de vida de los equipos de los
laboratorios: registro, préstamo, devolución (parcial o completa), daño, mantenimiento y baja.
Incluye **inicio de sesión con reconocimiento facial** (segundo factor), **códigos QR** para
identificar equipos, **auditoría**, **dashboard**, **reportes** y **notificaciones por correo**.

---

## Integrantes

| Nombre completo | Carné | Usuario de GitHub |
|---|---|---|
| _(completar)_ | _(completar)_ | XRayoxX / Rayo |
| _(completar)_ | _(completar)_ | drabanalesc2-collab |
| _(completar)_ | _(completar)_ | Hernan |
| _(completar)_ | _(completar)_ | ljimenezg15-lang |

---

## Requisitos

| Componente | Versión |
|---|---|
| Java (JDK) | **21** |
| Maven | 3.9 o superior (el que trae IntelliJ sirve) |
| IDE | IntelliJ IDEA (los formularios `.form` usan IntelliJ Swing UI Designer) |
| Base de datos | **Microsoft SQL Server 2017 o superior**, o **Azure SQL Database** |
| SSMS | SQL Server Management Studio, para ejecutar los scripts |
| Cámara web | Para el reconocimiento facial y la lectura de QR por cámara |
| Componente facial | Python 3.12 + OpenCV/InsightFace (carpeta `sigelab-facial-api`) |

---

## Arquitectura

```
View (Swing .form + .java) → Controller → Service → Repository → JDBC → SQL Server
```

| Paquete | Responsabilidad |
|---|---|
| `SIGELAB.view` | Formularios: el diseño está en los `.form`, los eventos en los `.java` |
| `SIGELAB.controller` | Coordina cada formulario con sus servicios |
| `SIGELAB.service` | **Reglas de negocio** (RN-01 a RN-20), transacciones, permisos por rol |
| `SIGELAB.repository` | Todo el SQL (JDBC con `PreparedStatement`) |
| `SIGELAB.model` / `SIGELAB.enums` | Entidades y estados (`EstadoEquipo`, `EstadoPrestamo`, …) |
| `SIGELAB.util` | Conexión, transacciones, excepciones, tema visual, QR, fechas |
| `SIGELAB.facial` | Cliente Java de la API de reconocimiento facial |
| `SIGELAB.qr` | Lectura de QR con la cámara |

Los errores técnicos no se muestran al usuario: se usan `BusinessException` (validaciones)
y `DataAccessException` (base de datos) con mensajes comprensibles.

---

## 1. Crear la base de datos

Los scripts están en la carpeta `database/` y se ejecutan **en orden** desde SSMS.

| Script | Qué hace | ¿Cuándo? |
|---|---|---|
| `01_sigelab_esquema.sql` | Crea la base `Sigelab`, tablas, llaves, `CHECK`, índices y vistas | Base nueva. **Borra la base `Sigelab` si ya existe** |
| `02_sigelab_datos_iniciales.sql` | Roles, usuarios, laboratorios, tipos, equipos, solicitantes y préstamos de prueba | Base nueva |
| `03_sigelab_usuario_aplicacion.sql` | Crea el login `sigelab_app` (solo lectura/escritura de datos) | Base local |
| `04_sigelab_generar_usuario.sql` | Genera automáticamente el nombre de usuario (`jperez`, `jcperez`, …) | Siempre |
| `05_sigelab_hora_guatemala.sql` | Función `dbo.fn_AhoraGuatemala()`: fechas en hora de Guatemala | Siempre (obligatorio en Azure, que trabaja en UTC) |
| `06_sigelab_convertir_fechas_existentes.sql` | Corrige registros antiguos guardados en UTC (una sola vez) | Solo en una base de Azure con datos anteriores al script 05 |
| `07_sigelab_recordatorios.sql` | Tabla `RecordatorioPrestamo` para los correos de vencimiento | Siempre, después del 05 |

**Base local (nueva):** `01 → 02 → 03 → 04 → 05 → 07`.
**Base en Azure:** conéctese directamente a la base (por ejemplo `sigelabadministrador`), omita
las líneas `USE` y ejecute los scripts que falten en el mismo orden.

> Para la API facial, ejecute además `sigelab-facial-api/sql/001_sigelab_facial_tables.sql`
> (crea `PerfilFacial` y `AuditoriaFacial`).

---

## 2. Configurar la conexión

1. Copie `src/main/resources/config.properties.example` como
   `src/main/resources/config.properties`.
2. Complete sus datos. Este archivo está en `.gitignore`: **nunca** se sube al repositorio.

```properties
# --- SQL Server local ---
db.url=jdbc:sqlserver://localhost:1433;databaseName=Sigelab;encrypt=true;trustServerCertificate=true
db.usuario=sigelab_app
db.contrasena=Sigelab2026

# --- Azure SQL (alternativa) ---
# db.url=jdbc:sqlserver://SU_SERVIDOR.database.windows.net:1433;databaseName=SU_BASE;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=60

# --- Reconocimiento facial ---
facial.habilitado=true
facial.api.url=http://localhost:8000
facial.api.key=SU_API_KEY

# --- Correo (Gmail con "contraseña de aplicación") ---
correo.habilitado=true
correo.remitente=SU_CORREO@gmail.com
correo.contrasena=SU_CONTRASENA_DE_APLICACION
```

- La contraseña de `sigelab_app` es la del script 03; si la cambia allí, cámbiela aquí también.
- `correo.contrasena` es una **contraseña de aplicación** de Google (Cuenta de Google → Seguridad →
  Verificación en dos pasos → Contraseñas de aplicaciones), no la contraseña normal.
- Con `correo.habilitado=false` el sistema funciona igual, pero no envía correos.

---

## 3. Ejecutar la aplicación

1. Abra la carpeta del proyecto en **IntelliJ IDEA** y deje que Maven descargue las dependencias.
2. **Build → Rebuild Project** (necesario: compila los formularios `.form`).
3. Ejecute la clase `Main` (`src/main/java/Main.java`).

**Ejecutable para Windows (opcional):** después del *Rebuild*, ejecute

```powershell
powershell -ExecutionPolicy Bypass -File empaquetar.ps1
```

Genera `dist\SIGELAB\SIGELAB.exe` (incluye su propio Java) y `dist\SIGELAB-portable.zip`.
Copie `config.properties` junto al ejecutable en `dist\SIGELAB\app\`.

---

## 4. Ejecutar el componente facial

El reconocimiento facial es una API en **Python** (FastAPI + OpenCV/InsightFace) que Java consume
por HTTP. Solo valida el rostro del usuario como **segundo factor** del inicio de sesión.

**Local:**

```powershell
cd sigelab-facial-api
py -3.12 -m venv .venv
.venv\Scripts\pip install -r requirements.txt
.venv\Scripts\python -m scripts.generate_key
```

El último comando imprime una `FACIAL_ENCRYPTION_KEY` y una `API_KEYS`. Cree el archivo
`sigelab-facial-api\.env` con esos dos valores y los datos de su base:

```properties
DB_HOST=localhost
DB_NAME=Sigelab
DB_USER=sigelab_app
DB_PASSWORD=Sigelab2026
DB_TRUST_SERVER_CERTIFICATE=true
FACIAL_ENCRYPTION_KEY=(la que imprimió generate_key)
API_KEYS=(la que imprimió generate_key)
```

Luego inicie la API:

```powershell
.venv\Scripts\uvicorn app.main:app --port 8000
```

Use la misma API key en `facial.api.key` de `config.properties`. Necesita el **ODBC Driver 18
for SQL Server**. **No cambie** la `FACIAL_ENCRYPTION_KEY` después de registrar rostros: los
rostros guardados ya no podrían verificarse.
La guía completa (endpoints, despliegue en Azure, calibración) está en
[`sigelab-facial-api/README.md`](sigelab-facial-api/README.md).

**Registro del rostro:** el ADMIN lo hace desde **Usuarios → Registrar rostro…** con la persona
frente a la cámara.

> `facial.habilitado=false` desactiva la verificación facial (solo para pruebas sin cámara).

---

## 5. Usuarios de prueba (script 02)

| Usuario | Contraseña | Rol | Para qué sirve |
|---|---|---|---|
| `admin` | `Admin123` | ADMIN | Acceso completo |
| `encargado` | `Encargado123` | ENCARGADO | Operación diaria |
| `inactivo` | `Encargado123` | ENCARGADO (INACTIVO) | Probar que un usuario inactivo no entra (RN-01) |

Las contraseñas se guardan como hash **SHA-256**. El script 02 también crea equipos en **cada
uno de los cinco estados** y préstamos de ejemplo: uno **vencido**, uno con **devolución parcial**
y uno **finalizado**, para probar las reglas de negocio de inmediato.

---

## 6. Funciones principales

| Módulo | Qué hace |
|---|---|
| **Inicio de sesión** | Usuario + contraseña + estado ACTIVO + **validación facial** (RN-01, RN-02). Modo claro / oscuro |
| **Centro de control** | Dashboard con totales por estado, préstamos activos y vencidos. El menú se adapta al rol |
| **Usuarios** (ADMIN) | Alta, edición y desactivación lógica; nombre de usuario automático; registro del rostro |
| **Laboratorios / Tipos de equipo** (ADMIN) | Catálogos con código único y desactivación lógica |
| **Equipos** | Registro con código de inventario único, **generación del QR y guardado como imagen PNG**, historial del equipo |
| **Solicitantes** | Estudiantes, docentes y administrativos que reciben préstamos |
| **Préstamos** | Encabezado + detalle con **varios equipos**; agregar por código o **leyendo el QR**; solo equipos DISPONIBLES; todo en **una transacción** |
| **Devoluciones** | Devolución **parcial o completa**, equipo por equipo: BUENO → DISPONIBLE, DANADO → DANADO |
| **Mantenimiento** | PREVENTIVO / CORRECTIVO; al cerrar: REPARADO → DISPONIBLE, NO_REPARABLE → BAJA |
| **Reportes** | Equipos por estado, préstamos activos / finalizados / vencidos / entre fechas, historial de equipo, historial de mantenimiento, devoluciones, equipos más prestados. **Doble clic en un préstamo** muestra los equipos que se llevó. Exporta a PDF y Excel (CSV) e imprime |
| **Auditoría** (ADMIN) | Consulta con filtros de quién hizo qué y cuándo; exporta a CSV |
| **Correos** | Aviso de préstamo registrado, de equipo devuelto, de intento de ingreso facial fallido y **recordatorios automáticos**: *"hoy vence su préstamo"* y *"su préstamo venció, devuelva el equipo"* (máximo una vez al día) |

**Estados del equipo:** `DISPONIBLE`, `PRESTADO`, `DANADO`, `MANTENIMIENTO`, `BAJA`.
Solo los cambian los procesos (préstamo, devolución, mantenimiento); nunca se editan a mano.
Un préstamo **vencido** no se guarda: se calcula (fecha esperada pasada + equipos pendientes).

---

## 7. Dependencias externas (Maven)

| Librería | Uso |
|---|---|
| `com.microsoft.sqlserver:mssql-jdbc:13.4.0.jre11` | Conexión JDBC a SQL Server |
| `com.intellij:forms_rt:7.0.3` | Soporte de los formularios `.form` de IntelliJ |
| `com.google.zxing:core` / `javase:3.5.3` | Generar y leer códigos QR |
| `com.github.sarxos:webcam-capture:0.3.12` | Cámara web (QR y reconocimiento facial) |
| `org.apache.pdfbox:pdfbox:3.0.6` | Exportar reportes a PDF |
| `org.eclipse.angus:angus-mail:2.0.3` | Envío de correos (Gmail SMTP) |

Componente facial (Python): `fastapi`, `uvicorn`, `opencv-python-headless`, `insightface`,
`onnxruntime`, `pyodbc`, `cryptography` (ver `sigelab-facial-api/requirements.txt`).

---

## 8. Estructura del repositorio

```
SIGELAB/
├── database/              Scripts SQL 01 a 07
├── src/main/java/         Código Java (Main + paquete SIGELAB)
├── src/main/resources/    config.properties.example
├── sigelab-facial-api/    API de reconocimiento facial (Python)
├── empaquetar.ps1         Genera el ejecutable para Windows
└── pom.xml                Dependencias Maven
```
