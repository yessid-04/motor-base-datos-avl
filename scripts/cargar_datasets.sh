#!/usr/bin/env sh
# Carga el dataset pequeno (20 filas) y el mediano (10.000 filas) en ./data
cd "$(dirname "$0")/.." || exit 1
if [ ! -f target/motorbd.jar ]; then
  mvn -q -DskipTests package || exit 1
fi
java -jar target/motorbd.jar --script datasets/pequeno.sql
java -jar target/motorbd.jar --script datasets/cargar_mediano.sql
