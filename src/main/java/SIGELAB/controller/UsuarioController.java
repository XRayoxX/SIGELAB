package SIGELAB.controller;

import SIGELAB.model.Usuario;
import SIGELAB.service.UsuarioService;

import java.util.List;

/**
 * Puente entre FrmUsuarios y UsuarioService.
 */
public class UsuarioController {

    private final UsuarioService service = new UsuarioService();

    public List<Usuario> listar() {
        return service.listar();
    }

    public List<Usuario> buscar(String texto) {
        return service.buscar(texto);
    }

    public Usuario guardar(Usuario u, char[] contrasena, char[] confirmacion) {
        return service.registrar(u, new String(contrasena), new String(confirmacion));
    }

    public void actualizar(Usuario u, char[] contrasena, char[] confirmacion) {
        service.actualizar(u, new String(contrasena), new String(confirmacion));
    }

    public void desactivar(int idUsuario) {
        service.desactivar(idUsuario);
    }
}
