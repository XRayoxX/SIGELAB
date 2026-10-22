package SIGELAB.controller;

import SIGELAB.model.Usuario;
import SIGELAB.service.AuthService;

/**
 * Puente entre la ventana Login y AuthService.
 */
public class LoginController {

    private final AuthService service = new AuthService();

    /**
     * Ejecuta el login completo y devuelve el usuario autenticado.
     * Lanza BusinessException con un mensaje para el usuario si algo no se cumple.
     */
    public Usuario iniciarSesion(String nombreUsuario, char[] contrasena) {
        Usuario usuario = service.validarCredenciales(nombreUsuario, new String(contrasena));

        // TODO RN-02 (Etapa 4): aquí se llamará al servicio facial (Python + OpenCV).
        //      Si el rostro no corresponde al usuario, lanzar BusinessException y no completar el login.

        service.completarLogin(usuario);
        return usuario;
    }

    public void cerrarSesion() {
        service.cerrarSesion();
    }
}
