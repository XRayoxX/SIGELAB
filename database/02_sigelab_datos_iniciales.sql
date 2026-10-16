/* ==========================================================================
   SIGELAB - Script 02: Datos iniciales y de prueba
   Ejecutar DESPUÉS de 01_sigelab_esquema.sql
   ==========================================================================
   Usuarios de prueba (contraseña guardada como SHA-256 en hexadecimal):
     admin      / Admin123      -> rol ADMIN
     encargado  / Encargado123  -> rol ENCARGADO
     inactivo   / Encargado123  -> usuario INACTIVO (para probar RN-01)

   En Java el hash se compara así:
     MessageDigest.getInstance("SHA-256") -> bytes -> hexadecimal en minúscula
   ========================================================================== */

USE Sigelab;
GO

SET NOCOUNT ON;
GO

/* ---------- Roles ---------- */
INSERT INTO dbo.Rol (Nombre, Descripcion) VALUES
('ADMIN',     'Acceso completo al sistema'),
('ENCARGADO', 'Operación cotidiana: equipos, préstamos, devoluciones y mantenimiento'),
('DOCENTE',   'Consultas');
GO

/* ---------- Usuarios ---------- */
INSERT INTO dbo.Usuario (Nombre, Apellido, NombreUsuario, ContrasenaHash, Correo, IdRol, Estado) VALUES
('Administrador', 'SIGELAB', 'admin',
 '3b612c75a7b5048a435fb6ec81e52ff92d6d795a8b5a9c17070f6a63c97a53b2',
 'admin@sigelab.edu.gt',
 (SELECT IdRol FROM dbo.Rol WHERE Nombre = 'ADMIN'), 'ACTIVO'),

('María', 'López', 'encargado',
 'd30bbd4ee45b2bc2c6e586d198e2a7fb03a633f9a6e1fc58ee5ff9fd975be6e0',
 'encargado@sigelab.edu.gt',
 (SELECT IdRol FROM dbo.Rol WHERE Nombre = 'ENCARGADO'), 'ACTIVO'),

('Pedro', 'Ramírez', 'inactivo',
 'd30bbd4ee45b2bc2c6e586d198e2a7fb03a633f9a6e1fc58ee5ff9fd975be6e0',
 'inactivo@sigelab.edu.gt',
 (SELECT IdRol FROM dbo.Rol WHERE Nombre = 'ENCARGADO'), 'INACTIVO');
GO

/* ---------- Laboratorios ---------- */
INSERT INTO dbo.Laboratorio (Codigo, Nombre, Ubicacion, Capacidad, Estado) VALUES
('LAB-01', 'Laboratorio de Computación 1', 'Edificio A, nivel 2', 30, 'ACTIVO'),
('LAB-02', 'Laboratorio de Redes',         'Edificio A, nivel 3', 25, 'ACTIVO'),
('LAB-03', 'Laboratorio de Electrónica',   'Edificio B, nivel 1', 20, 'ACTIVO');
GO

/* ---------- Tipos de equipo ---------- */
INSERT INTO dbo.TipoEquipo (Nombre, Descripcion) VALUES
('Laptop',          'Computadora portátil'),
('Cámara',          'Cámara fotográfica o de video'),
('Proyector',       'Proyector multimedia'),
('Router',          'Equipo de red - router'),
('Switch',          'Equipo de red - switch'),
('Tablet',          'Tableta electrónica'),
('Monitor',         'Monitor de computadora'),
('Kit electrónico', 'Kit de componentes electrónicos');
GO

/* ---------- Equipos (uno por cada estado, para probar las reglas) ---------- */
DECLARE @Lab1 INT = (SELECT IdLaboratorio FROM dbo.Laboratorio WHERE Codigo = 'LAB-01');
DECLARE @Lab2 INT = (SELECT IdLaboratorio FROM dbo.Laboratorio WHERE Codigo = 'LAB-02');
DECLARE @Lab3 INT = (SELECT IdLaboratorio FROM dbo.Laboratorio WHERE Codigo = 'LAB-03');

INSERT INTO dbo.Equipo (CodigoInventario, Nombre, IdTipoEquipo, Marca, Modelo, NumeroSerie, IdLaboratorio, Estado, Observaciones) VALUES
('LAP-001', 'Laptop Dell 01',       (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Laptop'),          'Dell',     'Latitude 5420', 'DL5420-0001', @Lab1, 'DISPONIBLE',    NULL),
('LAP-002', 'Laptop Dell 02',       (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Laptop'),          'Dell',     'Latitude 5420', 'DL5420-0002', @Lab1, 'DISPONIBLE',    NULL),
('LAP-003', 'Laptop HP 01',         (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Laptop'),          'HP',       'ProBook 450',   'HP450-0001',  @Lab1, 'PRESTADO',      NULL),
('CAM-001', 'Cámara Canon 01',      (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Cámara'),          'Canon',    'EOS M50',       'CNM50-0001',  @Lab1, 'DISPONIBLE',    NULL),
('PRO-001', 'Proyector Epson 01',   (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Proyector'),       'Epson',    'PowerLite X49', 'EPX49-0001',  @Lab1, 'PRESTADO',      NULL),
('PRO-002', 'Proyector Epson 02',   (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Proyector'),       'Epson',    'PowerLite X49', 'EPX49-0002',  @Lab1, 'DANADO',        'Lámpara no enciende'),
('ROU-001', 'Router Cisco 01',      (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Router'),          'Cisco',    'ISR 1100',      'CS1100-0001', @Lab2, 'DISPONIBLE',    NULL),
('SWI-001', 'Switch Cisco 01',      (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Switch'),          'Cisco',    'Catalyst 2960', 'CS2960-0001', @Lab2, 'MANTENIMIENTO', 'Revisión de puertos'),
('TAB-001', 'Tablet Samsung 01',    (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Tablet'),          'Samsung',  'Galaxy Tab A8', 'SMA8-0001',   @Lab2, 'PRESTADO',      NULL),
('MON-001', 'Monitor LG 01',        (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Monitor'),         'LG',       '24MK430H',      'LG24-0001',   @Lab1, 'BAJA',          'Pantalla quebrada, no reparable'),
('KIT-001', 'Kit Arduino 01',       (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Kit electrónico'), 'Arduino',  'Starter Kit',   NULL,          @Lab3, 'DISPONIBLE',    NULL),
('KIT-002', 'Kit Arduino 02',       (SELECT IdTipoEquipo FROM dbo.TipoEquipo WHERE Nombre = 'Kit electrónico'), 'Arduino',  'Starter Kit',   NULL,          @Lab3, 'DISPONIBLE',    NULL);
GO

/* ---------- Solicitantes ---------- */
INSERT INTO dbo.Solicitante (Carne, Nombre, Apellido, Tipo, Correo, Telefono) VALUES
('0900-24-1001', 'Ana',    'García',    'ESTUDIANTE', 'agarcia@miumg.edu.gt',   '5555-1001'),
('0900-24-1002', 'Luis',   'Hernández', 'ESTUDIANTE', 'lhernandez@miumg.edu.gt','5555-1002'),
('0900-23-2050', 'Sofía',  'Morales',   'ESTUDIANTE', 'smorales@miumg.edu.gt',  '5555-2050'),
('DOC-0015',     'Carlos', 'Méndez',    'DOCENTE',    'cmendez@miumg.edu.gt',   '5555-0015');
GO

/* ==========================================================================
   Préstamos de ejemplo
   - Préstamo 1: ACTIVO y VENCIDO (fecha esperada ya pasó) -> LAP-003 y PRO-001
   - Préstamo 2: ACTIVO en tiempo, devolución parcial       -> TAB-001 pendiente, CAM-001 devuelta
   - Préstamo 3: FINALIZADO, un equipo regresó dañado       -> PRO-002
   ========================================================================== */

DECLARE @Encargado INT = (SELECT IdUsuario FROM dbo.Usuario WHERE NombreUsuario = 'encargado');
DECLARE @P1 INT, @P2 INT, @P3 INT, @Det INT;

-- Préstamo 1: vencido
INSERT INTO dbo.Prestamo (IdSolicitante, FechaPrestamo, FechaEsperadaDevolucion, IdUsuarioRegistra, Estado, Observaciones)
VALUES ((SELECT IdSolicitante FROM dbo.Solicitante WHERE Carne = '0900-24-1001'),
        DATEADD(DAY, -10, SYSDATETIME()), DATEADD(DAY, -3, SYSDATETIME()), @Encargado, 'ACTIVO', 'Proyecto de clase');
SET @P1 = SCOPE_IDENTITY();

INSERT INTO dbo.DetallePrestamo (IdPrestamo, IdEquipo, Estado) VALUES
(@P1, (SELECT IdEquipo FROM dbo.Equipo WHERE CodigoInventario = 'LAP-003'), 'PENDIENTE'),
(@P1, (SELECT IdEquipo FROM dbo.Equipo WHERE CodigoInventario = 'PRO-001'), 'PENDIENTE');

-- Préstamo 2: activo con devolución parcial
INSERT INTO dbo.Prestamo (IdSolicitante, FechaPrestamo, FechaEsperadaDevolucion, IdUsuarioRegistra, Estado)
VALUES ((SELECT IdSolicitante FROM dbo.Solicitante WHERE Carne = 'DOC-0015'),
        DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, 5, SYSDATETIME()), @Encargado, 'ACTIVO');
SET @P2 = SCOPE_IDENTITY();

INSERT INTO dbo.DetallePrestamo (IdPrestamo, IdEquipo, Estado) VALUES
(@P2, (SELECT IdEquipo FROM dbo.Equipo WHERE CodigoInventario = 'TAB-001'), 'PENDIENTE');

INSERT INTO dbo.DetallePrestamo (IdPrestamo, IdEquipo, Estado) VALUES
(@P2, (SELECT IdEquipo FROM dbo.Equipo WHERE CodigoInventario = 'CAM-001'), 'DEVUELTO');
SET @Det = SCOPE_IDENTITY();

INSERT INTO dbo.Devolucion (IdDetallePrestamo, FechaDevolucion, Condicion, Observaciones, IdUsuarioRecibe)
VALUES (@Det, SYSDATETIME(), 'BUENO', NULL, @Encargado);

-- Préstamo 3: finalizado, equipo devuelto dañado
INSERT INTO dbo.Prestamo (IdSolicitante, FechaPrestamo, FechaEsperadaDevolucion, IdUsuarioRegistra, Estado, FechaFinalizacion)
VALUES ((SELECT IdSolicitante FROM dbo.Solicitante WHERE Carne = '0900-23-2050'),
        DATEADD(DAY, -20, SYSDATETIME()), DATEADD(DAY, -15, SYSDATETIME()), @Encargado, 'FINALIZADO', DATEADD(DAY, -16, SYSDATETIME()));
SET @P3 = SCOPE_IDENTITY();

INSERT INTO dbo.DetallePrestamo (IdPrestamo, IdEquipo, Estado) VALUES
(@P3, (SELECT IdEquipo FROM dbo.Equipo WHERE CodigoInventario = 'PRO-002'), 'DEVUELTO');
SET @Det = SCOPE_IDENTITY();

INSERT INTO dbo.Devolucion (IdDetallePrestamo, FechaDevolucion, Condicion, Observaciones, IdUsuarioRecibe)
VALUES (@Det, DATEADD(DAY, -16, SYSDATETIME()), 'DANADO', 'Lámpara no enciende', @Encargado);
GO

/* ---------- Mantenimientos ---------- */
DECLARE @Encargado INT = (SELECT IdUsuario FROM dbo.Usuario WHERE NombreUsuario = 'encargado');

-- Abierto: SWI-001 está en MANTENIMIENTO
INSERT INTO dbo.Mantenimiento (IdEquipo, FechaIngreso, TipoMantenimiento, Descripcion, Responsable, Estado, IdUsuarioRegistra)
VALUES ((SELECT IdEquipo FROM dbo.Equipo WHERE CodigoInventario = 'SWI-001'),
        DATEADD(DAY, -2, SYSDATETIME()), 'PREVENTIVO', 'Revisión y limpieza de puertos', 'Técnico interno - Jorge Pérez', 'EN_PROCESO', @Encargado);

-- Finalizado: MON-001 no se pudo reparar y quedó de BAJA
INSERT INTO dbo.Mantenimiento (IdEquipo, FechaIngreso, TipoMantenimiento, Descripcion, Responsable, FechaSalida, Resultado, Estado, Observaciones, IdUsuarioRegistra)
VALUES ((SELECT IdEquipo FROM dbo.Equipo WHERE CodigoInventario = 'MON-001'),
        DATEADD(DAY, -30, SYSDATETIME()), 'CORRECTIVO', 'Pantalla quebrada', 'Servicio externo - TecniGuate',
        DATEADD(DAY, -25, SYSDATETIME()), 'NO_REPARABLE', 'FINALIZADO', 'Costo de reparación mayor al valor del equipo', @Encargado);
GO

/* ---------- Auditoría de ejemplo ---------- */
INSERT INTO dbo.Auditoria (IdUsuario, NombreUsuario, Accion, Entidad, IdRegistro, Descripcion) VALUES
((SELECT IdUsuario FROM dbo.Usuario WHERE NombreUsuario = 'admin'), 'admin', 'LOGIN_EXITOSO', 'Usuario',
 (SELECT IdUsuario FROM dbo.Usuario WHERE NombreUsuario = 'admin'), 'Inicio de sesión correcto'),
(NULL, 'noexiste', 'LOGIN_FALLIDO', 'Usuario', NULL, 'Usuario no registrado');
GO

/* ---------- Verificación rápida ---------- */
SELECT * FROM dbo.vw_Dashboard;
SELECT * FROM dbo.vw_PrestamoResumen ORDER BY IdPrestamo;
GO

PRINT 'Datos iniciales de Sigelab cargados correctamente.';
GO
