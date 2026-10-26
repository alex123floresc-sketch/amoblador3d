@echo off
rem Doble clic para crear Amoblador3D para Windows (ver crear-instalador.ps1).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0crear-instalador.ps1" %*
pause
