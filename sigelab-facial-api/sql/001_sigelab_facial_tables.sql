/* =====================================================================
   SIGELAB - Tablas de reconocimiento facial (Azure SQL)
   Ejecútelo una vez en la base sigelabadministrador:
     - Portal de Azure > base de datos > Editor de consultas, o
     - SSMS / Azure Data Studio, o
     - python -m scripts.migrate
   Es idempotente: no borra nada si las tablas ya existen.

   Hecho para la tabla dbo.Usuario de SIGELAB (PK IdUsuario INT). Si cambia,
   cambie las dos líneas "REFERENCES dbo.Usuario(IdUsuario)".
   ===================================================================== */

IF OBJECT_ID(N'dbo.PerfilFacial', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PerfilFacial
    (
        id_perfil_facial    INT IDENTITY(1,1) NOT NULL,
        id_usuario          INT            NOT NULL,
        embedding           VARBINARY(MAX) NOT NULL,   -- vector facial cifrado con AES-GCM (no es una foto)
        modelo              NVARCHAR(100)  NOT NULL,
        dimension           INT            NOT NULL,
        fecha_registro      DATETIME2(0)   NOT NULL CONSTRAINT DF_PerfilFacial_FechaRegistro DEFAULT SYSDATETIME(),
        fecha_actualizacion DATETIME2(0)   NOT NULL CONSTRAINT DF_PerfilFacial_FechaActualizacion DEFAULT SYSDATETIME(),
        activo              BIT            NOT NULL CONSTRAINT DF_PerfilFacial_Activo DEFAULT 1,
        cantidad_muestras   INT            NOT NULL,
        quality_score       FLOAT          NULL,
        version_algoritmo   NVARCHAR(50)   NOT NULL,

        CONSTRAINT PK_PerfilFacial PRIMARY KEY (id_perfil_facial),
        CONSTRAINT UQ_PerfilFacial_Usuario UNIQUE (id_usuario),
        CONSTRAINT FK_PerfilFacial_Usuario FOREIGN KEY (id_usuario)
            REFERENCES dbo.Usuario(IdUsuario),
        CONSTRAINT CK_PerfilFacial_Dimension CHECK (dimension > 0),
        CONSTRAINT CK_PerfilFacial_Muestras CHECK (cantidad_muestras > 0)
    );
END;
GO

IF OBJECT_ID(N'dbo.AuditoriaFacial', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AuditoriaFacial
    (
        id_auditoria BIGINT IDENTITY(1,1) NOT NULL,
        id_usuario   INT            NULL,
        fecha_hora   DATETIME2(0)   NOT NULL CONSTRAINT DF_AuditoriaFacial_Fecha DEFAULT SYSDATETIME(),
        accion       VARCHAR(30)    NOT NULL,   -- ENROLL_EXITOSO, VERIFY_FALLIDO, IDENTIFY_EXITOSO, LIVENESS_FALLIDO, ...
        resultado    VARCHAR(20)    NOT NULL,   -- MATCH, NO_MATCH, DENIED, OK
        score        FLOAT          NULL,
        threshold    FLOAT          NULL,
        detalle      NVARCHAR(1000) NULL,
        dispositivo  NVARCHAR(200)  NULL,       -- valor del header X-Client (ej. "sigelab-desktop", "sigelab-android")

        CONSTRAINT PK_AuditoriaFacial PRIMARY KEY (id_auditoria),
        CONSTRAINT FK_AuditoriaFacial_Usuario FOREIGN KEY (id_usuario)
            REFERENCES dbo.Usuario(IdUsuario)
            ON DELETE SET NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_AuditoriaFacial_UsuarioFecha'
               AND object_id = OBJECT_ID(N'dbo.AuditoriaFacial'))
BEGIN
    CREATE INDEX IX_AuditoriaFacial_UsuarioFecha
        ON dbo.AuditoriaFacial(id_usuario, fecha_hora DESC);
END;
GO
