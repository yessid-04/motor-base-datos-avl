#!/usr/bin/env sh
# Regenera los datasets. Uso: ./scripts/generar_datasets.sh [filas_mediano]
cd "$(dirname "$0")/.." || exit 1
mvn -q -DskipTests compile || exit 1
java -cp target/classes com.motorbd.herramientas.GeneradorDatasets datasets "${1:-10000}"
