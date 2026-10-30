
package SIGELAB.controller;

import SIGELAB.model.Laboratorio;
import SIGELAB.model.ResultadoReporte;
import SIGELAB.service.LaboratorioService;
import SIGELAB.service.ReporteService;

import java.util.List;

public class ReporteController {

    private final ReporteService service =
            new ReporteService();

    private final LaboratorioService laboratorioService =
            new LaboratorioService();

    public List<Laboratorio> laboratoriosActivos() {

        return laboratorioService.listarActivos();
    }

    // Equipos que se llevaron en un préstamo (doble clic en el reporte)
    public ResultadoReporte detallePrestamo(int idPrestamo) {

        return service.detallePrestamo(idPrestamo);
    }

    public ResultadoReporte generar(
            String tipoReporte,
            Integer idLaboratorio,
            String codigoEquipo,
            String estadoEquipo,
            String fechaDesde,
            String fechaHasta) {

        return service.generar(
                tipoReporte,
                idLaboratorio,
                codigoEquipo,
                estadoEquipo,
                fechaDesde,
                fechaHasta
        );
    }
}
