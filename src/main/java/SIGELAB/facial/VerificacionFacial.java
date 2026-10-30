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

public final class VerificacionFacial {

    public enum Resultado {
        APROBADO,
        RECHAZADO,
        CANCELADO,
        NO_DISPONIBLE
    }

    private VerificacionFacial() {
    }

    public static Resultado verificarEnLogin(
            Window padre,
            Usuario usuario
    ) {

        if (usuario == null || usuario.getIdUsuario() <= 0) {
            return Resultado.RECHAZADO;
        }

        // RN-02: nunca aprobar por falta de configuración.
        if (!ServicioFacial.habilitado()) {

            JOptionPane.showMessageDialog(
                    padre,
                    "El reconocimiento facial no está configurado.\n\n"
                            + "Por seguridad, SIGELAB no permitirá "
                            + "el inicio de sesión.",
                    "Seguridad SIGELAB",
                    JOptionPane.ERROR_MESSAGE
            );

            return Resultado.NO_DISPONIBLE;
        }

        FacialApiClient api;

        try {
            api = ServicioFacial.cliente();
        } catch (Exception ex) {
            return Resultado.NO_DISPONIBLE;
        }

        FacialApiClient.Respuesta estado;

        try {
            estado = conEspera(
                    padre,
                    "Consultando el registro facial...",
                    () -> api.estado(usuario.getIdUsuario())
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    padre,
                    "No se pudo conectar con el servicio facial.\n"
                            + "Revise su conexión e intente nuevamente.",
                    "Reconocimiento facial",
                    JOptionPane.ERROR_MESSAGE
            );

            return Resultado.NO_DISPONIBLE;
        }

        if (estado == null || !estado.esExitosa()) {

            JOptionPane.showMessageDialog(
                    padre,
                    "No fue posible consultar el registro facial.",
                    "Reconocimiento facial",
                    JOptionPane.ERROR_MESSAGE
            );

            return Resultado.NO_DISPONIBLE;
        }

        // El enrolamiento debe efectuarse mediante
        // un proceso administrativo autorizado.
        if (!estado.getBoolean("enrolled")) {

            JOptionPane.showMessageDialog(
                    padre,
                    "Su cuenta todavía no tiene un rostro registrado.\n\n"
                            + "Solicite al administrador que realice "
                            + "su registro facial.\n\n"
                            + "Por seguridad, no se permite registrar "
                            + "un rostro durante el inicio de sesión.",
                    "Registro facial pendiente",
                    JOptionPane.WARNING_MESSAGE
            );

            return Resultado.RECHAZADO;
        }

        FacialApiClient.Respuesta respuesta;

        try {
            respuesta = CapturaFacialDialog.verificar(
                    padre,
                    api,
                    usuario.getIdUsuario()
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    padre,
                    "No se pudo completar la verificación facial.",
                    "Reconocimiento facial",
                    JOptionPane.ERROR_MESSAGE
            );

            return Resultado.NO_DISPONIBLE;
        }

        if (respuesta == null) {
            return Resultado.CANCELADO;
        }

        if (!respuesta.esExitosa()) {

            JOptionPane.showMessageDialog(
                    padre,
                    "La verificación facial fue rechazada.",
                    "Acceso denegado",
                    JOptionPane.WARNING_MESSAGE
            );

            return Resultado.RECHAZADO;
        }

        if (!respuesta.getBoolean("matched")) {

            JOptionPane.showMessageDialog(
                    padre,
                    "El rostro no coincide con el usuario registrado.",
                    "Acceso denegado",
                    JOptionPane.WARNING_MESSAGE
            );

            return Resultado.RECHAZADO;
        }

        return Resultado.APROBADO;
    }

    public static <T> T conEspera(
            Window padre,
            String mensaje,
            Callable<T> tarea
    ) throws Exception {

        JDialog espera = new JDialog(
                padre,
                "SIGELAB",
                JDialog.ModalityType.APPLICATION_MODAL
        );

        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);

        JPanel contenido = new JPanel(
                new BorderLayout(10, 10)
        );

        contenido.setBorder(
                BorderFactory.createEmptyBorder(
                        16, 16, 16, 16
                )
        );

        contenido.add(
                new JLabel(mensaje),
                BorderLayout.NORTH
        );

        contenido.add(
                barra,
                BorderLayout.CENTER
        );

        contenido.add(
                new JLabel(
                        "Espere mientras se consulta el servicio facial..."
                ),
                BorderLayout.SOUTH
        );

        espera.setContentPane(contenido);

        espera.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE
        );

        espera.pack();
        espera.setLocationRelativeTo(padre);

        SwingWorker<T, Void> worker =
                new SwingWorker<>() {

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
        espera.setVisible(true);

        try {
            return worker.get();

        } catch (ExecutionException ex) {

            Throwable causa = ex.getCause();

            if (causa instanceof Exception) {
                throw (Exception) causa;
            }

            throw ex;
        }
    }
}