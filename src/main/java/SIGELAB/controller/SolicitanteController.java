package SIGELAB.controller;

import SIGELAB.model.Solicitante;
import SIGELAB.service.SolicitanteService;

import java.util.List;

/**
 * Puente entre FrmSolicitantes (y el combo de FrmPrestamos) y SolicitanteService.
 */
public class SolicitanteController {

    private final SolicitanteService service = new SolicitanteService();

    public List<Solicitante> listar() {
        return service.listar();
    }

    public List<Solicitante> listarActivos() {
        return service.listarActivos();
    }

    public List<Solicitante> buscar(String texto) {
        return service.buscar(texto);
    }

    public Solicitante guardar(Solicitante s) {
        return service.registrar(s);
    }

    public void actualizar(Solicitante s) {
        service.actualizar(s);
    }

    public void desactivar(int idSolicitante) {
        service.desactivar(idSolicitante);
    }
}
