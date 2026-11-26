@echo off
REM Carga el dataset pequeno y el mediano en .\data
cd /d "%~dp0.."
if not exist target\motorbd.jar call mvn -q -DskipTests package
java -jar target\motorbd.jar --script datasets\pequeno.sql
java -jar target\motorbd.jar --script datasets\cargar_mediano.sql
