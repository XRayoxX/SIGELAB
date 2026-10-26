@echo off
REM Primer despliegue de SIGELAB Facial API en Azure. Doble clic para ejecutar.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy\deploy-azure.ps1"
echo.
pause
