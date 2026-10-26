package SIGELAB.facial;

import SIGELAB.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Segundo factor del inicio de sesión (RN-02): verificación facial con la API en Azure.
 *
 * Flujo:
 *  - Si el usuario ya registró su rostro: se abre la cámara y se verifica (con prueba de vida).
 *  - Si todavía no lo tiene: se le ofrece registrarlo en ese momento (primer ingreso).
 */
public final class VerificacionFacial {

    public enum Resultado {
        /** Rostro verificado (o registrado por primera vez). Se puede completar el login. */
        APROBADO,
        /** El rostro no coincide o no pasó la prueba de vida. */
        RECHAZADO,
        /** El usuario cerró la ventana o no quiso registrar su rostro. */
        CANCELADO,
        /** No se pudo contactar la API (sin internet, API caída, configuración incompleta). */
        NO_DISPONIBLE
    }

    private VerificacionFacial() {
    }

    public static Resultado verificarEnLogin(Window padre, Usuario usuario) {
        if (!ServicioFacial.habilitado()) {
            return Resultado.APROBADO; // desactivado en config.properties
        }
        FacialApiClient api = ServicioFacial.cliente();

        FacialApiClient.Respuesta estado;
        try {
            estado = conEspera(padre, "Conectando con el servicio de reconocimiento facial...",
                    () -> api.estado(usuario.getIdUsuario()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(padre,
                    "No se pudo contactar el servicio de reconocimiento facial.\n"
                            + "Revise su conexión a internet e intente de nuevo.",
                    "Reconocimiento facial", JOptionPane.ERROR_MESSAGE);
            return Resultado.NO_DISPONIBLE;
        }
        if (!estado.esExitosa()) {
            JOptionPane.showMessageDialog(padre, "Servicio facial: " + estado.mensaje(),
                    "Reconocimiento facial", JOptionPane.ERROR_MESSAGE);
            return Resultado.NO_DISPONIBLE;
        }

        if (!estado.getBoolean("enrolled")) {
            int opcion = JOptionPane.showConfirmDialog(padre,
                    "Hola " + usuario.getNombre() + ", todavía no tiene su rostro registrado.\n\n"
                            + "Para ingresar a SIGELAB debe registrarlo ahora. Se abrirá la cámara y\n"
                            + "se le pedirá mirar al frente y girar la cabeza a la izquierda y a la derecha.\n\n"
                            + "¿Desea registrar su rostro?",
                    "Registro facial", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return Resultado.CANCELADO;
            }
            FacialApiClient.Respuesta r = CapturaFacialDialog.registrar(padre, api, usuario.getIdUsuario(), false);
            if (r == null) {
                return Resultado.CANCELADO;
            }
            return r.esExitosa() ? Resultado.APROBADO : Resultado.RECHAZADO;
        }

        FacialApiClient.Respuesta r = CapturaFacialDialog.verificar(padre, api, usuario.getIdUsuario());
        if (r == null) {
            return Resultado.CANCELADO;
        }
        return r.esExitosa() && r.getBoolean("matched") ? Resultado.APROBADO : Resultado.RECHAZADO;
    }

    /**
     * Ejecuta una tarea de red mostrando una ventanita de "espere" sin congelar la aplicación.
     * Devuelve el resultado o lanza la excepción de la tarea.
     */
    public static <T> T conEspera(Window padre, String mensaje, Callable<T> tarea) throws Exception {
        JDialog espera = new JDialog(padre, "SIGELAB", JDialog.ModalityType.APPLICATION_MODAL);
        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(new JLabel(mensaje), BorderLayout.NORTH);
        contenido.add(barra, BorderLayout.CENTER);
        contenido.add(new JLabel("(la primera conexión del día puede tardar hasta un minuto)"), BorderLayout.SOUTH);
        espera.setContentPane(contenido);
        espera.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        espera.pack();
        espera.setLocationRelativeTo(padre);

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                espera.dispose();
            }
        };
        worker.execute();
        espera.setVisible(true); // bloquea hasta que la tarea termine (done() cierra la ventana)
        try {
            return worker.get();
        } catch (ExecutionException ex) {
            Throwable causa = ex.getCause();
            throw causa instanceof Exception ? (Exception) causa : ex;
        }
    }
}
