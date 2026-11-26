@echo off
REM Compila (si hace falta) y abre el motor. Uso: scripts\ejecutar.bat [--gui]
cd /d "%~dp0.."
if not exist target\motorbd.jar call mvn -q -DskipTests package
java -jar target\motorbd.jar %*
