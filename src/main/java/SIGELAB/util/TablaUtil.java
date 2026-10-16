package SIGELAB.util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class TablaUtil {

    private TablaUtil() {
    }

    // Asigna encabezados claros a un JTable y evita que el usuario edite las celdas directamente
    public static DefaultTableModel configurar(JTable tabla, String... columnas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla.setModel(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        return modelo;
    }
}
