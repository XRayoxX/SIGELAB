package SIGELAB.controller;

import SIGELAB.model.Auditoria;
import SIGELAB.model.Usuario;
import SIGELAB.service.AuditoriaService;
import SIGELAB.service.UsuarioService;
import SIGELAB.util.Fechas;

import java.util.List;

/**
 * Puente entre FrmAuditoria y AuditoriaService. Convierte los filtros "TODOS/TODAS" en null.
 */
public class AuditoriaController {

    private final AuditoriaService service = new AuditoriaService();
    private final UsuarioService usuarioService = new UsuarioService();

    public List<Usuario> usuarios() {
        return usuarioService.listar();
    }

    public List<Auditoria> consultar(Object usuario, Object entidad, Object accion, String desde, String hasta) {
        return service.consultar(
                filtro(usuario),
                filtro(entidad),
                filtro(accion),
                Fechas.leerOpcional(desde, "fecha desde"),
                Fechas.leerOpcional(hasta, "fecha hasta"));
    }

    private String filtro(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.toString();
        return "TODOS".equals(texto) || "TODAS".equals(texto) ? null : texto;
    }
}
