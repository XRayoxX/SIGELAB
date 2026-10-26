package sigelab.facial;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Ventana de captura facial para SIGELAB (Java Swing).
 *
 * <p>Abre la webcam con la librería webcam-capture (com.github.sarxos:webcam-capture:0.3.12),
 * guía al usuario con instrucciones, toma las fotos y las envía a la API en Azure.</p>
 *
 * <pre>
 * FacialApiClient api = new FacialApiClient(URL, API_KEY, "sigelab-desktop");
 * FacialApiClient.Respuesta r = CapturaFacialDialog.verificar(ventana, api, idUsuario);
 * if (r != null &amp;&amp; r.esExitosa() &amp;&amp; r.getBoolean("matched")) { // acceso concedido }
 * </pre>
 */
public class CapturaFacialDialog extends JDialog {

    private enum Modo { REGISTRAR, VERIFICAR, IDENTIFICAR }

    /** Un paso de la captura: instrucción en pantalla y cuántas fotos tomar. */
    private static final class Paso {
        final String texto;
        final int fotos;

        Paso(String texto, int fotos) {
            this.texto = texto;
            this.fotos = fotos;
        }
    }

    private static final String FRENTE = "Mire de frente a la cámara";
    private static final String IZQUIERDA = "Gire ligeramente la cabeza hacia SU IZQUIERDA";
    private static final String DERECHA = "Gire ligeramente la cabeza hacia SU DERECHA";

    private final FacialApiClient api;
    private final Modo modo;
    private final int idUsuario;
    private final boolean reemplazar;

    private final JLabel instruccion = new JLabel("Iniciando cámara...", SwingConstants.CENTER);
    private final JLabel estado = new JLabel(" ", SwingConstants.CENTER);
    private final JButton cancelar = new JButton("Cancelar");
    private Webcam webcam;
    private WebcamPanel panel;
    private volatile boolean cancelado;
    private FacialApiClient.Respuesta resultado;

    // ------------------------------------------------------- uso sencillo

    /** Registra (o reemplaza) el rostro del usuario. Devuelve null si se canceló. */
    public static FacialApiClient.Respuesta registrar(Window padre, FacialApiClient api, int idUsuario, boolean reemplazar) {
        return mostrar(new CapturaFacialDialog(padre, api, Modo.REGISTRAR, idUsuario, reemplazar));
    }

    /** Verifica que la persona sea el usuario indicado (por ejemplo, después del login). */
    public static FacialApiClient.Respuesta verificar(Window padre, FacialApiClient api, int idUsuario) {
        return mostrar(new CapturaFacialDialog(padre, api, Modo.VERIFICAR, idUsuario, false));
    }

    /** Reconoce quién es la persona sin pedir usuario (por ejemplo, para marcar ingreso al laboratorio). */
    public static FacialApiClient.Respuesta identificar(Window padre, FacialApiClient api) {
        return mostrar(new CapturaFacialDialog(padre, api, Modo.IDENTIFICAR, 0, false));
    }

    private static FacialApiClient.Respuesta mostrar(CapturaFacialDialog dialog) {
        dialog.setVisible(true); // modal: bloquea hasta que se cierre
        return dialog.resultado;
    }

    // ------------------------------------------------------------- ventana

    private CapturaFacialDialog(Window padre, FacialApiClient api, Modo modo, int idUsuario, boolean reemplazar) {
        super(padre, titulo(modo), ModalityType.APPLICATION_MODAL);
        this.api = api;
        this.modo = modo;
        this.idUsuario = idUsuario;
        this.reemplazar = reemplazar;

        instruccion.setFont(instruccion.getFont().deriveFont(Font.BOLD, 18f));
        instruccion.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        estado.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        cancelar.addActionListener(e -> {
            cancelado = true;
            cerrar();
        });

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(estado, BorderLayout.CENTER);
        abajo.add(cancelar, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(instruccion, BorderLayout.NORTH);
        add(abajo, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                cancelado = true;
                apagarCamara();
            }

            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                iniciar();
            }
        });
        setSize(700, 600);
        setLocationRelativeTo(padre);
    }

    private static String titulo(Modo modo) {
        switch (modo) {
            case REGISTRAR: return "SIGELAB - Registro facial";
            case VERIFICAR: return "SIGELAB - Verificación facial";
            default: return "SIGELAB - Reconocimiento facial";
        }
    }

    private void iniciar() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            terminarConError("No se encontró ninguna cámara.");
            return;
        }
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        panel = new WebcamPanel(webcam);
        panel.setMirrored(true); // solo la vista previa se ve como espejo; las fotos se envían sin espejo
        panel.setFPSDisplayed(false);
        add(panel, BorderLayout.CENTER);
        revalidate();
        new Captura().execute();
    }

    /** Toma fotos y llama a la API fuera del hilo de Swing. */
    private final class Captura extends SwingWorker<FacialApiClient.Respuesta, String[]> {
        @Override
        protected FacialApiClient.Respuesta doInBackground() throws Exception {
            publish(new String[] {"Conectando con el servidor...", ""});
            esperarCamara();
            FacialApiClient.Desafio desafio = null;
            List<Paso> pasos = new ArrayList<>();
            if (modo == Modo.REGISTRAR) {
                pasos.add(new Paso(FRENTE, 3));
                pasos.add(new Paso(IZQUIERDA, 2));
                pasos.add(new Paso(DERECHA, 2));
                pasos.add(new Paso(FRENTE, 1));
            } else {
                desafio = api.desafio(modo == Modo.VERIFICAR ? idUsuario : null);
                pasos.add(new Paso(FRENTE, 2));
                pasos.add(new Paso(desafio.esIzquierda() ? IZQUIERDA : DERECHA, 2));
            }

            List<BufferedImage> fotos = new ArrayList<>();
            for (Paso paso : pasos) {
                for (int s = 3; s >= 1 && !cancelado; s--) {
                    publish(new String[] {paso.texto, "Foto en " + s + "..."});
                    Thread.sleep(800);
                }
                for (int i = 0; i < paso.fotos && !cancelado; i++) {
                    publish(new String[] {paso.texto, "Capturando " + (i + 1) + "/" + paso.fotos});
                    BufferedImage foto = webcam.getImage();
                    if (foto != null) {
                        fotos.add(foto);
                    }
                    Thread.sleep(350);
                }
                if (cancelado) {
                    return null;
                }
            }

            publish(new String[] {"Procesando...", "Enviando " + fotos.size() + " fotos al servidor"});
            switch (modo) {
                case REGISTRAR: return api.registrar(idUsuario, reemplazar, fotos);
                case VERIFICAR: return api.verificar(idUsuario, desafio.token, fotos);
                default: return api.identificar(desafio.token, fotos);
            }
        }

        @Override
        protected void process(List<String[]> mensajes) {
            String[] ultimo = mensajes.get(mensajes.size() - 1);
            instruccion.setText(ultimo[0]);
            estado.setText(ultimo[1]);
        }

        @Override
        protected void done() {
            if (cancelado) {
                return;
            }
            try {
                resultado = get();
                mostrarResultado(resultado);
            } catch (Exception ex) {
                Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                terminarConError("No se pudo comunicar con el servidor: " + causa.getMessage());
            }
        }
    }

    private void esperarCamara() throws InterruptedException {
        for (int i = 0; i < 50 && !webcam.isOpen(); i++) {
            Thread.sleep(100);
        }
    }

    private void mostrarResultado(FacialApiClient.Respuesta r) {
        boolean ok = r.esExitosa() && (modo == Modo.REGISTRAR || r.getBoolean("matched"));
        instruccion.setForeground(ok ? new Color(0, 140, 0) : new Color(190, 0, 0));
        instruccion.setText(r.mensaje());
        List<String> motivos = r.motivosFotos();
        estado.setText(motivos.isEmpty() ? (ok ? "Listo" : r.codigo()) : String.join("  |  ", motivos));
        cancelar.setText("Cerrar");
        apagarCamara();
    }

    private void terminarConError(String mensaje) {
        instruccion.setForeground(new Color(190, 0, 0));
        instruccion.setText(mensaje);
        cancelar.setText("Cerrar");
        apagarCamara();
    }

    private void apagarCamara() {
        if (panel != null) {
            panel.stop();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    private void cerrar() {
        SwingUtilities.invokeLater(this::dispose);
    }
}
