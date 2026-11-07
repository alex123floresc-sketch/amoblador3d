@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0crear-instalador.ps1" %*
pause
