
package SIGELAB.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import javax.swing.*;
import javax.swing.table.TableModel;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ExportarPdfUtil {

    private static final PDType1Font NORMAL =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

    private static final PDType1Font NEGRITA =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

    private ExportarPdfUtil() {
    }

    public static File exportar(
            Component padre,
            JTable tabla,
            String titulo) {

        if (tabla.getRowCount() == 0) {
            throw new BusinessException(
                    "No existen resultados para exportar."
            );
        }

        JFileChooser selector = new JFileChooser();

        selector.setDialogTitle(
                "Guardar reporte en PDF"
        );

        selector.setSelectedFile(
                new File("Reporte_SIGELAB.pdf")
        );

        if (selector.showSaveDialog(padre)
                != JFileChooser.APPROVE_OPTION) {

            return null;
        }

        File archivo = selector.getSelectedFile();

        if (!archivo.getName().toLowerCase()
                .endsWith(".pdf")) {

            archivo = new File(
                    archivo.getAbsolutePath() + ".pdf"
            );
        }

        if (archivo.exists()) {

            int confirmar = JOptionPane.showConfirmDialog(
                    padre,
                    "El archivo ya existe. ¿Desea reemplazarlo?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirmar != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        crearDocumento(archivo, tabla, titulo);

        return archivo;
    }

    private static void crearDocumento(
            File archivo,
            JTable tabla,
            String titulo) {

        try (PDDocument documento = new PDDocument()) {

            PDPageContentStream contenido =
                    nuevaPagina(documento);

            float y = 550;

            escribir(
                    contenido,
                    "SIGELAB - SISTEMA DE GESTION DE LABORATORIOS",
                    40, y, 15, true
            );

            y -= 27;

            escribir(
                    contenido,
                    titulo,
                    40, y, 12, true
            );

            y -= 20;

            String fecha = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm"
                    )
            );

            escribir(
                    contenido,
                    "Generado: " + fecha,
                    40, y, 9, false
            );

            y -= 18;

            escribir(
                    contenido,
                    "Total de registros: " + tabla.getRowCount(),
                    40, y, 9, false
            );

            y -= 28;

            TableModel modelo = tabla.getModel();

            for (int fila = 0;
                 fila < modelo.getRowCount();
                 fila++) {

                // Una ficha por registro, para evitar
                // recortar columnas en documentos anchos.

                for (int columna = 0;
                     columna < modelo.getColumnCount();
                     columna++) {

                    if (y < 55) {

                        contenido.close();

                        contenido = nuevaPagina(
                                documento
                        );

                        y = 550;
                    }

                    String nombreColumna =
                            modelo.getColumnName(columna);

                    Object valor =
                            modelo.getValueAt(fila, columna);

                    String texto = nombreColumna + ": "
                            + (valor == null ? "" : valor);

                    escribir(
                            contenido,
                            texto,
                            43, y, 9, false
                    );

                    y -= 14;
                }

                if (y < 60) {

                    contenido.close();

                    contenido = nuevaPagina(documento);

                    y = 550;

                } else {

                    escribir(
                            contenido,
                            "---------------------------------------------",
                            40, y, 9, false
                    );

                    y -= 22;
                }
            }

            contenido.close();

            documento.save(archivo);

        } catch (IOException e) {

            throw new BusinessException(
                    "No se pudo generar el archivo PDF: "
                            + e.getMessage()
            );
        }
    }

    private static PDPageContentStream nuevaPagina(
            PDDocument documento) throws IOException {

        PDPage pagina = new PDPage(
                new PDRectangle(
                        PDRectangle.A4.getHeight(),
                        PDRectangle.A4.getWidth()
                )
        );

        documento.addPage(pagina);

        return new PDPageContentStream(
                documento,
                pagina
        );
    }

    private static void escribir(
            PDPageContentStream contenido,
            String texto,
            float x,
            float y,
            int tamano,
            boolean negrita) throws IOException {

        contenido.beginText();

        contenido.setFont(
                negrita ? NEGRITA : NORMAL,
                tamano
        );

        contenido.newLineAtOffset(x, y);

        contenido.showText(
                normalizar(texto)
        );

        contenido.endText();
    }

    /**
     * Utiliza caracteres compatibles con
     * las fuentes estándar de PDF.
     */
    private static String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String normalizado = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        normalizado = normalizado.replaceAll(
                "\\p{M}+",
                ""
        );

        normalizado = normalizado.replaceAll(
                "[^\\x20-\\x7E]",
                " "
        );

        // Limitación de una línea de la ficha.
        if (normalizado.length() > 125) {
            normalizado =
                    normalizado.substring(0, 122) + "...";
        }

        return normalizado;
    }
}
