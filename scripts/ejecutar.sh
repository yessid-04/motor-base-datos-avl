#!/usr/bin/env sh
# Compila (si hace falta) y abre el motor. Uso: ./scripts/ejecutar.sh [--gui]
cd "$(dirname "$0")/.." || exit 1
if [ ! -f target/motorbd.jar ]; then
  mvn -q -DskipTests package || exit 1
fi
java -jar target/motorbd.jar "$@"
