/* ==========================================================================
   SIGELAB - Sistema de Gestión y Control de Laboratorios Universitarios
   Script 01: Creación de la base de datos, tablas, índices y vistas
   Motor: Microsoft SQL Server 2017 o superior
   ==========================================================================
   NOTA DE DISEÑO
   - Las reglas de negocio (cambiar estados, validar disponibilidad, etc.)
     se ejecutan en la capa Service de Java (RN-20). Por eso este script NO
     usa triggers ni procedimientos que cambien estados automáticamente.
   - Las restricciones (PK, FK, UNIQUE, CHECK, índices filtrados) funcionan
     como "red de seguridad": si el Service tuviera un error, la base de
     datos igual impide dejar información inconsistente.
   - Los valores de estado van SIN tildes ni Ñ (DANADO) para coincidir
     exactamente con los enum de Java.
   ========================================================================== */

USE master;
GO

IF DB_ID('Sigelab') IS NOT NULL
BEGIN
    ALTER DATABASE Sigelab SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE Sigelab;
END
GO

-- CI = no distingue mayúsculas: 'cam-001' y 'CAM-001' se consideran el mismo código
CREATE DATABASE Sigelab COLLATE Modern_Spanish_CI_AS;
GO

USE Sigelab;
GO

/* ==========================================================================
   1. SEGURIDAD: ROL Y USUARIO
   ========================================================================== */

CREATE TABLE dbo.Rol (
    IdRol        INT IDENTITY(1,1) NOT NULL,
    Nombre       VARCHAR(20)       NOT NULL,
    Descripcion  VARCHAR(150)      NULL,
    CONSTRAINT PK_Rol         PRIMARY KEY (IdRol),
    CONSTRAINT UQ_Rol_Nombre  UNIQUE (Nombre),
    CONSTRAINT CK_Rol_Nombre  CHECK (Nombre IN ('ADMIN', 'ENCARGADO', 'DOCENTE'))
);
GO

CREATE TABLE dbo.Usuario (
    IdUsuario       INT IDENTITY(1,1) NOT NULL,
    Nombre          VARCHAR(60)       NOT NULL,
    Apellido        VARCHAR(60)       NOT NULL,
    SegundoNombre   NVARCHAR(100)     NULL,       -- opcional
    SegundoApellido NVARCHAR(100)     NULL,       -- opcional
    NombreUsuario   VARCHAR(30)       NOT NULL,
    ContrasenaHash  VARCHAR(64)       NOT NULL,   -- SHA-256 en hexadecimal (64 caracteres)
    Correo          VARCHAR(100)      NOT NULL,
    IdRol           INT               NOT NULL,
    Estado          VARCHAR(10)       NOT NULL CONSTRAINT DF_Usuario_Estado DEFAULT 'ACTIVO',
    RutaFotoRostro  VARCHAR(260)      NULL,       -- imagen de referencia para el servicio facial
    FechaCreacion   DATETIME2(0)      NOT NULL CONSTRAINT DF_Usuario_FechaCreacion DEFAULT SYSDATETIME(),
    CONSTRAINT PK_Usuario                PRIMARY KEY (IdUsuario),
    CONSTRAINT UQ_Usuario_NombreUsuario  UNIQUE (NombreUsuario),
    CONSTRAINT UQ_Usuario_Correo         UNIQUE (Correo),
    CONSTRAINT FK_Usuario_Rol            FOREIGN KEY (IdRol) REFERENCES dbo.Rol (IdRol),
    CONSTRAINT CK_Usuario_Estado         CHECK (Estado IN ('ACTIVO', 'INACTIVO')),          -- RN-01
    CONSTRAINT CK_Usuario_Correo         CHECK (Correo LIKE '%_@_%._%'),
    CONSTRAINT CK_Usuario_Hash           CHECK (LEN(ContrasenaHash) = 64)
);
GO

/* ==========================================================================
   2. CATÁLOGOS: LABORATORIO Y TIPO DE EQUIPO
   ========================================================================== */

CREATE TABLE dbo.Laboratorio (
    IdLaboratorio  INT IDENTITY(1,1) NOT NULL,
    Codigo         VARCHAR(15)       NOT NULL,
    Nombre         VARCHAR(80)       NOT NULL,
    Ubicacion      VARCHAR(120)      NOT NULL,
    Capacidad      INT               NOT NULL,
    Estado         VARCHAR(10)       NOT NULL CONSTRAINT DF_Laboratorio_Estado DEFAULT 'ACTIVO',
    CONSTRAINT PK_Laboratorio            PRIMARY KEY (IdLaboratorio),
    CONSTRAINT UQ_Laboratorio_Codigo     UNIQUE (Codigo),                                  -- RN-03
    CONSTRAINT CK_Laboratorio_Capacidad  CHECK (Capacidad > 0),
    CONSTRAINT CK_Laboratorio_Estado     CHECK (Estado IN ('ACTIVO', 'INACTIVO'))
);
GO

CREATE TABLE dbo.TipoEquipo (
    IdTipoEquipo  INT IDENTITY(1,1) NOT NULL,
    Nombre        VARCHAR(50)       NOT NULL,
    Descripcion   VARCHAR(150)      NULL,
    Estado        VARCHAR(10)       NOT NULL CONSTRAINT DF_TipoEquipo_Estado DEFAULT 'ACTIVO',
    CONSTRAINT PK_TipoEquipo         PRIMARY KEY (IdTipoEquipo),
    CONSTRAINT UQ_TipoEquipo_Nombre  UNIQUE (Nombre),
    CONSTRAINT CK_TipoEquipo_Estado  CHECK (Estado IN ('ACTIVO', 'INACTIVO'))
);
GO

/* ==========================================================================
   3. EQUIPO (activo principal)
   ========================================================================== */

CREATE TABLE dbo.Equipo (
    IdEquipo          INT IDENTITY(1,1) NOT NULL,
    CodigoInventario  VARCHAR(20)       NOT NULL,   -- valor que se guarda en el QR
    Nombre            VARCHAR(100)      NOT NULL,
    IdTipoEquipo      INT               NOT NULL,
    Marca             VARCHAR(50)       NOT NULL,
    Modelo            VARCHAR(50)       NOT NULL,
    NumeroSerie       VARCHAR(60)       NULL,
    IdLaboratorio     INT               NOT NULL,
    Estado            VARCHAR(15)       NOT NULL CONSTRAINT DF_Equipo_Estado DEFAULT 'DISPONIBLE',
    Observaciones     VARCHAR(500)      NULL,
    FechaRegistro     DATETIME2(0)      NOT NULL CONSTRAINT DF_Equipo_FechaRegistro DEFAULT SYSDATETIME(),
    CONSTRAINT PK_Equipo                   PRIMARY KEY (IdEquipo),
    CONSTRAINT UQ_Equipo_CodigoInventario  UNIQUE (CodigoInventario),                      -- RN-04
    CONSTRAINT FK_Equipo_TipoEquipo        FOREIGN KEY (IdTipoEquipo)  REFERENCES dbo.TipoEquipo (IdTipoEquipo),
    CONSTRAINT FK_Equipo_Laboratorio       FOREIGN KEY (IdLaboratorio) REFERENCES dbo.Laboratorio (IdLaboratorio),
    CONSTRAINT CK_Equipo_Estado            CHECK (Estado IN ('DISPONIBLE', 'PRESTADO', 'DANADO', 'MANTENIMIENTO', 'BAJA'))
);
GO

-- El número de serie es opcional, pero si se ingresa no puede repetirse
CREATE UNIQUE INDEX UX_Equipo_NumeroSerie ON dbo.Equipo (NumeroSerie) WHERE NumeroSerie IS NOT NULL;
CREATE INDEX IX_Equipo_Estado       ON dbo.Equipo (Estado);
CREATE INDEX IX_Equipo_Laboratorio  ON dbo.Equipo (IdLaboratorio);
CREATE INDEX IX_Equipo_TipoEquipo   ON dbo.Equipo (IdTipoEquipo);
GO

/* ==========================================================================
   4. SOLICITANTE (persona que recibe el préstamo)
   Se separa de Usuario porque quien solicita (estudiante/docente) no
   necesariamente inicia sesión en el sistema.
   ========================================================================== */

CREATE TABLE dbo.Solicitante (
    IdSolicitante  INT IDENTITY(1,1) NOT NULL,
    Carne          VARCHAR(20)       NOT NULL,   -- carné universitario o código de empleado
    Nombre         VARCHAR(60)       NOT NULL,
    Apellido       VARCHAR(60)       NOT NULL,
    Tipo           VARCHAR(15)       NOT NULL,
    Correo         VARCHAR(100)      NULL,
    Telefono       VARCHAR(15)       NULL,
    Estado         VARCHAR(10)       NOT NULL CONSTRAINT DF_Solicitante_Estado DEFAULT 'ACTIVO',
    CONSTRAINT PK_Solicitante         PRIMARY KEY (IdSolicitante),
    CONSTRAINT UQ_Solicitante_Carne   UNIQUE (Carne),
    CONSTRAINT CK_Solicitante_Tipo    CHECK (Tipo IN ('ESTUDIANTE', 'DOCENTE', 'ADMINISTRATIVO')),
    CONSTRAINT CK_Solicitante_Estado  CHECK (Estado IN ('ACTIVO', 'INACTIVO'))
);
GO

/* ==========================================================================
   5. PRÉSTAMO (encabezado) Y DETALLE_PRESTAMO
   ========================================================================== */

CREATE TABLE dbo.Prestamo (
    IdPrestamo               INT IDENTITY(1,1) NOT NULL,
    IdSolicitante            INT               NOT NULL,
    FechaPrestamo            DATETIME2(0)      NOT NULL CONSTRAINT DF_Prestamo_Fecha DEFAULT SYSDATETIME(),
    FechaEsperadaDevolucion  DATETIME2(0)      NOT NULL,
    IdUsuarioRegistra        INT               NOT NULL,
    Estado                   VARCHAR(12)       NOT NULL CONSTRAINT DF_Prestamo_Estado DEFAULT 'ACTIVO',
    FechaFinalizacion        DATETIME2(0)      NULL,      -- se llena al devolver el último equipo
    Observaciones            VARCHAR(500)      NULL,
    CONSTRAINT PK_Prestamo                PRIMARY KEY (IdPrestamo),
    CONSTRAINT FK_Prestamo_Solicitante    FOREIGN KEY (IdSolicitante)     REFERENCES dbo.Solicitante (IdSolicitante),
    CONSTRAINT FK_Prestamo_Usuario        FOREIGN KEY (IdUsuarioRegistra) REFERENCES dbo.Usuario (IdUsuario),
    CONSTRAINT CK_Prestamo_Fechas         CHECK (FechaEsperadaDevolucion > FechaPrestamo),
    -- VENCIDO no se guarda: se calcula (RN-17). Solo existen ACTIVO y FINALIZADO.
    CONSTRAINT CK_Prestamo_Estado         CHECK (Estado IN ('ACTIVO', 'FINALIZADO')),
    CONSTRAINT CK_Prestamo_Finalizacion   CHECK (
        (Estado = 'ACTIVO'     AND FechaFinalizacion IS NULL) OR
        (Estado = 'FINALIZADO' AND FechaFinalizacion IS NOT NULL AND FechaFinalizacion >= FechaPrestamo)
    )
);
GO

CREATE INDEX IX_Prestamo_Estado       ON dbo.Prestamo (Estado, FechaEsperadaDevolucion);
CREATE INDEX IX_Prestamo_Solicitante  ON dbo.Prestamo (IdSolicitante);
CREATE INDEX IX_Prestamo_Fecha        ON dbo.Prestamo (FechaPrestamo);
GO

CREATE TABLE dbo.DetallePrestamo (
    IdDetallePrestamo  INT IDENTITY(1,1) NOT NULL,
    IdPrestamo         INT               NOT NULL,
    IdEquipo           INT               NOT NULL,
    Estado             VARCHAR(10)       NOT NULL CONSTRAINT DF_DetallePrestamo_Estado DEFAULT 'PENDIENTE',
    CONSTRAINT PK_DetallePrestamo           PRIMARY KEY (IdDetallePrestamo),
    CONSTRAINT FK_DetallePrestamo_Prestamo  FOREIGN KEY (IdPrestamo) REFERENCES dbo.Prestamo (IdPrestamo),
    CONSTRAINT FK_DetallePrestamo_Equipo    FOREIGN KEY (IdEquipo)   REFERENCES dbo.Equipo (IdEquipo),
    CONSTRAINT UQ_DetallePrestamo_Equipo    UNIQUE (IdPrestamo, IdEquipo),                 -- RN-06
    CONSTRAINT CK_DetallePrestamo_Estado    CHECK (Estado IN ('PENDIENTE', 'DEVUELTO'))
);
GO

-- Red de seguridad para RN-08: un equipo solo puede estar PENDIENTE en un préstamo a la vez
CREATE UNIQUE INDEX UX_DetallePrestamo_EquipoPendiente
    ON dbo.DetallePrestamo (IdEquipo) WHERE Estado = 'PENDIENTE';
GO

/* ==========================================================================
   6. DEVOLUCIÓN (una por cada línea de detalle)
   ========================================================================== */

CREATE TABLE dbo.Devolucion (
    IdDevolucion       INT IDENTITY(1,1) NOT NULL,
    IdDetallePrestamo  INT               NOT NULL,
    FechaDevolucion    DATETIME2(0)      NOT NULL CONSTRAINT DF_Devolucion_Fecha DEFAULT SYSDATETIME(),
    Condicion          VARCHAR(10)       NOT NULL,
    Observaciones      VARCHAR(500)      NULL,
    IdUsuarioRecibe    INT               NOT NULL,
    CONSTRAINT PK_Devolucion           PRIMARY KEY (IdDevolucion),
    CONSTRAINT FK_Devolucion_Detalle   FOREIGN KEY (IdDetallePrestamo) REFERENCES dbo.DetallePrestamo (IdDetallePrestamo),
    CONSTRAINT FK_Devolucion_Usuario   FOREIGN KEY (IdUsuarioRecibe)   REFERENCES dbo.Usuario (IdUsuario),
    CONSTRAINT UQ_Devolucion_Detalle   UNIQUE (IdDetallePrestamo),                         -- RN-11
    CONSTRAINT CK_Devolucion_Condicion CHECK (Condicion IN ('BUENO', 'DANADO'))            -- RN-12 / RN-13
);
GO

/* ==========================================================================
   7. MANTENIMIENTO
   ========================================================================== */

CREATE TABLE dbo.Mantenimiento (
    IdMantenimiento    INT IDENTITY(1,1) NOT NULL,
    IdEquipo           INT               NOT NULL,
    FechaIngreso       DATETIME2(0)      NOT NULL CONSTRAINT DF_Mantenimiento_Ingreso DEFAULT SYSDATETIME(),
    TipoMantenimiento  VARCHAR(12)       NOT NULL,
    Descripcion        VARCHAR(500)      NOT NULL,
    Responsable        VARCHAR(100)      NOT NULL,   -- técnico interno o externo
    FechaSalida        DATETIME2(0)      NULL,
    Resultado          VARCHAR(15)       NULL,
    Estado             VARCHAR(12)       NOT NULL CONSTRAINT DF_Mantenimiento_Estado DEFAULT 'EN_PROCESO',
    Observaciones      VARCHAR(500)      NULL,
    IdUsuarioRegistra  INT               NOT NULL,
    CONSTRAINT PK_Mantenimiento           PRIMARY KEY (IdMantenimiento),
    CONSTRAINT FK_Mantenimiento_Equipo    FOREIGN KEY (IdEquipo)          REFERENCES dbo.Equipo (IdEquipo),
    CONSTRAINT FK_Mantenimiento_Usuario   FOREIGN KEY (IdUsuarioRegistra) REFERENCES dbo.Usuario (IdUsuario),
    CONSTRAINT CK_Mantenimiento_Tipo      CHECK (TipoMantenimiento IN ('PREVENTIVO', 'CORRECTIVO')),
    CONSTRAINT CK_Mantenimiento_Resultado CHECK (Resultado IN ('REPARADO', 'NO_REPARABLE')),       -- RN-15
    CONSTRAINT CK_Mantenimiento_Estado    CHECK (Estado IN ('EN_PROCESO', 'FINALIZADO')),
    -- Abierto: sin salida ni resultado. Cerrado: con ambos.
    CONSTRAINT CK_Mantenimiento_Cierre    CHECK (
        (Estado = 'EN_PROCESO' AND FechaSalida IS NULL     AND Resultado IS NULL) OR
        (Estado = 'FINALIZADO' AND FechaSalida IS NOT NULL AND Resultado IS NOT NULL)
    ),
    CONSTRAINT CK_Mantenimiento_Fechas    CHECK (FechaSalida IS NULL OR FechaSalida >= FechaIngreso)
);
GO

-- Un equipo no puede tener dos mantenimientos abiertos al mismo tiempo
CREATE UNIQUE INDEX UX_Mantenimiento_EquipoAbierto
    ON dbo.Mantenimiento (IdEquipo) WHERE Estado = 'EN_PROCESO';
CREATE INDEX IX_Mantenimiento_Equipo ON dbo.Mantenimiento (IdEquipo, FechaIngreso);
GO

/* ==========================================================================
   8. AUDITORÍA
   ========================================================================== */

CREATE TABLE dbo.Auditoria (
    IdAuditoria    BIGINT IDENTITY(1,1) NOT NULL,
    IdUsuario      INT                  NULL,      -- NULL si el login falló con un usuario inexistente
    NombreUsuario  VARCHAR(30)          NOT NULL,  -- usuario que actuó o que se intentó usar
    FechaHora      DATETIME2(0)         NOT NULL CONSTRAINT DF_Auditoria_FechaHora DEFAULT SYSDATETIME(),
    Accion         VARCHAR(40)          NOT NULL,
    Entidad        VARCHAR(40)          NOT NULL,
    IdRegistro     INT                  NULL,
    Descripcion    VARCHAR(500)         NULL,
    CONSTRAINT PK_Auditoria          PRIMARY KEY (IdAuditoria),
    CONSTRAINT FK_Auditoria_Usuario  FOREIGN KEY (IdUsuario) REFERENCES dbo.Usuario (IdUsuario),
    -- Debe coincidir con el enum AccionAuditoria de Java. Agregar aquí si se crean nuevas acciones.
    CONSTRAINT CK_Auditoria_Accion   CHECK (Accion IN (
        'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'LOGIN_FACIAL_FALLIDO',
        'USUARIO_CREADO', 'USUARIO_MODIFICADO', 'USUARIO_DESACTIVADO',
        'LABORATORIO_CREADO', 'LABORATORIO_MODIFICADO',
        'EQUIPO_CREADO', 'EQUIPO_MODIFICADO',
        'PRESTAMO_CREADO', 'PRESTAMO_FINALIZADO', 'EQUIPO_DEVUELTO',
        'EQUIPO_ENVIADO_MANTENIMIENTO', 'MANTENIMIENTO_FINALIZADO'
    ))
);
GO

CREATE INDEX IX_Auditoria_FechaHora ON dbo.Auditoria (FechaHora DESC);
CREATE INDEX IX_Auditoria_Usuario   ON dbo.Auditoria (IdUsuario);
GO

/* ==========================================================================
   9. VISTAS DE APOYO PARA DASHBOARD Y REPORTES
   ========================================================================== */

-- Equipo con sus datos relacionados (para JTable y para la búsqueda por QR)
CREATE VIEW dbo.vw_EquipoDetalle AS
SELECT  e.IdEquipo,
        e.CodigoInventario,
        e.Nombre,
        t.Nombre        AS TipoEquipo,
        e.Marca,
        e.Modelo,
        e.NumeroSerie,
        l.Codigo        AS CodigoLaboratorio,
        l.Nombre        AS Laboratorio,
        e.Estado,
        e.Observaciones
FROM    dbo.Equipo      e
JOIN    dbo.TipoEquipo  t ON t.IdTipoEquipo  = e.IdTipoEquipo
JOIN    dbo.Laboratorio l ON l.IdLaboratorio = e.IdLaboratorio;
GO

-- Resumen de préstamos con conteo de pendientes y cálculo de vencido (RN-16 / RN-17)
CREATE VIEW dbo.vw_PrestamoResumen AS
SELECT  p.IdPrestamo,
        p.FechaPrestamo,
        p.FechaEsperadaDevolucion,
        p.FechaFinalizacion,
        p.Estado,
        s.Carne,
        s.Nombre + ' ' + s.Apellido                                   AS Solicitante,
        u.NombreUsuario                                               AS RegistradoPor,
        COUNT(d.IdDetallePrestamo)                                    AS TotalEquipos,
        SUM(CASE WHEN d.Estado = 'PENDIENTE' THEN 1 ELSE 0 END)       AS EquiposPendientes,
        CAST(CASE WHEN p.Estado = 'ACTIVO'
                   AND p.FechaEsperadaDevolucion < SYSDATETIME()
                   AND SUM(CASE WHEN d.Estado = 'PENDIENTE' THEN 1 ELSE 0 END) > 0
                  THEN 1 ELSE 0 END AS BIT)                           AS EsVencido
FROM    dbo.Prestamo        p
JOIN    dbo.Solicitante     s ON s.IdSolicitante = p.IdSolicitante
JOIN    dbo.Usuario         u ON u.IdUsuario     = p.IdUsuarioRegistra
JOIN    dbo.DetallePrestamo d ON d.IdPrestamo    = p.IdPrestamo
GROUP BY p.IdPrestamo, p.FechaPrestamo, p.FechaEsperadaDevolucion, p.FechaFinalizacion,
         p.Estado, s.Carne, s.Nombre, s.Apellido, u.NombreUsuario;
GO

-- Contadores del formulario principal (sección 6.11)
CREATE VIEW dbo.vw_Dashboard AS
SELECT
    (SELECT COUNT(*) FROM dbo.Equipo)                                   AS TotalEquipos,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'DISPONIBLE')       AS Disponibles,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'PRESTADO')         AS Prestados,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'DANADO')           AS Danados,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'MANTENIMIENTO')    AS EnMantenimiento,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'BAJA')             AS DeBaja,
    (SELECT COUNT(*) FROM dbo.Prestamo WHERE Estado = 'ACTIVO')         AS PrestamosActivos,
    (SELECT COUNT(*)
       FROM dbo.Prestamo p
      WHERE p.Estado = 'ACTIVO'
        AND p.FechaEsperadaDevolucion < SYSDATETIME()
        AND EXISTS (SELECT 1 FROM dbo.DetallePrestamo d
                     WHERE d.IdPrestamo = p.IdPrestamo AND d.Estado = 'PENDIENTE')) AS PrestamosVencidos;
GO

PRINT 'Base de datos Sigelab creada correctamente.';
GO
