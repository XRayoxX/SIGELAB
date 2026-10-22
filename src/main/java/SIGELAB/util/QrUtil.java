package SIGELAB.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Genera y lee códigos QR con la librería ZXing.
 * El QR solo contiene el código de inventario (por ejemplo "CAM-001"): sirve para IDENTIFICAR el equipo,
 * nunca cambia su estado por sí solo (RN-19).
 */
public final class QrUtil {

    private QrUtil() {
    }

    public static BufferedImage generar(String texto, int tamanoPixeles) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamanoPixeles, tamanoPixeles,
                    Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(), EncodeHintType.MARGIN, 1));
            return MatrixToImageWriter.toBufferedImage(matriz);
        } catch (WriterException e) {
            throw new BusinessException("No se pudo generar el código QR para " + texto + ".");
        }
    }

    public static void guardarPng(BufferedImage imagen, File archivo) {
        try {
            ImageIO.write(imagen, "png", archivo);
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar la imagen del QR en " + archivo.getAbsolutePath() + ".");
        }
    }

    // Lee una imagen (foto o captura del QR) y devuelve el texto que contiene
    public static String leer(File archivoImagen) {
        try {
            BufferedImage imagen = ImageIO.read(archivoImagen);
            if (imagen == null) {
                throw new BusinessException("El archivo seleccionado no es una imagen válida.");
            }
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
            return new MultiFormatReader().decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE)).getText().trim();
        } catch (NotFoundException e) {
            throw new BusinessException("No se encontró ningún código QR en la imagen seleccionada.");
        } catch (IOException e) {
            throw new BusinessException("No se pudo abrir la imagen seleccionada.");
        }
    }
}
