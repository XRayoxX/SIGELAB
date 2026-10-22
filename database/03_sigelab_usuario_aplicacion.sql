/* ==========================================================================
   SIGELAB - Script 03: Usuario con el que la aplicación Java se conecta
   Ejecutar DESPUÉS de 01 y 02, conectado como administrador (Windows).
   IMPORTANTE: cambie la contraseña antes de ejecutar y use la misma en
   src/main/resources/db.properties (db.contrasena).
   ========================================================================== */

USE master;
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'sigelab_app')
    CREATE LOGIN sigelab_app
        WITH PASSWORD = 'Sigelab2026',
             DEFAULT_DATABASE = Sigelab,
             CHECK_POLICY = OFF;
GO

USE Sigelab;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'sigelab_app')
    CREATE USER sigelab_app FOR LOGIN sigelab_app;
GO

-- Solo puede leer y escribir datos; no puede borrar tablas ni cambiar la estructura
ALTER ROLE db_datareader ADD MEMBER sigelab_app;
ALTER ROLE db_datawriter ADD MEMBER sigelab_app;
GO

PRINT 'Usuario sigelab_app listo.';
GO
