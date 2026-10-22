package SIGELAB.util;

import javax.swing.*;
import java.awt.*;

/**
 * Mensajes al usuario. Convierte las excepciones en avisos entendibles:
 * BusinessException -> aviso de validación; DataAccessException -> error de base de datos
 * (el detalle técnico queda en la consola, nunca en pantalla).
 */
public final class Dialogos {

    private Dialogos() {
    }

    public static void ejecutar(Component ventana, Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(ventana, e.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(ventana, e.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void info(Component ventana, String mensaje) {
        JOptionPane.showMessageDialog(ventana, mensaje, "SIGELAB", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirmar(Component ventana, String pregunta) {
        return JOptionPane.showConfirmDialog(ventana, pregunta, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    // Texto de un campo sin espacios al inicio/final; null si está vacío
    public static String texto(JTextField campo) {
        String valor = campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }
}
