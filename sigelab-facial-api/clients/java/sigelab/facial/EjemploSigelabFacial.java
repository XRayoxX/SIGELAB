package sigelab.facial;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Ejemplo mínimo de integración en SIGELAB. Copie la parte que necesite en sus formularios.
 *
 * Ponga aquí la URL y la API key que imprime deploy-azure.ps1 al terminar.
 * (En el proyecto real conviene leerlas de un archivo config.properties.)
 */
public class EjemploSigelabFacial {

    private static final String URL_API = "https://sigelab-facial-api.CAMBIAR.azurecontainerapps.io";
    private static final String API_KEY = "CAMBIAR";

    public static void main(String[] args) {
        FacialApiClient api = new FacialApiClient(URL_API, API_KEY, "sigelab-desktop");

        // Despertar la API apenas abre la aplicación (si estaba apagada tarda unos segundos).
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                api.salud();
                return null;
            }
        }.execute();

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("SIGELAB - Prueba de reconocimiento facial");
            ventana.setLayout(new FlowLayout());

            JButton registrar = new JButton("Registrar mi rostro");
            registrar.addActionListener(e -> {
                int id = pedirId(ventana);
                if (id > 0) {
                    FacialApiClient.Respuesta r = CapturaFacialDialog.registrar(ventana, api, id, true);
                    mostrar(ventana, r);
                }
            });

            JButton verificar = new JButton("Verificar (después del login)");
            verificar.addActionListener(e -> {
                int id = pedirId(ventana);
                if (id > 0) {
                    FacialApiClient.Respuesta r = CapturaFacialDialog.verificar(ventana, api, id);
                    if (r != null && r.esExitosa() && r.getBoolean("matched")) {
                        JOptionPane.showMessageDialog(ventana, "Acceso concedido a " + r.getString("username"));
                    } else {
                        mostrar(ventana, r);
                    }
                }
            });

            JButton identificar = new JButton("Marcar ingreso con rostro");
            identificar.addActionListener(e -> {
                FacialApiClient.Respuesta r = CapturaFacialDialog.identificar(ventana, api);
                if (r != null && r.esExitosa() && r.getBoolean("matched")) {
                    int idUsuario = r.getInt("userId");
                    // Aquí registraría el ingreso al laboratorio en su tabla de SIGELAB.
                    JOptionPane.showMessageDialog(ventana, "Bienvenido " + r.getString("username") + " (id " + idUsuario + ")");
                } else {
                    mostrar(ventana, r);
                }
            });

            ventana.add(registrar);
            ventana.add(verificar);
            ventana.add(identificar);
            ventana.setSize(620, 120);
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    private static int pedirId(JFrame ventana) {
        String texto = JOptionPane.showInputDialog(ventana, "ID del usuario:");
        try {
            return texto == null ? 0 : Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static void mostrar(JFrame ventana, FacialApiClient.Respuesta r) {
        if (r != null) {
            JOptionPane.showMessageDialog(ventana, r.mensaje());
        }
    }
}
