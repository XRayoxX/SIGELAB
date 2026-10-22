package SIGELAB.controller;

import SIGELAB.enums.EstadoEquipo;
import SIGELAB.model.Equipo;
import SIGELAB.model.Laboratorio;
import SIGELAB.model.ResultadoReporte;
import SIGELAB.model.TipoEquipo;
import SIGELAB.service.EquipoService;
import SIGELAB.service.LaboratorioService;
import SIGELAB.service.TipoEquipoService;
import SIGELAB.util.QrUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Coordina FrmEquipos: catálogos para los combos, CRUD, QR e historial.
 */
public class EquipoController {

    private final EquipoService service = new EquipoService();
    private final TipoEquipoService tipoService = new TipoEquipoService();
    private final LaboratorioService laboratorioService = new LaboratorioService();

    public List<Equipo> listar(String texto, EstadoEquipo estado, Integer idLaboratorio) {
        return service.listar(texto, estado, idLaboratorio);
    }

    public List<TipoEquipo> tiposActivos() {
        return tipoService.listarActivos();
    }

    public List<Laboratorio> laboratoriosActivos() {
        return laboratorioService.listarActivos();
    }

    public Equipo guardar(Equipo e) {
        return service.registrar(e);
    }

    public void actualizar(Equipo e) {
        service.actualizar(e);
    }

    public ResultadoReporte historial(String codigoInventario) {
        return service.historial(codigoInventario);
    }

    // El QR contiene únicamente el código de inventario del equipo
    public BufferedImage generarQr(String codigoInventario, int tamano) {
        Equipo equipo = service.identificarPorCodigo(codigoInventario);
        return QrUtil.generar(equipo.getCodigoInventario(), tamano);
    }

    public void guardarQr(BufferedImage imagen, File archivo) {
        QrUtil.guardarPng(imagen, archivo);
    }
}
