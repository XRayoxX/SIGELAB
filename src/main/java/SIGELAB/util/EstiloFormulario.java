
package SIGELAB.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;

import java.awt.*;

public final class EstiloFormulario {

    private EstiloFormulario() {
        // Clase de utilidades.
    }

    // ========================================
    // APLICAR TEMA
    // ========================================

    public static void aplicar(JPanel panelPrincipal) {

        aplicarComponente(panelPrincipal);

        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    // ========================================
    // RECORRER COMPONENTES DEL FORM
    // ========================================

    private static void aplicarComponente(Component componente) {

        if (componente instanceof JPanel panel) {

            panel.setBackground(
                    TemaSIGELAB.panel()
            );

            actualizarBorde(panel);
        }

        if (componente instanceof JLabel etiqueta) {

            etiqueta.setForeground(
                    TemaSIGELAB.texto()
            );
        }

        if (componente instanceof JTextField campo) {

            campo.setBackground(
                    TemaSIGELAB.campo()
            );

            campo.setForeground(
                    TemaSIGELAB.texto()
            );

            campo.setCaretColor(
                    TemaSIGELAB.texto()
            );
        }

        if (componente instanceof JTextArea area) {

            area.setBackground(
                    TemaSIGELAB.campo()
            );

            area.setForeground(
                    TemaSIGELAB.texto()
            );

            area.setCaretColor(
                    TemaSIGELAB.texto()
            );
        }

        if (componente instanceof JComboBox<?> combo) {

            combo.setBackground(
                    TemaSIGELAB.campo()
            );

            combo.setForeground(
                    TemaSIGELAB.texto()
            );
        }

        if (componente instanceof JSpinner spinner) {

            spinner.setBackground(
                    TemaSIGELAB.campo()
            );

            spinner.setForeground(
                    TemaSIGELAB.texto()
            );
        }

        if (componente instanceof JCheckBox check) {

            check.setForeground(
                    TemaSIGELAB.texto()
            );

            check.setOpaque(false);
        }

        if (componente instanceof JButton boton) {

            aplicarEstiloBoton(boton);
        }

        if (componente instanceof JTable tabla) {

            aplicarEstiloTabla(tabla);
        }

        if (componente instanceof JScrollPane scroll) {

            scroll.getViewport().setBackground(
                    TemaSIGELAB.panel()
            );
        }

        // Recorrer componentes internos.

        if (componente instanceof Container contenedor) {

            for (Component hijo : contenedor.getComponents()) {

                aplicarComponente(hijo);
            }
        }
    }

    // ========================================
    // ESTILO DE BOTONES
    // ========================================

    private static void aplicarEstiloBoton(JButton boton) {

        String texto = boton.getText();

        if (texto == null) {
            texto = "";
        }

        String accion = texto.toLowerCase();

        Color fondo = TemaSIGELAB.primario();

        if (accion.contains("desactivar")
                || accion.contains("eliminar")) {

            fondo = TemaSIGELAB.error();

        } else if (accion.contains("regresar")
                || accion.contains("limpiar")
                || accion.contains("salir")) {

            fondo = TemaSIGELAB.navegacion();
        }

        boton.setBackground(fondo);

        boton.setForeground(Color.WHITE);

        boton.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        boton.setFocusPainted(false);

        boton.setOpaque(true);

        boton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    // ========================================
    // ESTILO DE TABLAS
    // ========================================

    private static void aplicarEstiloTabla(JTable tabla) {

        tabla.setBackground(
                TemaSIGELAB.campo()
        );

        tabla.setForeground(
                TemaSIGELAB.texto()
        );

        tabla.setGridColor(
                TemaSIGELAB.borde()
        );

        tabla.setSelectionBackground(
                TemaSIGELAB.primario()
        );

        tabla.setSelectionForeground(
                Color.WHITE
        );

        tabla.setRowHeight(30);

        tabla.setShowGrid(true);

        tabla.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        JTableHeader encabezado =
                tabla.getTableHeader();

        if (encabezado != null) {

            encabezado.setBackground(
                    TemaSIGELAB.navegacion()
            );

            encabezado.setForeground(
                    Color.WHITE
            );

            encabezado.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            12
                    )
            );
        }
    }

    // ========================================
    // ACTUALIZAR BORDES CON TITULO
    // ========================================

    private static void actualizarBorde(JPanel panel) {

        Border bordeActual = panel.getBorder();

        if (bordeActual instanceof TitledBorder tituloActual) {

            TitledBorder nuevoBorde =
                    BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(
                                    TemaSIGELAB.borde()
                            ),
                            tituloActual.getTitle()
                    );

            nuevoBorde.setTitleColor(
                    TemaSIGELAB.texto()
            );

            nuevoBorde.setTitleFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            12
                    )
            );

            panel.setBorder(nuevoBorde);
        }
    }
}
