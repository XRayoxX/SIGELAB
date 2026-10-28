package SIGELAB.facial;

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
import javax.swing.JOptionPane;
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
        final String objetivo; // pose que debe detectar la API: "front", "left" o "right"
        final int fotos;

        Paso(String texto, String objetivo, int fotos) {
            this.texto = texto;
            this.objetivo = objetivo;
            this.fotos = fotos;
        }
    }

    /** Tiempo máximo para lograr las fotos de un paso. */
    private static final long LIMITE_POR_PASO_MS = 25_000;

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
        instruccion.setText("Buscando cámaras...");
        new SwingWorker<List<Webcam>, Void>() {
            @Override
            protected List<Webcam> doInBackground() throws Exception {
                return Webcam.getWebcams(15, java.util.concurrent.TimeUnit.SECONDS);
            }

            @Override
            protected void done() {
                if (cancelado) {
                    return;
                }
                try {
                    List<Webcam> camaras = get();
                    if (camaras.isEmpty()) {
                        terminarConError("No se encontró ninguna cámara en esta computadora.");
                        return;
                    }
                    if (camaras.size() == 1) {
                        webcam = camaras.get(0);
                    } else {
                        String[] nombres = camaras.stream().map(Webcam::getName).toArray(String[]::new);
                        String seleccion = (String) JOptionPane.showInputDialog(
                                CapturaFacialDialog.this,
                                "Seleccione la cámara que desea usar para el reconocimiento facial:",
                                "Elegir cámara", JOptionPane.QUESTION_MESSAGE,
                                null, nombres, nombres[0]);
                        if (seleccion == null) {
                            cancelado = true;
                            cerrar();
                            return;
                        }
                        webcam = camaras.get(java.util.Arrays.asList(nombres).indexOf(seleccion));
                    }
                    if (!cancelado) {
                        abrirCamaraSeleccionada();
                    }
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    terminarConError("No se pudo preparar la cámara: " + causa.getMessage());
                }
            }
        }.execute();
    }

    private void abrirCamaraSeleccionada() {
        instruccion.setText("Iniciando cámara: " + webcam.getName());
        try {
            if (java.util.Arrays.asList(webcam.getViewSizes()).contains(WebcamResolution.VGA.getSize())) {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
            }
            panel = new WebcamPanel(webcam);
            panel.setMirrored(true); // solo la vista previa se ve como espejo; las fotos se envían sin espejo
            panel.setFPSDisplayed(false);
            add(panel, BorderLayout.CENTER);
            revalidate();
            new Captura().execute();
        } catch (RuntimeException ex) {
            terminarConError("No se pudo abrir la cámara seleccionada: " + ex.getMessage());
        }
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
                pasos.add(new Paso(FRENTE, "front", 3));
                pasos.add(new Paso(IZQUIERDA, "left", 2));
                pasos.add(new Paso(DERECHA, "right", 2));
            } else {
                desafio = api.desafio(modo == Modo.VERIFICAR ? idUsuario : null);
                pasos.add(new Paso(FRENTE, "front", 2));
                pasos.add(desafio.esIzquierda() ? new Paso(IZQUIERDA, "left", 2) : new Paso(DERECHA, "right", 2));
            }

            // Cada foto se revisa con la API al momento (calidad y pose) y solo se guarda si sirve.
            List<BufferedImage> fotos = new ArrayList<>();
            for (int p = 0; p < pasos.size(); p++) {
                Paso paso = pasos.get(p);
                String titulo = "Paso " + (p + 1) + " de " + pasos.size() + ": " + paso.texto;
                for (int s = 2; s >= 1 && !cancelado; s--) {
                    publish(new String[] {titulo, "Prepárese... " + s});
                    Thread.sleep(700);
                }
                int logradas = 0;
                long limite = System.currentTimeMillis() + LIMITE_POR_PASO_MS;
                while (logradas < paso.fotos && !cancelado && System.currentTimeMillis() < limite) {
                    BufferedImage foto = webcam.getImage();
                    if (foto == null) {
                        Thread.sleep(200);
                        continue;
                    }
                    FacialApiClient.Respuesta analisis = api.analizar(List.of(foto));
                    if (!analisis.esExitosa()) {
                        throw new IllegalStateException(analisis.mensaje());
                    }
                    boolean valida = analisis.getBoolean("accepted");
                    String pose = analisis.getString("pose");
                    if (valida && paso.objetivo.equals(pose)) {
                        fotos.add(foto);
                        logradas++;
                        publish(new String[] {titulo, "✓ Foto " + logradas + " de " + paso.fotos + " lista"});
                        Thread.sleep(300);
                    } else {
                        publish(new String[] {titulo, consejo(paso.objetivo, valida, pose, analisis.getString("reason"))});
                    }
                }
                if (cancelado) {
                    return null;
                }
                if (logradas < paso.fotos) {
                    throw new IllegalStateException("No se logró la foto: \"" + paso.texto
                            + "\". Intente de nuevo con buena luz y con el rostro dentro de la cámara.");
                }
            }

            if (fotos.isEmpty()) {
                throw new IllegalStateException("La cámara no entregó imágenes. Revise que no esté siendo usada por otro programa.");
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
                if (causa instanceof IllegalStateException) {
                    terminarConError(causa.getMessage());
                } else {
                    terminarConError("No se pudo comunicar con el servicio facial. Revise su conexión a internet. ("
                            + causa.getMessage() + ")");
                }
            }
        }
    }

    /** Indicación para el usuario cuando la foto no sirvió. */
    private static String consejo(String objetivo, boolean valida, String pose, String motivo) {
        if (!valida) {
            return motivo != null ? motivo : "Coloque su rostro frente a la cámara";
        }
        if ("front".equals(objetivo)) {
            return "Mire directamente a la cámara, sin girar la cabeza";
        }
        String lado = "left".equals(objetivo) ? "IZQUIERDA" : "DERECHA";
        String contrario = "left".equals(objetivo) ? "right" : "left";
        if (contrario.equals(pose)) {
            return "Hacia el otro lado: gire hacia su " + lado;
        }
        return "Gire un poco más la cabeza hacia su " + lado;
    }

    private void esperarCamara() throws InterruptedException {
        for (int i = 0; i < 100 && !webcam.isOpen(); i++) {
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
        if (ok) {
            // Si todo salió bien, la ventana se cierra sola después de mostrar el mensaje.
            javax.swing.Timer cierre = new javax.swing.Timer(1500, e -> dispose());
            cierre.setRepeats(false);
            cierre.start();
        }
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
