@echo off
rem Doble clic para abrir Amoblador3D (necesita Java 21 o superior).
cd /d "%~dp0"
echo Abriendo Amoblador3D... (la primera vez tarda porque descarga lo necesario)
call "%~dp0mvnw.cmd" -q javafx:run
if errorlevel 1 (
  echo.
  echo No se pudo abrir. Revisa el mensaje de arriba o GUIA_EJECUCION.md
  pause
)
