package SIGELAB.controller;

import SIGELAB.model.ResumenDashboard;
import SIGELAB.service.DashboardService;
import SIGELAB.service.RecordatorioService;

/**
 * Puente entre el menú principal y DashboardService / RecordatorioService.
 */
public class DashboardController {

    private final DashboardService service = new DashboardService();
    private final RecordatorioService recordatorioService = new RecordatorioService();

    public ResumenDashboard resumen() {
        return service.resumen();
    }

    // Correos automáticos de préstamos que vencen hoy o ya vencieron
    public int enviarRecordatorios() {
        return recordatorioService.enviarPendientes();
    }
}
