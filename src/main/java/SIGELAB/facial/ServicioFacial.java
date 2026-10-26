package SIGELAB.facial;

import SIGELAB.util.Configuracion;

/**
 * Acceso único a la API de reconocimiento facial (Azure).
 * La URL y la API key se leen de config.properties:
 *
 *   facial.habilitado=true
 *   facial.api.url=https://sigelab-facial-api....azurecontainerapps.io
 *   facial.api.key=SU_API_KEY
 */
public final class ServicioFacial {

    private static FacialApiClient cliente;

    private ServicioFacial() {
    }

    /** false si se desactivó en config.properties (facial.habilitado=false) o falta la URL/API key. */
    public static boolean habilitado() {
        return Boolean.parseBoolean(Configuracion.valor("facial.habilitado", "true"))
                && Configuracion.valor("facial.api.url") != null
                && Configuracion.valor("facial.api.key") != null;
    }

    public static synchronized FacialApiClient cliente() {
        if (cliente == null) {
            cliente = new FacialApiClient(
                    Configuracion.valor("facial.api.url"),
                    Configuracion.valor("facial.api.key"),
                    "sigelab-desktop");
        }
        return cliente;
    }

    /**
     * Si Azure tenía la API apagada (escala a cero), la enciende en segundo plano
     * mientras el usuario escribe su contraseña. No bloquea la interfaz.
     */
    public static void despertar() {
        if (!habilitado()) {
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                cliente().salud();
            } catch (Exception ignorado) {
                // Si falla aquí, se informará al intentar la verificación.
            }
        }, "despertar-api-facial");
        hilo.setDaemon(true);
        hilo.start();
    }
}
