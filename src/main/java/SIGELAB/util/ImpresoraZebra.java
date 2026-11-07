package SIGELAB.util;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

/**
 * Imprime la etiqueta QR de un equipo en una impresora Zebra (por ejemplo la iMZ320).
 *
 * No se manda una imagen: se manda texto en ZPL, el lenguaje propio de las Zebra.
 * La impresora dibuja el QR por sí misma, así sale nítido y del tamaño exacto.
 *
 * Requisito: la impresora debe estar instalada en Windows (USB o Bluetooth) con el
 * driver "ZDesigner" de Zebra; Java le manda los comandos "en crudo" a través de ese driver.
 */
public final class ImpresoraZebra {

    // iMZ320: papel de 3 pulgadas, 203 puntos por pulgada -> 576 puntos de ancho imprimible
    private static final int ANCHO = 576;
    private static final int LARGO = 380;   // unos 4.7 cm de etiqueta (203 puntos = 1 pulgada)

    // Preferences guarda datos pequeños en el registro de Windows del usuario (no en la base de datos)
    private static final Preferences PREFERENCIAS = Preferences.userNodeForPackage(ImpresoraZebra.class);
    private static final String CLAVE_ULTIMA = "impresoraQr";

    private ImpresoraZebra() {
    }

    /** Impresoras instaladas en Windows. */
    public static PrintService[] listar() {
        return PrintServiceLookup.lookupPrintServices(null, null);
    }

    /**
     * La impresora que se ofrece primero:
     *  1) la última que el usuario usó para imprimir QR (se recuerda en esta PC),
     *  2) si no, la primera que parezca Zebra,
     *  3) si no, la predeterminada de Windows.
     * Ojo: Windows puede tener varias "iMZ320" instaladas y solo una conectada a la impresora real.
     */
    public static PrintService sugerida() {
        String ultima = PREFERENCIAS.get(CLAVE_ULTIMA, null);
        for (PrintService p : listar()) {
            if (p.getName().equals(ultima)) {
                return p;
            }
        }
        for (PrintService p : listar()) {
            String nombre = p.getName().toLowerCase();
            if (nombre.contains("zebra") || nombre.contains("zdesigner") || nombre.contains("imz")) {
                return p;
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    public static void imprimirEtiquetaQr(PrintService impresora, String codigo, String nombreEquipo, int copias) {
        if (impresora == null) {
            throw new BusinessException("No hay ninguna impresora instalada en Windows.");
        }
        byte[] zpl = etiquetaZpl(codigo, nombreEquipo, copias).getBytes(StandardCharsets.UTF_8);
        try {
            DocPrintJob trabajo = impresora.createPrintJob();
            Doc documento = new SimpleDoc(zpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            trabajo.print(documento, null);
            PREFERENCIAS.put(CLAVE_ULTIMA, impresora.getName()); // se recuerda para la próxima vez
        } catch (PrintException e) {
            throw new BusinessException("No se pudo enviar la etiqueta a " + impresora.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Arma la etiqueta en ZPL:
     *   SIGELAB
     *   [ QR con el código de inventario ]
     *   CAM-001
     *   Nombre del equipo
     */
    static String etiquetaZpl(String codigo, String nombreEquipo, int copias) {
        int tamanoQr = 200; // un QR versión 2 (25 módulos) x 8 puntos por módulo
        int xQr = (ANCHO - tamanoQr) / 2;
        // Comandos SGD de Zebra (empiezan con "! U1"): le dicen a la impresora móvil que el rollo es
        // CONTINUO ("journal"). Si queda en "label", antes de imprimir avanza la cinta buscando la
        // separación entre etiquetas. El ajuste queda guardado en la impresora; repetirlo no gasta papel.
        return "! U1 setvar \"media.type\" \"journal\"\r\n"
                + "! U1 setvar \"ezpl.media_type\" \"continuous\"\r\n"
                + "^XA"
                + "^CI28"                                   // textos en UTF-8 (tildes y ñ)
                // ^MNN = papel CONTINUO (rollo tipo recibo): avanza solo el largo ^LL y se detiene.
                // Sin esto, la impresora busca la separación entre etiquetas y gasta mucha cinta.
                + "^MNN"
                + "^PW" + ANCHO + "^LL" + LARGO + "^LH0,0"
                + "^FO0,10^FB" + ANCHO + ",1,0,C^A0N,30,30^FDSIGELAB^FS"
                + "^FO" + xQr + ",40^BQN,2,8^FDMA," + limpiar(codigo) + "^FS"
                + "^FO0,262^FB" + ANCHO + ",1,0,C^A0N,40,40^FD" + limpiar(codigo) + "^FS"
                + "^FO10,308^FB" + (ANCHO - 20) + ",2,0,C^A0N,28,28^FD" + limpiar(nombreEquipo) + "^FS"
                + "^PQ" + Math.max(1, copias)            // cantidad de copias
                + "^XZ";
    }

    // ^ y ~ son comandos en ZPL: si aparecen en un texto romperían la etiqueta
    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replace('^', ' ').replace('~', ' ').trim();
    }
}
