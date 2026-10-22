package SIGELAB.util;

import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;

/**
 * Guarda el usuario que inició sesión para que cualquier capa sepa quién está operando
 * (por ejemplo, para registrar la auditoría o el "usuario que registra" un préstamo).
 */
public final class Sesion {

    private static Usuario usuarioActual;

    private Sesion() {
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static void cerrar() {
        usuarioActual = null;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    /**
     * Validación de permisos en la lógica (no solo ocultando botones).
     * Los Service la llaman antes de operar.
     */
    public static void exigirRol(RolUsuario... roles) {
        if (usuarioActual == null) {
            throw new BusinessException("Debe iniciar sesión para realizar esta operación.");
        }
        if (!tieneRol(roles)) {
            throw new BusinessException("Su rol (" + usuarioActual.getRol() + ") no tiene permiso para realizar esta operación.");
        }
    }

    public static int getIdUsuario() {
        if (usuarioActual == null) {
            throw new BusinessException("Debe iniciar sesión para realizar esta operación.");
        }
        return usuarioActual.getIdUsuario();
    }

    public static boolean tieneRol(RolUsuario... roles) {
        if (usuarioActual == null) {
            return false;
        }
        for (RolUsuario rol : roles) {
            if (usuarioActual.getRol() == rol) {
                return true;
            }
        }
        return false;
    }
}
