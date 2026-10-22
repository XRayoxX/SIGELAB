package SIGELAB.util;

import SIGELAB.model.ResultadoReporte;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

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

    // Muestra un ResultadoReporte (columnas dinámicas) en una tabla
    public static void mostrar(JTable tabla, ResultadoReporte resultado) {
        DefaultTableModel modelo = configurar(tabla, resultado.getColumnas().toArray(new String[0]));
        for (Object[] fila : resultado.getFilas()) {
            modelo.addRow(fila);
        }
    }

    // Ventana emergente con una tabla de solo lectura (por ejemplo, el historial de un equipo)
    public static void mostrarEnDialogo(Component padre, String titulo, ResultadoReporte resultado) {
        JTable tabla = new JTable();
        mostrar(tabla, resultado);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(850, 300));
        String encabezado = resultado.getFilas().isEmpty() ? "Sin movimientos registrados." : resultado.getFilas().size() + " registro(s)";
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(encabezado), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(padre, panel, titulo, JOptionPane.PLAIN_MESSAGE);
    }
}
