@echo off
REM Sube a Azure los cambios del codigo Python (despues del primer despliegue).
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy\deploy-azure.ps1" -SoloActualizarCodigo
echo.
pause
