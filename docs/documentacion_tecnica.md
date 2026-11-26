---
title: "MotorBD: documentación técnica"
subtitle: "Motor de base de datos relacional con índice AVL · Ciencias de la Computación I"
author: "Integrantes: ______________________ · ______________________ · ______________________"
date: "Noviembre de 2026"
---

# 1. Diseño arquitectónico

MotorBD es un gestor de bases de datos **relacional** escrito en Java 17 sin librerías externas (JUnit solo para pruebas). Sigue el patrón **Modelo-Vista-Controlador (MVC)**:

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Vista | `vista` | `VistaConsola` (REPL obligatorio) y `VistaGrafica` (Swing, extra). Solo leen texto y muestran un `Resultado`. |
| Controlador | `controlador` | `Lexer` (texto → tokens), `Parser` (tokens → `Comando`), `Controlador` (ejecuta el comando contra el modelo y arma el `Resultado`). |
| Modelo: almacenamiento | `modelo`, `modelo.datos` | `BaseDatos` (fachada, catálogo y atomicidad), `Tabla` (filas e índices), `Fila`, `Condicion`. |
| Modelo: índice | `modelo.indice` | `AVLArbol` y `AVLNodo`, implementación propia. |
| Modelo: esquema | `modelo.esquema` | `Tipo` (INT, TEXT, REAL, BOOL), `Campo`, `Esquema`. |
| Modelo: persistencia | `modelo.persistencia` | `Persistencia` (archivos y escritura atómica), `Json` (lector y escritor propios). |

**Flujo de un comando:** la vista envía la línea al `Controlador` → `Lexer` la parte en tokens → `Parser` construye un objeto `Comando` → el controlador llama al método de `BaseDatos` → `BaseDatos` valida, cambia los árboles en memoria y guarda en disco → el controlador devuelve un `Resultado` (mensaje, filas, columnas y plan) → la vista lo muestra.

Como el controlador no imprime nada, la consola y la GUI comparten exactamente la misma lógica.

**Organización en memoria de una tabla:**

- **Índice primario:** `AVLArbol<clave primaria, Fila>`. Es el almacenamiento principal de la tabla.
- **Índices secundarios** (uno por cada `CREATE INDEX`): `AVLArbol<valor del campo, lista ordenada de claves primarias>`. Se usa una lista porque varios registros pueden compartir un valor.

# 2. Lenguaje de comandos

Palabras clave sin distinguir mayúsculas, `;` final opcional, textos entre comillas dobles o simples (`""` escapa una comilla). Gramática:

```
comando     := CREATE TABLE id '(' id tipo [PRIMARY KEY] (',' id tipo [PRIMARY KEY])* ')'
             | DROP TABLE [IF EXISTS] id
             | CREATE INDEX ON id '(' id ')'   |  DROP INDEX ON id '(' id ')'
             | SHOW TABLES  |  DESCRIBE id
             | INSERT INTO id ['(' id (',' id)* ')'] VALUES tupla (',' tupla)*
             | SELECT ('*' | id (',' id)*) FROM id [WHERE condicion]
             | UPDATE id SET id '=' literal (',' id '=' literal)* [WHERE condicion]
             | DELETE FROM id [WHERE condicion]
             | LOAD id texto  |  SOURCE texto  |  TREE id [id]  |  HELP  |  EXIT
tupla       := '(' literal (',' literal)* ')'
condicion   := id ('=' | '!=' | '<>' | '<' | '<=' | '>' | '>=') literal
             | id BETWEEN literal AND literal
tipo        := INT | TEXT | REAL | BOOL
literal     := entero | real | texto | TRUE | FALSE
```

**Reglas semánticas:** cada tabla tiene exactamente una clave primaria; todos los campos son obligatorios (no hay NULL); la clave primaria no se puede modificar con UPDATE; un entero se acepta en un campo REAL (3 → 3.0) pero no al revés. `UPDATE` y `DELETE` sin `WHERE` afectan a todas las filas. `LOAD` lee un CSV con encabezado y `SOURCE` ejecuta un archivo con un comando por línea.

**Plan de ejecución.** Para cada condición el motor elige el camino y lo informa:

| Condición sobre... | Camino | Costo |
|---|---|---|
| clave primaria (`=`, `<`, `<=`, `>`, `>=`, `BETWEEN`) | rango sobre el AVL primario | O(log n + m) |
| campo con índice | rango sobre el AVL secundario + búsqueda de cada clave en el primario | O(log n + m log n) |
| campo sin índice, o operador `!=` | recorrido inorden completo filtrando | O(n) |

# 3. Diseño del árbol AVL

`AVLArbol<K, V>` es genérico y recibe un `Comparator`. Cada `AVLNodo` guarda clave, valor, hijo izquierdo, hijo derecho y altura (hoja = 1, nulo = 0).

**Invariantes**, verificados por el método `esValido()` en las pruebas:

1. **Orden BST:** todas las claves del subárbol izquierdo son menores que la del nodo y las del derecho son mayores. No hay claves repetidas.
2. **Altura correcta:** `altura(n) = 1 + max(altura(izq), altura(der))`.
3. **Balance:** `FB(n) = altura(izq) − altura(der)` pertenece a {−1, 0, 1} en todos los nodos.

**Cómo se mantienen.** Inserción y eliminación son recursivas. Al volver de cada llamada se ejecuta `rebalancear(nodo)`: se recalcula la altura y, si |FB| = 2, se aplica una rotación:

| Caso | Condición | Corrección |
|---|---|---|
| Izquierda-Izquierda | FB(n) = +2 y FB(izq) ≥ 0 | rotación simple a la derecha |
| Izquierda-Derecha | FB(n) = +2 y FB(izq) < 0 | rotar a la izquierda el hijo, luego a la derecha el nodo |
| Derecha-Derecha | FB(n) = −2 y FB(der) ≤ 0 | rotación simple a la izquierda |
| Derecha-Izquierda | FB(n) = −2 y FB(der) > 0 | rotar a la derecha el hijo, luego a la izquierda el nodo |

Una rotación solo cambia tres punteros y recalcula dos alturas, así que es O(1) y conserva el orden BST.

**Eliminación con dos hijos:** el nodo toma la clave y el valor de su **sucesor inorden** (mínimo del subárbol derecho) y ese sucesor se elimina del subárbol derecho, rebalanceando el camino.

**Búsqueda por rango:** recorrido inorden con poda. Solo se baja a la izquierda si la clave del nodo es mayor que el mínimo y solo a la derecha si es menor que el máximo. Un límite nulo significa "sin límite".

**Altura garantizada:** un AVL con n nodos tiene altura menor que 1,44·log₂(n + 2). En las pruebas, 100.000 inserciones en orden (el peor caso de un BST sin balance) dejan una altura de 17; con 10.000 claves aleatorias, la altura medida es 16.

# 4. Persistencia y recuperación

**Formato.** Una carpeta de datos (por defecto `./data`) con:

- `catalogo.json`: versión y, por tabla, nombre, campos (nombre, tipo, primario) y lista de campos indexados.
- `<tabla>.json`: `{"tabla": ..., "filas": [ {campo: valor, ...}, ... ]}`, con las filas ordenadas por clave primaria.

```json
{ "tabla": "estudiantes",
  "filas": [ { "id": 20261001, "nombre": "Andres Gomez", "carrera": "Industrial",
               "semestre": 4, "promedio": 4.9, "activo": true } ] }
```

**Escritura atómica.** Toda escritura va a `<archivo>.tmp`, se fuerza a disco (`FileChannel.force`) y luego se renombra sobre el original con `Files.move(..., ATOMIC_MOVE)`. El renombre es atómico, así que el archivo queda completo en su versión vieja o en la nueva, nunca a medias.

**Atomicidad por operación** (`BaseDatos`):

1. **Validar** todo sin tocar nada: tabla, campos, tipos, claves repetidas (también dentro del mismo INSERT múltiple o CSV).
2. **Aplicar** el cambio en los árboles en memoria.
3. **Guardar** la tabla. Si falla (disco lleno, permisos), se ejecuta la acción inversa en memoria (reinsertar lo borrado, restaurar las filas viejas o quitar lo insertado) y se informa el error.

Así, si una operación falla, ni la memoria ni el disco cambian. Un UPDATE o DELETE de varias filas y un LOAD completo se aplican enteros o no se aplican.

**Recuperación al iniciar.** Se borran los `.tmp` huérfanos de una escritura interrumpida, se lee el catálogo, se cargan las filas de cada tabla validando sus tipos y se **reconstruyen** el índice primario y los secundarios: O(n log n). Los árboles no se guardan en disco. Si un archivo está dañado, el motor no arranca, muestra el archivo y la posición del error, y **no** sobrescribe nada.

**Concurrencia.** No se implementa: un solo usuario por carpeta de datos.

# 5. Análisis de complejidad

n = filas de la tabla, m = filas del resultado, k = índices secundarios de la tabla, L = largo del comando.

| Operación | Tiempo | Espacio adicional |
|---|---|---|
| Lexer y Parser | O(L) | O(L) |
| AVL: insertar, buscar, eliminar | O(log n) | O(log n) por la recursión |
| AVL: recorrido inorden | O(n) | O(n) |
| AVL: rango | O(log n + m) | O(m) |
| SELECT por clave primaria (igualdad o rango) | O(log n + m) | O(m) |
| SELECT por campo indexado | O(log n + m log n) | O(m) |
| SELECT sin índice | O(n) | O(m) |
| INSERT de una fila (memoria) | O((k + 1) log n) | O(1) |
| UPDATE / DELETE con WHERE (memoria) | costo del SELECT + O(m (k + 1) log n) | O(m) para deshacer |
| Guardar una tabla en disco | O(n) | O(n) |
| CREATE INDEX | O(n log n) | O(n) |
| Cargar al iniciar o LOAD de n filas | O(n log n) | O(n) |

Cada INSERT, UPDATE o DELETE termina guardando la tabla completa, así que el costo total de una escritura es O(n). Es el precio de un formato JSON simple con escritura atómica. Para cargas masivas se usa `LOAD`, que valida, inserta todo y guarda una sola vez.

Memoria total: O(n) por tabla más O(n) por cada índice secundario.

**Mediciones** (10.000 filas, portátil común): búsqueda por clave alrededor de 0,2 ms, rango sobre índice secundario con 350 resultados alrededor de 0,5 ms y búsqueda por campo sin índice alrededor de 5 ms. Con 100.000 filas, el árbol primario tiene altura 20.

# 6. Datos y pruebas

**Datasets** (`datasets/`, generados con semilla fija por `GeneradorDatasets`): `pequeno.sql`, con 20 estudiantes insertados en orden aleatorio y dos índices; `mediano.csv`, con 10.000 filas; y `cargar_mediano.sql`, que crea la tabla, los índices y ejecuta el `LOAD`. Se cargan con `scripts/cargar_datasets.sh` o `.bat`.

**Pruebas** (`mvn test`, 45 casos con JUnit 5, todos en verde):

- `AVLArbolTest`: las 4 rotaciones; eliminación de una hoja, de un nodo con un hijo y de uno con dos hijos; inorden; rangos; altura logarítmica con 100.000 claves ordenadas; 20.000 operaciones aleatorias comparadas contra `java.util.TreeMap`.
- `BaseDatosTest`: inserción, búsqueda por clave, por rango y por campo indexado; UPDATE y DELETE por condición; validación de tipos; claves repetidas.
- `PersistenciaTest`: recuperación tras reinicio con índices reconstruidos; archivo dañado no sobrescrito; fallo de disco simulado que deshace INSERT, UPDATE y DELETE; temporales huérfanos.
- `ControladorTest`: lexer, parser y errores de sintaxis; comandos completos; scripts; carga de los datasets reales y reinicio.
