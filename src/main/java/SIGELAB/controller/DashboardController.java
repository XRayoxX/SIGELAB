package SIGELAB.controller;

import SIGELAB.model.ResumenDashboard;
import SIGELAB.service.DashboardService;

/**
 * Puente entre el menú principal y DashboardService.
 */
public class DashboardController {

    private final DashboardService service = new DashboardService();

    public ResumenDashboard resumen() {
        return service.resumen();
    }
}
