package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.enums.EstadoDetalle;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoPrestamo;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.Prestamo;
import SIGELAB.repository.DevolucionRepository;
import SIGELAB.repository.EquipoRepository;
import SIGELAB.repository.PrestamoRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;
import SIGELAB.util.Transaccion;

import java.util.List;

/**
 * Reglas de devolución (sección 6.8): se devuelve equipo por equipo (devolución parcial).
 */
public class DevolucionService {

    private final PrestamoRepository prestamoRepository = new PrestamoRepository();
    private final DevolucionRepository devolucionRepository = new DevolucionRepository();
    private final EquipoRepository equipoRepository = new EquipoRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    public List<Prestamo> prestamosActivos(String texto) {
        String busqueda = texto == null || texto.isBlank() ? null : texto.trim();
        return prestamoRepository.listar(busqueda, EstadoPrestamo.ACTIVO.name());
    }

    public List<DetallePrestamo> equiposDelPrestamo(int idPrestamo) {
        return prestamoRepository.listarDetalles(idPrestamo);
    }

    /**
     * Registra la devolución de UN equipo como una transacción y devuelve cuántos quedan pendientes.
     * PRESTADO + BUENO -> DISPONIBLE (RN-12); PRESTADO + DANADO -> DANADO (RN-13).
     * Cuando ya no queda ninguno pendiente, el préstamo pasa a FINALIZADO (RN-16).
     */
    public int registrar(int idDetallePrestamo, CondicionDevolucion condicion, String observaciones) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (idDetallePrestamo <= 0) {
            throw new BusinessException("Seleccione en la tabla el equipo que se está devolviendo.");
        }
        if (condicion == null) {
            throw new BusinessException("Seleccione la condición en la que regresa el equipo (BUENO o DANADO).");
        }
        String obs = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        if (condicion == CondicionDevolucion.DANADO && obs == null) {
            throw new BusinessException("Describa en Observaciones el daño que presenta el equipo.");
        }
        if (obs != null && obs.length() > 500) {
            throw new BusinessException("Las observaciones no pueden tener más de 500 caracteres.");
        }

        int idUsuario = Sesion.getIdUsuario();
        return Transaccion.ejecutar(cn -> {
            DetallePrestamo detalle = prestamoRepository.buscarDetalle(cn, idDetallePrestamo)
                    .orElseThrow(() -> new BusinessException("El equipo seleccionado no pertenece a ningún préstamo."));
            String codigo = detalle.getEquipo().getCodigoInventario();

            // RN-11: no se puede devolver dos veces
            if (detalle.getEstado() != EstadoDetalle.PENDIENTE) {
                throw new BusinessException("El equipo " + codigo + " ya fue devuelto en este préstamo.");
            }
            // RN-10: solo se devuelve si está pendiente dentro de un préstamo ACTIVO
            if (detalle.getEstadoPrestamo() != EstadoPrestamo.ACTIVO) {
                throw new BusinessException("El préstamo No. " + detalle.getIdPrestamo() + " ya está FINALIZADO.");
            }
            // Solo cambia si sigue PENDIENTE en este instante (protege contra dos devoluciones simultáneas)
            if (!prestamoRepository.marcarDetalleDevuelto(cn, idDetallePrestamo)) {
                throw new BusinessException("El equipo " + codigo + " ya fue devuelto en este préstamo.");
            }

            devolucionRepository.insertar(cn, idDetallePrestamo, condicion, obs, idUsuario);

            EstadoEquipo nuevoEstado = condicion == CondicionDevolucion.BUENO ? EstadoEquipo.DISPONIBLE : EstadoEquipo.DANADO;
            if (!equipoRepository.cambiarEstado(cn, detalle.getEquipo().getIdEquipo(), EstadoEquipo.PRESTADO, nuevoEstado)) {
                throw new BusinessException("El equipo " + codigo + " no está en estado PRESTADO; no se puede registrar la devolución.");
            }
            auditoria.registrar(cn, AccionAuditoria.EQUIPO_DEVUELTO, "Prestamo", detalle.getIdPrestamo(),
                    "Equipo " + codigo + " devuelto " + condicion + " -> " + nuevoEstado + (obs == null ? "" : ". " + obs));

            // RN-16: el préstamo sigue ACTIVO mientras quede al menos un equipo pendiente
            int pendientes = prestamoRepository.contarPendientes(cn, detalle.getIdPrestamo());
            if (pendientes == 0) {
                prestamoRepository.finalizar(cn, detalle.getIdPrestamo());
                auditoria.registrar(cn, AccionAuditoria.PRESTAMO_FINALIZADO, "Prestamo", detalle.getIdPrestamo(),
                        "Todos los equipos fueron devueltos");
            }
            return pendientes;
        });
    }
}
