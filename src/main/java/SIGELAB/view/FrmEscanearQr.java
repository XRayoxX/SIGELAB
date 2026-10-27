
package SIGELAB.view;

import SIGELAB.qr.LectorQrCamara;
import SIGELAB.util.TemaSIGELAB;

import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Ventana para escanear códigos QR utilizando una cámara web.
 *
 * El diseño principal está definido en FrmEscanearQr.form.
 *
 * Esta clase únicamente captura y reconoce el QR.
 * La validación del equipo se realiza posteriormente
 * mediante PrestamoService y EquipoService.
 */
public class FrmEscanearQr {

    // COMPONENTES VINCULADOS AL ARCHIVO .FORM
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JLabel lblEstado;
    private JLabel lblVista;
    private JButton btnCerrar;

    // VENTANA
    private final JDialog ventana;

    // RESULTADO
    private String codigoDetectado;

    // CONTROL DE CAPTURA
    private volatile boolean detenerCaptura;
    private SwingWorker<String, BufferedImage> trabajador;

    // COLORES DEL SISTEMA
    private static final Color OSCURO_FONDO =
            new Color(13, 20, 32);

    private static final Color OSCURO_TARJETA =
            new Color(24, 35, 51);

    private static final Color OSCURO_TEXTO =
            new Color(227, 234, 242);

    private static final Color CLARO_FONDO =
            new Color(244, 247, 250);

    private static final Color CLARO_TARJETA =
            Color.WHITE;

    private static final Color CLARO_TEXTO =
            new Color(35, 51, 67);

    private static final Color AZUL_PRINCIPAL =
            new Color(54, 125, 170);

    public FrmEscanearQr(Window propietario) {

        ventana = new JDialog(
                propietario,
                "SIGELAB - Escanear código QR",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        ventana.setContentPane(panelPrincipal);

        ventana.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );

        ventana.setMinimumSize(
                new Dimension(660, 500)
        );

        ventana.setSize(740, 570);

        ventana.setLocationRelativeTo(propietario);

        configurarComponentes();
        aplicarTema();

        btnCerrar.addActionListener(e -> cerrar());

        ventana.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                detenerCaptura = true;
            }

            @Override
            public void windowClosed(WindowEvent e) {
                detenerCaptura = true;
            }
        });
    }

    /**
     * Abre la ventana de escaneo.
     *
     * Devuelve el código detectado.
     * Si el usuario cancela, devuelve null.
     */
    public static String mostrarYLeer(Window propietario) {

        FrmEscanearQr formulario =
                new FrmEscanearQr(propietario);

        return formulario.mostrar();
    }

    private String mostrar() {

        iniciarCaptura();

        ventana.setVisible(true);

        return codigoDetectado;
    }

    private void configurarComponentes() {

        lblTitulo.setFont(
                new Font("Segoe UI", Font.BOLD, 21)
        );

        lblTitulo.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        lblEstado.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        lblEstado.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        lblVista.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        lblVista.setVerticalAlignment(
                SwingConstants.CENTER
        );

        lblVista.setOpaque(true);

        btnCerrar.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        btnCerrar.setFocusPainted(false);

        btnCerrar.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        panelPrincipal.setBorder(
                new EmptyBorder(20, 20, 20, 20)
        );
    }

    private void aplicarTema() {

        boolean oscuro = TemaSIGELAB.esOscuro();

        Color fondo = oscuro
                ? OSCURO_FONDO
                : CLARO_FONDO;

        Color tarjeta = oscuro
                ? OSCURO_TARJETA
                : CLARO_TARJETA;

        Color texto = oscuro
                ? OSCURO_TEXTO
                : CLARO_TEXTO;

        panelPrincipal.setBackground(fondo);

        lblTitulo.setForeground(texto);

        lblEstado.setForeground(texto);

        lblVista.setBackground(tarjeta);

        lblVista.setForeground(texto);

        btnCerrar.setBackground(AZUL_PRINCIPAL);

        btnCerrar.setForeground(Color.WHITE);

        btnCerrar.setOpaque(true);

        btnCerrar.setContentAreaFilled(true);
    }

    /**
     * Captura los fotogramas en segundo plano.
     *
     * SwingWorker evita bloquear la interfaz gráfica.
     */
    private void iniciarCaptura() {

        detenerCaptura = false;
        codigoDetectado = null;

        lblEstado.setText(
                "Buscando cámara disponible..."
        );

        trabajador =
                new SwingWorker<String, BufferedImage>() {

                    @Override
                    protected String doInBackground()
                            throws Exception {

                        Webcam camara = null;

                        try {

                            camara = Webcam.getDefault();

                            if (camara == null) {
                                throw new IllegalStateException(
                                        "No se encontró ninguna cámara."
                                );
                            }

                            camara.open();

                            SwingUtilities.invokeLater(() -> {

                                if (!detenerCaptura) {
                                    lblEstado.setText(
                                            "Cámara activa. Coloque el QR frente al lente."
                                    );
                                }
                            });

                            while (!detenerCaptura
                                    && !isCancelled()) {

                                BufferedImage imagen =
                                        camara.getImage();

                                if (imagen != null) {

                                    publish(imagen);

                                    String codigo =
                                            LectorQrCamara.leer(imagen);

                                    if (codigo != null
                                            && !codigo.isBlank()) {

                                        return codigo;
                                    }
                                }

                                Thread.sleep(100);
                            }

                            return null;

                        } finally {

                            if (camara != null
                                    && camara.isOpen()) {

                                camara.close();
                            }
                        }
                    }

                    @Override
                    protected void process(
                            List<BufferedImage> imagenes) {

                        if (detenerCaptura
                                || !ventana.isDisplayable()
                                || imagenes.isEmpty()) {
                            return;
                        }

                        BufferedImage imagen =
                                imagenes.get(imagenes.size() - 1);

                        int ancho = Math.max(
                                lblVista.getWidth(), 320
                        );

                        int alto = Math.max(
                                lblVista.getHeight(), 240
                        );

                        Image escalada = imagen.getScaledInstance(
                                ancho,
                                alto,
                                Image.SCALE_FAST
                        );

                        lblVista.setText("");

                        lblVista.setIcon(
                                new ImageIcon(escalada)
                        );
                    }

                    @Override
                    protected void done() {

                        if (detenerCaptura || isCancelled()) {
                            return;
                        }

                        try {

                            String resultado = get();

                            if (resultado != null
                                    && !resultado.isBlank()) {

                                codigoDetectado =
                                        resultado.trim();

                                lblEstado.setText(
                                        "QR identificado: "
                                                + codigoDetectado
                                );

                                detenerCaptura = true;

                                ventana.dispose();
                            }

                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();

                            mostrarError(
                                    "El escaneo fue interrumpido."
                            );

                        } catch (CancellationException e) {

                            // El usuario canceló el escaneo.

                        } catch (ExecutionException e) {

                            Throwable causa = e.getCause();

                            String mensaje = causa == null
                                    ? e.getMessage()
                                    : causa.getMessage();

                            mostrarError(
                                    "No se pudo utilizar la cámara.\n"
                                            + mensaje
                            );
                        }
                    }
                };

        trabajador.execute();
    }

    private void mostrarError(String mensaje) {

        if (detenerCaptura) {
            return;
        }

        detenerCaptura = true;

        JOptionPane.showMessageDialog(
                ventana,
                mensaje,
                "SIGELAB - Escáner QR",
                JOptionPane.ERROR_MESSAGE
        );

        ventana.dispose();
    }

    private void cerrar() {

        detenerCaptura = true;

        // El trabajador saldrá del bucle y liberará
        // la cámara desde su bloque finally.

        ventana.dispose();
    }
}
