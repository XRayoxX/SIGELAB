package SIGELAB.controller;

import SIGELAB.model.Laboratorio;
import SIGELAB.service.LaboratorioService;

import java.util.List;

/**
 * Puente entre FrmLaboratorios y LaboratorioService.
 * El formulario nunca habla directamente con el Service ni con el Repository.
 */
public class LaboratorioController {

    private final LaboratorioService service = new LaboratorioService();

    public List<Laboratorio> listar() {
        return service.listar();
    }

    public List<Laboratorio> listarActivos() {
        return service.listarActivos();
    }

    public List<Laboratorio> buscar(String texto) {
        return service.buscar(texto);
    }

    public Laboratorio guardar(Laboratorio lab) {
        return service.registrar(lab);
    }

    public void actualizar(Laboratorio lab) {
        service.actualizar(lab);
    }

    public void desactivar(int idLaboratorio) {
        service.desactivar(idLaboratorio);
    }
}
