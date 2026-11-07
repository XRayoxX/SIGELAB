import SIGELAB.util.LogoSIGELAB;
import SIGELAB.view.Login;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        // Icono de SIGELAB en todas las ventanas (en lugar de la taza de café de Java)
        LogoSIGELAB.usarEnTodasLasVentanas();

        SwingUtilities.invokeLater(() -> new Login().mostrar());
    }
}

