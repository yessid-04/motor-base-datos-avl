package com.motorbd.modelo.persistencia;

import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.datos.Fila;
import com.motorbd.modelo.datos.Tabla;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Esquema;
import com.motorbd.modelo.esquema.Tipo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda y recupera la base de datos en una carpeta:
 *
 *   datos/catalogo.json   -> esquema de cada tabla y sus campos indexados
 *   datos/&lt;tabla&gt;.json -> filas de la tabla
 *
 * Escritura atomica: se escribe un archivo .tmp, se fuerza a disco y luego se
 * renombra sobre el original. Un renombre es atomico en el sistema de archivos,
 * asi que el archivo queda o con la version vieja completa o con la nueva completa.
 *
 * Los indices no se guardan: se reconstruyen al cargar (O(n log n)).
 */
public class Persistencia {

    public static final String CATALOGO = "catalogo.json";
    private static final int VERSION = 1;

    private final Path carpeta;

    public Persistencia(Path carpeta) {
        this.carpeta = carpeta;
    }

    public Path getCarpeta() {
        return carpeta;
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    /**
     * Lee el catalogo y todas las tablas, reconstruyendo sus indices.
     * Si algun archivo esta danado lanza ErrorMotor y NO modifica nada en disco.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Tabla> cargarTodo() {
        Map<String, Tabla> tablas = new LinkedHashMap<>();
        try {
            Files.createDirectories(carpeta);
            limpiarTemporales();
            Path cat = carpeta.resolve(CATALOGO);
            if (!Files.exists(cat)) {
                return tablas;
            }
            Map<String, Object> catalogo = (Map<String, Object>) leerJson(cat);
            for (Object o : (List<Object>) catalogo.get("tablas")) {
                Map<String, Object> def = (Map<String, Object>) o;
                String nombre = (String) def.get("nombre");
                List<Campo> campos = new ArrayList<>();
                for (Object c : (List<Object>) def.get("campos")) {
                    Map<String, Object> campo = (Map<String, Object>) c;
                    campos.add(new Campo((String) campo.get("nombre"),
                            Tipo.valueOf((String) campo.get("tipo")),
                            Boolean.TRUE.equals(campo.get("primario"))));
                }
                Tabla tabla = new Tabla(nombre, new Esquema(campos));
                cargarFilas(tabla);
                for (Object idx : (List<Object>) def.get("indices")) {
                    tabla.crearIndice((String) idx);
                }
                tablas.put(nombre.toLowerCase(), tabla);
            }
            return tablas;
        } catch (IOException e) {
            throw new ErrorMotor("No se pudo leer la carpeta de datos: " + e.getMessage(), e);
        } catch (Json.ErrorJson | ClassCastException | NullPointerException | IllegalArgumentException e) {
            throw new ErrorMotor("Archivo de datos danado en " + carpeta + ": " + e.getMessage()
                    + ". No se modifico nada; revise o restaure el archivo.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void cargarFilas(Tabla tabla) throws IOException {
        Path archivo = archivoDe(tabla.getNombre());
        if (!Files.exists(archivo)) {
            return;
        }
        Map<String, Object> contenido = (Map<String, Object>) leerJson(archivo);
        Esquema esquema = tabla.getEsquema();
        for (Object o : (List<Object>) contenido.get("filas")) {
            Map<String, Object> crudo = (Map<String, Object>) o;
            Map<String, Object> valores = new LinkedHashMap<>();
            for (Campo c : esquema.getCampos()) {
                valores.put(c.getNombre(), c.getTipo().convertir(crudo.get(c.getNombre()), c.getNombre()));
            }
            tabla.insertar(new Fila(valores));
        }
    }

    private Object leerJson(Path archivo) throws IOException {
        try {
            return Json.leer(Files.readString(archivo, StandardCharsets.UTF_8));
        } catch (Json.ErrorJson e) {
            throw new Json.ErrorJson(archivo.getFileName() + ": " + e.getMessage());
        }
    }

    /** Borra archivos .tmp que hayan quedado de una escritura interrumpida. */
    private void limpiarTemporales() throws IOException {
        try (var archivos = Files.list(carpeta)) {
            for (Path p : (Iterable<Path>) archivos::iterator) {
                if (p.getFileName().toString().endsWith(".tmp")) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Guardado
    // ------------------------------------------------------------------

    /** Reescribe el catalogo con todas las tablas. O(t), t = numero de tablas. */
    public void guardarCatalogo(Collection<Tabla> tablas) throws IOException {
        List<Object> defs = new ArrayList<>();
        for (Tabla t : tablas) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("nombre", t.getNombre());
            List<Object> campos = new ArrayList<>();
            for (Campo c : t.getEsquema().getCampos()) {
                Map<String, Object> campo = new LinkedHashMap<>();
                campo.put("nombre", c.getNombre());
                campo.put("tipo", c.getTipo().name());
                campo.put("primario", c.esPrimario());
                campos.add(campo);
            }
            def.put("campos", campos);
            def.put("indices", new ArrayList<>(t.camposIndexados()));
            defs.add(def);
        }
        Map<String, Object> catalogo = new LinkedHashMap<>();
        catalogo.put("version", VERSION);
        catalogo.put("tablas", defs);
        escribirAtomico(carpeta.resolve(CATALOGO), Json.escribir(catalogo));
    }

    /** Reescribe el archivo de la tabla con todas sus filas en orden de clave. O(n). */
    public void guardarTabla(Tabla tabla) throws IOException {
        List<Object> filas = new ArrayList<>(tabla.tamanio());
        for (Fila f : tabla.todas()) {
            filas.add(f.comoMapa());
        }
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("tabla", tabla.getNombre());
        contenido.put("filas", filas);
        escribirAtomico(archivoDe(tabla.getNombre()), Json.escribir(contenido));
    }

    public void borrarArchivoTabla(String nombre) throws IOException {
        Files.deleteIfExists(archivoDe(nombre));
    }

    private Path archivoDe(String nombreTabla) {
        return carpeta.resolve(nombreTabla.toLowerCase() + ".json");
    }

    /**
     * Escribe el contenido en un temporal, lo fuerza a disco y lo renombra
     * sobre el destino. Si falla antes del renombre, el destino no cambia.
     */
    protected void escribirAtomico(Path destino, String contenido) throws IOException {
        Files.createDirectories(carpeta);
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        try (FileChannel canal = FileChannel.open(tmp, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(contenido.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                canal.write(buffer);
            }
            canal.force(true);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        try {
            Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
