
package SIGELAB.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.awt.image.BufferedImage;

/**
 * SIGELAB - Lector de códigos QR mediante cámara web.
 *
 * Identifica códigos QR en imágenes capturadas.
 *
 * El escaneo no modifica el estado del equipo.
 * La validación se realiza posteriormente
 * mediante EquipoService y PrestamoService.
 */
public final class LectorQrCamara {

    private LectorQrCamara() {
    }

    /**
     * Busca un código QR dentro de una imagen.
     *
     * @param imagen Imagen capturada por la cámara.
     * @return Código identificado o null si no se detecta.
     */
    public static String leer(BufferedImage imagen) {

        if (imagen == null) {
            return null;
        }

        try {

            BufferedImageLuminanceSource fuente =
                    new BufferedImageLuminanceSource(imagen);

            HybridBinarizer binarizador =
                    new HybridBinarizer(fuente);

            BinaryBitmap bitmap =
                    new BinaryBitmap(binarizador);

            QRCodeReader lector =
                    new QRCodeReader();

            String codigo = lector.decode(bitmap)
                    .getText()
                    .trim();

            if (codigo.isBlank()) {
                return null;
            }

            return codigo;

        } catch (NotFoundException |
                 ChecksumException |
                 FormatException e) {

            /*
             * NotFoundException:
             * No se encontró un QR en la imagen.
             *
             * ChecksumException:
             * El QR contiene errores de lectura.
             *
             * FormatException:
             * El formato del QR no pudo interpretarse.
             *
             * Estos errores son normales durante
             * el escaneo continuo de la cámara.
             *
             * Se devuelve null para permitir
             * analizar el siguiente fotograma.
             */

            return null;
        }
    }
}
