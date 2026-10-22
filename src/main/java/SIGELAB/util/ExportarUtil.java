package SIGELAB.util;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Exporta el contenido de un JTable a CSV (se abre directamente con Excel).
 */
public final class ExportarUtil {

    private ExportarUtil() {
    }

    // Pregunta dónde guardar y escribe el archivo. Devuelve el archivo o null si el usuario canceló.
    public static File exportarCsv(Component padre, JTable tabla, String nombreSugerido) {
        if (tabla.getRowCount() == 0) {
            throw new BusinessException("No hay datos para exportar. Realice primero una búsqueda.");
        }
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File(nombreSugerido + ".csv"));
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getAbsolutePath() + ".csv");
        }
        TableModel modelo = tabla.getModel();
        try (PrintWriter salida = new PrintWriter(archivo, StandardCharsets.UTF_8)) {
            salida.print('﻿');   // BOM: Excel reconoce las tildes en UTF-8
            for (int c = 0; c < modelo.getColumnCount(); c++) {
                salida.print((c == 0 ? "" : ";") + celda(modelo.getColumnName(c)));
            }
            salida.println();
            for (int f = 0; f < modelo.getRowCount(); f++) {
                for (int c = 0; c < modelo.getColumnCount(); c++) {
                    salida.print((c == 0 ? "" : ";") + celda(modelo.getValueAt(f, c)));
                }
                salida.println();
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el archivo " + archivo.getAbsolutePath() + ".");
        }
        return archivo;
    }

    private static String celda(Object valor) {
        String texto = valor == null ? "" : valor.toString();
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
