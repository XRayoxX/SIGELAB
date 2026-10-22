package SIGELAB.controller;

import SIGELAB.model.TipoEquipo;
import SIGELAB.service.TipoEquipoService;

import java.util.List;

/**
 * Puente entre FrmTiposEquipo y TipoEquipoService.
 */
public class TipoEquipoController {

    private final TipoEquipoService service = new TipoEquipoService();

    public List<TipoEquipo> listar() {
        return service.listar();
    }

    public List<TipoEquipo> listarActivos() {
        return service.listarActivos();
    }

    public List<TipoEquipo> buscar(String texto) {
        return service.buscar(texto);
    }

    public TipoEquipo guardar(TipoEquipo tipo) {
        return service.registrar(tipo);
    }

    public void actualizar(TipoEquipo tipo) {
        service.actualizar(tipo);
    }

    public void desactivar(int idTipoEquipo) {
        service.desactivar(idTipoEquipo);
    }
}
