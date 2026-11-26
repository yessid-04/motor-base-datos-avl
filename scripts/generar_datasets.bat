@echo off
REM Regenera los datasets. Uso: scripts\generar_datasets.bat [filas_mediano]
cd /d "%~dp0.."
call mvn -q -DskipTests compile
set N=%1
if "%N%"=="" set N=10000
java -cp target\classes com.motorbd.herramientas.GeneradorDatasets datasets %N%
