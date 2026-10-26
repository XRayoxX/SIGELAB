package SIGELAB.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lee config.properties una sola vez y lo comparte con toda la aplicación.
 *
 * Orden de búsqueda:
 *  1) Junto al ejecutable: dist\SIGELAB\app\config.properties (empaquetar.ps1 lo copia ahí),
 *     así se puede cambiar la configuración en cualquier PC sin recompilar.
 *  2) Dentro del classpath: src/main/resources/config.properties (al correr desde IntelliJ).
 */
public final class Configuracion {

    private static final String ARCHIVO = "config.properties";
    private static Properties propiedades;

    private Configuracion() {
    }

    public static synchronized Properties get() {
        if (propiedades == null) {
            propiedades = cargar();
        }
        return propiedades;
    }

    /** Valor de la clave o null si no existe / está vacío. */
    public static String valor(String clave) {
        String v = get().getProperty(clave);
        return v == null || v.isBlank() ? null : v.trim();
    }

    public static String valor(String clave, String porDefecto) {
        String v = valor(clave);
        return v == null ? porDefecto : v;
    }

    public static boolean existe() {
        return !get().isEmpty();
    }

    private static Properties cargar() {
        Properties p = new Properties();
        Path externo = archivoJuntoAlJar();
        try {
            if (externo != null && Files.isRegularFile(externo)) {
                try (InputStream entrada = Files.newInputStream(externo)) {
                    p.load(entrada);
                    return p;
                }
            }
            try (InputStream entrada = Configuracion.class.getResourceAsStream("/" + ARCHIVO)) {
                if (entrada != null) {
                    p.load(entrada);
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer " + ARCHIVO + ".", e);
        }
        return p;
    }

    // Carpeta donde está sigelab.jar (en el ejecutable: dist\SIGELAB\app)
    private static Path archivoJuntoAlJar() {
        try {
            String appPath = System.getProperty("jpackage.app-path"); // ...\SIGELAB\SIGELAB.exe
            if (appPath != null) {
                Path enApp = Paths.get(appPath).getParent().resolve("app").resolve(ARCHIVO);
                if (Files.isRegularFile(enApp)) {
                    return enApp;
                }
            }
            Path ubicacion = Paths.get(Configuracion.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path carpeta = Files.isDirectory(ubicacion) ? null : ubicacion.getParent(); // solo si es un .jar
            return carpeta == null ? null : carpeta.resolve(ARCHIVO);
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            return null;
        }
    }
}
