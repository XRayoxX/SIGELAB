package SIGELAB.controller;

import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.Prestamo;
import SIGELAB.service.DevolucionService;

import java.util.List;

/**
 * Puente entre FrmDevoluciones y DevolucionService.
 */
public class DevolucionController {

    private final DevolucionService service = new DevolucionService();

    public List<Prestamo> prestamosActivos(String texto) {
        return service.prestamosActivos(texto);
    }

    public List<DetallePrestamo> equiposDelPrestamo(int idPrestamo) {
        return service.equiposDelPrestamo(idPrestamo);
    }

    public int registrar(int idDetallePrestamo, CondicionDevolucion condicion, String observaciones) {
        return service.registrar(idDetallePrestamo, condicion, observaciones);
    }
}
