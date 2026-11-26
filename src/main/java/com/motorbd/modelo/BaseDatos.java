package com.motorbd.modelo;

import com.motorbd.modelo.datos.Condicion;
import com.motorbd.modelo.datos.Fila;
import com.motorbd.modelo.datos.ResultadoBusqueda;
import com.motorbd.modelo.datos.Tabla;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Esquema;
import com.motorbd.modelo.esquema.Identificadores;
import com.motorbd.modelo.indice.AVLArbol;
import com.motorbd.modelo.persistencia.Persistencia;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Fachada del MODELO. Es la unica clase que el controlador usa para cambiar datos.
 *
 * Regla de atomicidad por operacion, en tres pasos:
 *   1. Validar todo (tabla, campos, tipos, claves repetidas) sin tocar nada.
 *   2. Aplicar el cambio en memoria (arboles AVL).
 *   3. Guardar en disco con escritura atomica. Si guardar falla, se deshace
 *      el paso 2, de modo que memoria y disco siguen iguales a antes.
 * Una operacion sobre varias filas (UPDATE/DELETE con WHERE, LOAD) se aplica completa o no se aplica.
 */
public class BaseDatos {

    private final Persistencia persistencia;
    /** nombre en minusculas -> tabla. TreeMap para listarlas en orden. */
    private final Map<String, Tabla> tablas = new TreeMap<>();

    public BaseDatos(Path carpeta) {
        this(new Persistencia(carpeta));
    }

    /** Carga todo lo que haya en disco y reconstruye los indices. */
    public BaseDatos(Persistencia persistencia) {
        this.persistencia = persistencia;
        this.tablas.putAll(persistencia.cargarTodo());
    }

    public Path getCarpeta() {
        return persistencia.getCarpeta();
    }

    // ------------------------------------------------------------------
    // Definicion de datos (DDL)
    // ------------------------------------------------------------------

    public void crearTabla(String nombre, List<Campo> campos) {
        Identificadores.validar(nombre, "tabla");
        if (tablas.containsKey(nombre.toLowerCase())) {
            throw new ErrorMotor("La tabla '" + nombre + "' ya existe.");
        }
        Tabla tabla = new Tabla(nombre, new Esquema(campos));
        tablas.put(nombre.toLowerCase(), tabla);
        try {
            persistencia.guardarTabla(tabla);
            persistencia.guardarCatalogo(tablas.values());
        } catch (IOException e) {
            tablas.remove(nombre.toLowerCase());
            throw errorDisco(e);
        }
    }

    public void borrarTabla(String nombre) {
        Tabla tabla = tabla(nombre);
        tablas.remove(tabla.getNombre().toLowerCase());
        try {
            persistencia.guardarCatalogo(tablas.values());
        } catch (IOException e) {
            tablas.put(tabla.getNombre().toLowerCase(), tabla);
            throw errorDisco(e);
        }
        try {
            persistencia.borrarArchivoTabla(tabla.getNombre());
        } catch (IOException e) {
            // El catalogo ya no la nombra: el archivo sobrante no afecta la consistencia.
        }
    }

    public void crearIndice(String nombreTabla, String campo) {
        Tabla tabla = tabla(nombreTabla);
        tabla.crearIndice(campo);
        try {
            persistencia.guardarCatalogo(tablas.values());
        } catch (IOException e) {
            tabla.borrarIndice(campo);
            throw errorDisco(e);
        }
    }

    public void borrarIndice(String nombreTabla, String campo) {
        Tabla tabla = tabla(nombreTabla);
        tabla.borrarIndice(campo);
        try {
            persistencia.guardarCatalogo(tablas.values());
        } catch (IOException e) {
            tabla.crearIndice(campo);
            throw errorDisco(e);
        }
    }

    public List<Tabla> listarTablas() {
        return new ArrayList<>(tablas.values());
    }

    public Tabla tabla(String nombre) {
        Tabla t = tablas.get(nombre.toLowerCase());
        if (t == null) {
            throw new ErrorMotor("La tabla '" + nombre + "' no existe.");
        }
        return t;
    }

    public boolean existeTabla(String nombre) {
        return tablas.containsKey(nombre.toLowerCase());
    }

    // ------------------------------------------------------------------
    // Manipulacion de datos (DML)
    // ------------------------------------------------------------------

    /**
     * Inserta una o varias filas. columnas == null significa "en el orden del esquema".
     * Todas las filas se validan antes de insertar la primera.
     *
     * @return numero de filas insertadas
     */
    public int insertar(String nombreTabla, List<String> columnas, List<List<Object>> filas) {
        Tabla tabla = tabla(nombreTabla);
        Esquema esquema = tabla.getEsquema();
        List<Fila> nuevas = new ArrayList<>();
        Set<Object> clavesNuevas = new HashSet<>();
        String pk = esquema.getPrimario().getNombre();

        for (List<Object> literales : filas) {
            Fila fila = construirFila(esquema, columnas, literales);
            Object clave = fila.get(pk);
            if (tabla.buscarPorClave(clave) != null || !clavesNuevas.add(clave)) {
                throw new ErrorMotor("Ya existe un registro con " + pk + " = " + clave
                        + ". No se inserto ninguna fila.");
            }
            nuevas.add(fila);
        }
        for (Fila f : nuevas) {
            tabla.insertar(f);
        }
        guardarODeshacer(tabla, () -> {
            for (Fila f : nuevas) {
                tabla.eliminarPorClave(f.get(pk));
            }
        });
        return nuevas.size();
    }

    /** SELECT: devuelve las filas y el plan de ejecucion usado. */
    public ResultadoBusqueda seleccionar(String nombreTabla, Condicion condicion) {
        Tabla tabla = tabla(nombreTabla);
        return tabla.buscar(condicion == null ? null : condicion.normalizar(tabla.getEsquema()));
    }

    /**
     * UPDATE ... SET ... WHERE. Cambia todas las filas que cumplen la condicion.
     * No se permite modificar la clave primaria.
     *
     * @return numero de filas actualizadas
     */
    public int actualizar(String nombreTabla, Map<String, Object> cambios, Condicion condicion) {
        Tabla tabla = tabla(nombreTabla);
        Esquema esquema = tabla.getEsquema();
        if (cambios.isEmpty()) {
            throw new ErrorMotor("UPDATE necesita al menos un campo en SET.");
        }
        // 1. Validar y convertir los nuevos valores
        Map<String, Object> convertidos = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : cambios.entrySet()) {
            Campo c = esquema.campo(e.getKey());
            if (c.esPrimario()) {
                throw new ErrorMotor("No se permite modificar la clave primaria '" + c.getNombre() + "'.");
            }
            convertidos.put(c.getNombre(), c.getTipo().convertir(e.getValue(), c.getNombre()));
        }
        List<Fila> afectadas = seleccionar(nombreTabla, condicion).getFilas();
        List<Fila[]> pares = new ArrayList<>();
        for (Fila vieja : afectadas) {
            Map<String, Object> valores = vieja.comoMapa();
            valores.putAll(convertidos);
            pares.add(new Fila[] {vieja, new Fila(valores)});
        }
        // 2. Aplicar en memoria
        for (Fila[] p : pares) {
            tabla.reemplazar(p[0], p[1]);
        }
        // 3. Guardar o deshacer
        guardarODeshacer(tabla, () -> {
            for (Fila[] p : pares) {
                tabla.reemplazar(p[1], p[0]);
            }
        });
        return pares.size();
    }

    /**
     * DELETE ... WHERE. Sin condicion borra todas las filas.
     *
     * @return numero de filas eliminadas
     */
    public int eliminar(String nombreTabla, Condicion condicion) {
        Tabla tabla = tabla(nombreTabla);
        String pk = tabla.getEsquema().getPrimario().getNombre();
        List<Fila> afectadas = new ArrayList<>(seleccionar(nombreTabla, condicion).getFilas());
        for (Fila f : afectadas) {
            tabla.eliminarPorClave(f.get(pk));
        }
        guardarODeshacer(tabla, () -> {
            for (Fila f : afectadas) {
                tabla.insertar(f);
            }
        });
        return afectadas.size();
    }

    /**
     * Carga masiva desde un CSV cuya primera linea son los nombres de los campos.
     * Se valida todo el archivo antes de insertar; se guarda una sola vez al final.
     *
     * @return numero de filas cargadas
     */
    public int cargarCsv(String nombreTabla, Path archivo) {
        Tabla tabla = tabla(nombreTabla);
        Esquema esquema = tabla.getEsquema();
        List<String> columnas;
        List<List<Object>> filas = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String encabezado = lector.readLine();
            if (encabezado == null) {
                throw new ErrorMotor("El archivo CSV esta vacio.");
            }
            columnas = Csv.separar(encabezado.replace("\uFEFF", ""));
            List<Campo> campos = new ArrayList<>();
            for (String col : columnas) {
                campos.add(esquema.campo(col.trim()));
            }
            String linea;
            int numero = 1;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (linea.isBlank()) {
                    continue;
                }
                List<String> celdas = Csv.separar(linea);
                if (celdas.size() != campos.size()) {
                    throw new ErrorMotor("Linea " + numero + " del CSV: se esperaban " + campos.size()
                            + " valores y hay " + celdas.size() + ".");
                }
                List<Object> valores = new ArrayList<>();
                for (int i = 0; i < campos.size(); i++) {
                    try {
                        valores.add(campos.get(i).getTipo().desdeTexto(celdas.get(i), campos.get(i).getNombre()));
                    } catch (ErrorMotor e) {
                        throw new ErrorMotor("Linea " + numero + " del CSV: " + e.getMessage());
                    }
                }
                filas.add(valores);
            }
        } catch (IOException e) {
            throw new ErrorMotor("No se pudo leer el archivo '" + archivo + "': " + e.getMessage(), e);
        }
        List<String> nombres = new ArrayList<>();
        for (String c : columnas) {
            nombres.add(c.trim());
        }
        return insertar(nombreTabla, nombres, filas);
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /** Arbol AVL de una tabla (campo null = indice primario). Solo lectura para la vista. */
    public AVLArbol<Object, ?> arbol(String nombreTabla, String campo) {
        return tabla(nombreTabla).arbol(campo);
    }

    private Fila construirFila(Esquema esquema, List<String> columnas, List<Object> literales) {
        List<Campo> campos = esquema.getCampos();
        Map<String, Object> valores = new LinkedHashMap<>();
        if (columnas == null) {
            if (literales.size() != campos.size()) {
                throw new ErrorMotor("Se esperaban " + campos.size() + " valores " + esquema.nombres()
                        + " y llegaron " + literales.size() + ".");
            }
            for (int i = 0; i < campos.size(); i++) {
                Campo c = campos.get(i);
                valores.put(c.getNombre(), c.getTipo().convertir(literales.get(i), c.getNombre()));
            }
        } else {
            if (columnas.size() != literales.size()) {
                throw new ErrorMotor("Hay " + columnas.size() + " columnas y " + literales.size() + " valores.");
            }
            Map<String, Object> dados = new LinkedHashMap<>();
            for (int i = 0; i < columnas.size(); i++) {
                Campo c = esquema.campo(columnas.get(i));
                if (dados.put(c.getNombre(), literales.get(i)) != null) {
                    throw new ErrorMotor("Columna repetida: " + c.getNombre());
                }
            }
            for (Campo c : campos) {
                if (!dados.containsKey(c.getNombre())) {
                    throw new ErrorMotor("Falta el valor del campo '" + c.getNombre() + "'.");
                }
                valores.put(c.getNombre(), c.getTipo().convertir(dados.get(c.getNombre()), c.getNombre()));
            }
        }
        return new Fila(valores);
    }

    private void guardarODeshacer(Tabla tabla, Runnable deshacer) {
        try {
            persistencia.guardarTabla(tabla);
        } catch (IOException | RuntimeException e) {
            deshacer.run();
            throw errorDisco(e);
        }
    }

    private static ErrorMotor errorDisco(Exception e) {
        return new ErrorMotor("No se pudo guardar en disco (" + e.getMessage()
                + "). La operacion se deshizo y los datos quedan como estaban.", e);
    }
}
