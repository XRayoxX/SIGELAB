package SIGELAB.util;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Logo e icono de SIGELAB (archivos en src/main/resources):
 *  - logo-sigelab.png : logo completo con su fondo (llena el lado izquierdo del Login).
 *  - icono-sigelab.png: solo el emblema (icono de las ventanas y de la barra de tareas).
 *  - sigelab.ico      : icono del ejecutable SIGELAB.exe (lo usa empaquetar.ps1).
 */
public final class LogoSIGELAB {

    private LogoSIGELAB() {
    }

    /**
     * Panel que dibuja el logo LLENANDO todo su espacio (como un fondo de pantalla):
     * se agranda hasta cubrir el panel y se centra; lo que sobra del fondo azul oscuro se recorta.
     * Null si no se encontró la imagen.
     */
    public static JComponent panelLogo() {
        BufferedImage imagen = leer("/logo-sigelab.png");
        if (imagen == null) {
            return null;
        }
        return new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                // "cubrir": la escala más grande para que no queden bordes vacíos
                double escala = Math.max((double) getWidth() / imagen.getWidth(), (double) getHeight() / imagen.getHeight());
                int ancho = (int) Math.ceil(imagen.getWidth() * escala);
                int alto = (int) Math.ceil(imagen.getHeight() * escala);
                g2.drawImage(imagen, (getWidth() - ancho) / 2, (getHeight() - alto) / 2, ancho, alto, null);
                g2.dispose();
            }
        };
    }

    /**
     * Pone el icono de SIGELAB a TODAS las ventanas que se abran (Login, menú, formularios, diálogos),
     * en lugar de la taza de café de Java. Se llama una sola vez desde Main.
     */
    public static void usarEnTodasLasVentanas() {
        BufferedImage icono = leer("/icono-sigelab.png");
        if (icono == null) {
            return;
        }
        // Windows elige el tamaño que necesita (barra de título, barra de tareas, Alt+Tab)
        List<Image> tamanos = new ArrayList<>();
        for (int lado : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            tamanos.add(icono.getScaledInstance(lado, lado, Image.SCALE_SMOOTH));
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(evento -> {
            if (evento.getID() == WindowEvent.WINDOW_OPENED && evento.getSource() instanceof Window ventana) {
                ventana.setIconImages(tamanos);
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    private static BufferedImage leer(String recurso) {
        try (InputStream entrada = LogoSIGELAB.class.getResourceAsStream(recurso)) {
            return entrada == null ? null : ImageIO.read(entrada);
        } catch (IOException e) {
            return null; // sin logo la aplicación funciona igual
        }
    }
}
