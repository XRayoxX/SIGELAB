/* ==========================================================================
   SIGELAB - Script 06: Corregir registros guardados en hora UTC (UNA SOLA VEZ)
   Ejecutar DESPUÉS del script 05, solo en la base de Azure, y solo si sus
   registros anteriores se guardaron con la hora UTC del servidor.
   Resta 6 horas a las fechas automáticas ya guardadas.
   Queda marcado en la base: si se ejecuta otra vez, no hace nada.
   ========================================================================== */

-- En Azure, conéctese directamente a la base (sigelabadministrador).

SET XACT_ABORT ON;

IF EXISTS (SELECT 1 FROM sys.extended_properties WHERE class = 0 AND name = 'SIGELAB_FechasConvertidasAGuatemala')
BEGIN
    PRINT 'Las fechas ya habían sido convertidas. No se hizo ningún cambio.';
    RETURN;
END;

BEGIN TRANSACTION;

UPDATE dbo.Auditoria     SET FechaHora       = DATEADD(HOUR, -6, FechaHora);
UPDATE dbo.Usuario       SET FechaCreacion   = DATEADD(HOUR, -6, FechaCreacion);
UPDATE dbo.Equipo        SET FechaRegistro   = DATEADD(HOUR, -6, FechaRegistro);
UPDATE dbo.Prestamo      SET FechaPrestamo           = DATEADD(HOUR, -6, FechaPrestamo),
                             FechaEsperadaDevolucion = DATEADD(HOUR, -6, FechaEsperadaDevolucion),
                             FechaFinalizacion       = DATEADD(HOUR, -6, FechaFinalizacion);
UPDATE dbo.Devolucion    SET FechaDevolucion = DATEADD(HOUR, -6, FechaDevolucion);
UPDATE dbo.Mantenimiento SET FechaIngreso    = DATEADD(HOUR, -6, FechaIngreso),
                             FechaSalida     = DATEADD(HOUR, -6, FechaSalida);

EXEC sys.sp_addextendedproperty @name = N'SIGELAB_FechasConvertidasAGuatemala', @value = N'UTC-6';

COMMIT TRANSACTION;

PRINT 'Registros existentes convertidos a hora de Guatemala.';
