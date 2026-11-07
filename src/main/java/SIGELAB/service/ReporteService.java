
package SIGELAB.service;

import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.ResultadoReporte;
import SIGELAB.repository.ReporteFiltroRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Fechas;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class ReporteService {

    private final PrestamoService prestamoService =
            new PrestamoService();

    private final ReporteFiltroRepository repository =
            new ReporteFiltroRepository();

    private static final Set<String> ESTADOS_VALIDOS =
            Set.of(
                    "DISPONIBLE",
                    "PRESTADO",
                    "DANADO",
                    "MANTENIMIENTO",
                    "BAJA"
            );

    public ResultadoReporte generar(
            String tipo,
            Integer laboratorio,
            String codigo,
            String estado,
            String fechaDesde,
            String fechaHasta) {

        if (tipo == null || tipo.isBlank()) {
            throw new BusinessException(
                    "Seleccione un tipo de reporte."
            );
        }

        if (laboratorio != null && laboratorio <= 0) {
            throw new BusinessException(
                    "El laboratorio seleccionado no es válido."
            );
        }

        String codigoNormalizado =
                normalizar(codigo);

        String estadoNormalizado =
                normalizar(estado);

        if ("TODOS".equals(estadoNormalizado)
                || estadoNormalizado.isBlank()) {

            estadoNormalizado = null;
        }

        if (estadoNormalizado != null
                && !ESTADOS_VALIDOS.contains(estadoNormalizado)) {

            throw new BusinessException(
                    "El estado de equipo seleccionado no es válido."
            );
        }

        if (codigoNormalizado.isBlank()) {
            codigoNormalizado = null;
        }

        if (codigoNormalizado != null
                && codigoNormalizado.length() > 20) {

            throw new BusinessException(
                    "El código del equipo no puede superar "
                            + "20 caracteres."
            );
        }

        LocalDate desde = Fechas.leerOpcional(
                fechaDesde,
                "fecha desde"
        );

        LocalDate hasta = Fechas.leerOpcional(
                fechaHasta,
                "fecha hasta"
        );

        if (desde != null && hasta != null
                && desde.isAfter(hasta)) {

            throw new BusinessException(
                    "La fecha inicial no puede ser "
                            + "posterior a la fecha final."
            );
        }

        return repository.consultar(
                tipo,
                laboratorio,
                codigoNormalizado,
                estadoNormalizado,
                desde,
                hasta
        );
    }

    /**
     * Equipos incluidos en un préstamo (ventana emergente de Reportes):
     * situación en el préstamo, condición de devolución y estado actual del equipo.
     */
    public ResultadoReporte detallePrestamo(int idPrestamo) {

        if (idPrestamo <= 0) {
            throw new BusinessException(
                    "Seleccione un préstamo válido."
            );
        }

        List<DetallePrestamo> detalles =
                prestamoService.listarDetalles(idPrestamo);

        if (detalles.isEmpty()) {
            throw new BusinessException(
                    "El préstamo No. " + idPrestamo
                            + " no tiene equipos registrados."
            );
        }

        ResultadoReporte resultado = new ResultadoReporte(List.of(
                "Código", "Equipo", "Situación", "Condición",
                "Fecha devolución", "Observación", "Estado actual"
        ));

        for (DetallePrestamo d : detalles) {
            resultado.agregarFila(new Object[]{
                    d.getEquipo().getCodigoInventario(),
                    d.getEquipo().getNombre(),
                    d.getEstado(),
                    d.getCondicion() == null ? "" : d.getCondicion(),
                    Fechas.formatear(d.getFechaDevolucion()),
                    d.getObservacionDevolucion() == null ? "" : d.getObservacionDevolucion(),
                    d.getEquipo().getEstado()
            });
        }

        return resultado;
    }

    private String normalizar(String valor) {

        return valor == null
                ? ""
                : valor.trim().toUpperCase();
    }
}
