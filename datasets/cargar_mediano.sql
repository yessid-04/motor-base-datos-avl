-- Dataset mediano. Ejecutar con: SOURCE "datasets/cargar_mediano.sql"
DROP TABLE IF EXISTS estudiantes_m
CREATE TABLE estudiantes_m (id INT PRIMARY KEY, nombre TEXT, carrera TEXT, semestre INT, promedio REAL, activo BOOL)
CREATE INDEX ON estudiantes_m (promedio)
CREATE INDEX ON estudiantes_m (carrera)
LOAD estudiantes_m "datasets/mediano.csv"
