package SIGELAB.controller;

import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoMantenimiento;
import SIGELAB.enums.ResultadoMantenimiento;
import SIGELAB.model.Equipo;
import SIGELAB.model.Mantenimiento;
import SIGELAB.service.MantenimientoService;

import java.util.List;

/**
 * Puente entre FrmMantenimiento y MantenimientoService.
 */
public class MantenimientoController {

    private final MantenimientoService service = new MantenimientoService();

    public List<Equipo> equiposParaMantenimiento() {
        return service.equiposParaMantenimiento();
    }

    public List<Mantenimiento> listar(String texto, EstadoMantenimiento estado) {
        return service.listar(texto, estado);
    }

    public int enviar(Mantenimiento m) {
        return service.enviarAMantenimiento(m);
    }

    public EstadoEquipo finalizar(int idMantenimiento, ResultadoMantenimiento resultado, String observaciones) {
        return service.finalizar(idMantenimiento, resultado, observaciones);
    }
}
