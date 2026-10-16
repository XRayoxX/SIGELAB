import SIGELAB.view.Login;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Las ventanas de Swing deben crearse en el hilo de eventos (EDT)
        SwingUtilities.invokeLater(() -> new Login().mostrar());
    }
}
