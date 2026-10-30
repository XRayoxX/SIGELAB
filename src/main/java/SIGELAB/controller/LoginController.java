package SIGELAB.controller;

import SIGELAB.facial.VerificacionFacial;
import SIGELAB.model.Usuario;
import SIGELAB.service.AuthService;

public class LoginController {

    private final AuthService service = new AuthService();

    public Usuario validarCredenciales(
            String nombreUsuario,
            char[] contrasena
    ) {
        if (contrasena == null) {
            contrasena = new char[0];
        }

        return service.validarCredenciales(
                nombreUsuario,
                new String(contrasena)
        );
    }

    public void completarLogin(
            Usuario usuario,
            VerificacionFacial.Resultado resultadoFacial
    ) {
        service.completarLogin(usuario, resultadoFacial);
    }

    public void registrarFalloFacial(
            Usuario usuario,
            String detalle
    ) {
        service.registrarFalloFacial(usuario, detalle);
    }

    public void cerrarSesion() {
        service.cerrarSesion();
    }
}