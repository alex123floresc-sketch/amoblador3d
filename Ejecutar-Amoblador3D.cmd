@echo off
cd /d "%~dp0"
echo Abriendo Space Cat... (la primera vez tarda porque descarga lo necesario)
call "%~dp0mvnw.cmd" -q javafx:run
if errorlevel 1 (
  echo.
  echo No se pudo abrir. Revisa el mensaje de arriba o el Manual de instalacion
  pause
)
