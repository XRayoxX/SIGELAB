package SIGELAB.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Única clase que sabe CÓMO conectarse a SQL Server (local o Azure SQL).
 * Los datos (url, usuario, contraseña) se leen de src/main/resources/config.properties,
 * así la contraseña no queda escrita dentro del código ni se sube a Git.
 */
public final class ConexionBD {

    // Azure SQL serverless responde con este código mientras la base pausada "despierta"
    private static final int ERROR_BASE_NO_DISPONIBLE = 40613;
    private static final int INTENTOS = 3;
    private static final long ESPERA_ENTRE_INTENTOS_MS = 5000;

    private static String url;
    private static String usuario;
    private static String contrasena;

    private ConexionBD() {
    }


    private static synchronized void cargarConfiguracion() {
        if (url != null) {
            return;
        }
        if (!Configuracion.existe()) {
            throw new DataAccessException(
                    "No se encontró config.properties. Copie config.properties.example como config.properties y configure la conexión.");
        }
        Properties propiedades = Configuracion.get();
        url = obligatoria(propiedades, "db.url");
        usuario = obligatoria(propiedades, "db.usuario");
        contrasena = obligatoria(propiedades, "db.contrasena");
    }

    private static String obligatoria(Properties propiedades, String clave) {
        String valor = propiedades.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new DataAccessException("Falta el valor de " + clave + " en config.properties.");
        }
        return valor.trim();
    }

    /**
     * Abre una conexión nueva. Quien la pide debe cerrarla, idealmente con try-with-resources:
     * try (Connection cn = ConexionBD.obtenerConexion()) { ... }
     */
    public static Connection obtenerConexion() {
        cargarConfiguracion();
        SQLException ultimoError = null;
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                return DriverManager.getConnection(url, usuario, contrasena);
            } catch (SQLException e) {
                ultimoError = e;
                // Solo se reintenta si la base serverless está reanudándose; otros errores fallan de inmediato
                if (e.getErrorCode() != ERROR_BASE_NO_DISPONIBLE || intento == INTENTOS) {
                    break;
                }
                esperar();
            }
        }
        throw new DataAccessException(
                "No se pudo conectar con la base de datos. Verifique su conexión a internet y los datos de config.properties.",
                ultimoError);
    }

    private static void esperar() {
        try {
            Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
