package SIGELAB.controller;

import SIGELAB.model.Usuario;
import SIGELAB.service.AuthService;

/**
 * Puente entre la ventana Login y AuthService.
 *
 * El inicio de sesión tiene dos pasos:
 *  1) validarCredenciales: usuario + contraseña + estado ACTIVO (RN-01).
 *  2) Verificación facial (RN-02): la ventana Login usa SIGELAB.facial.VerificacionFacial.
 *     Solo si el rostro es aprobado se llama a completarLogin.
 */
public class LoginController {

    private final AuthService service = new AuthService();

    /** Paso 1. Lanza BusinessException con un mensaje para el usuario si algo no se cumple. */
    public Usuario validarCredenciales(String nombreUsuario, char[] contrasena) {
        return service.validarCredenciales(nombreUsuario, new String(contrasena));
    }

    /** Paso final: abre la sesión y registra LOGIN_EXITOSO en la auditoría. */
    public void completarLogin(Usuario usuario) {
        service.completarLogin(usuario);
    }

    /** Registra LOGIN_FACIAL_FALLIDO en la auditoría. */
    public void registrarFalloFacial(Usuario usuario, String detalle) {
        service.registrarFalloFacial(usuario, detalle);
    }

    public void cerrarSesion() {
        service.cerrarSesion();
    }
}
