package SIGELAB.controller;

import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.Equipo;
import SIGELAB.model.Prestamo;
import SIGELAB.model.Solicitante;
import SIGELAB.service.EquipoService;
import SIGELAB.service.PrestamoService;
import SIGELAB.service.SolicitanteService;
import SIGELAB.util.QrUtil;

import java.io.File;
import java.util.List;

/**
 * Coordina FrmPrestamos: solicitantes, identificación de equipos (código o QR) y confirmación.
 */
public class PrestamoController {

    private final PrestamoService service = new PrestamoService();
    private final EquipoService equipoService = new EquipoService();
    private final SolicitanteService solicitanteService = new SolicitanteService();

    public List<Solicitante> solicitantesActivos() {
        return solicitanteService.listarActivos();
    }

    // Lee el QR de una imagen y devuelve el código de inventario que contiene
    public String leerCodigoQr(File imagen) {
        return QrUtil.leer(imagen);
    }

    // Solo identifica (muestra datos y estado); no modifica nada (RN-19)
    public Equipo identificarEquipo(String codigo) {
        return equipoService.identificarPorCodigo(codigo);
    }

    public Equipo validarEquipoParaPrestamo(String codigo, List<Equipo> agregados) {
        return service.validarEquipoParaPrestamo(codigo, agregados);
    }

    public int confirmar(Prestamo prestamo, List<Equipo> equipos) {
        return service.confirmar(prestamo, equipos);
    }

    public List<Prestamo> listar(String texto, String filtro) {
        return service.listar(texto, filtro);
    }

    public List<DetallePrestamo> listarDetalles(int idPrestamo) {
        return service.listarDetalles(idPrestamo);
    }
}
