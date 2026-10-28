package SIGELAB.util;

import java.awt.Color;
import java.util.prefs.Preferences;

public final class TemaSIGELAB {

    private static final Preferences PREFERENCIAS =
            Preferences.userNodeForPackage(TemaSIGELAB.class);

    private static final String CLAVE_TEMA = "sigelab.tema.oscuro";

    private static boolean modoOscuro =
            PREFERENCIAS.getBoolean(CLAVE_TEMA, true);

    private TemaSIGELAB() {
        // Clase de utilidades.
    }

    // =====================================
    // CONTROL DEL TEMA
    // =====================================

    public static boolean esOscuro() {
        return modoOscuro;
    }

    public static void establecerModoOscuro(boolean oscuro) {
        modoOscuro = oscuro;
        PREFERENCIAS.putBoolean(CLAVE_TEMA, oscuro);
    }

    public static void alternarTema() {
        establecerModoOscuro(!modoOscuro);
    }

    // =====================================
    // PALETA PRINCIPAL
    // =====================================

    public static Color fondo() {
        return modoOscuro
                ? new Color(12, 19, 34)
                : new Color(245, 250, 245);
    }

    public static Color panel() {
        return modoOscuro
                ? new Color(23, 37, 59)
                : Color.WHITE;
    }

    public static Color navegacion() {
        return modoOscuro
                ? new Color(17, 29, 48)
                : new Color(21, 87, 36);
    }

    public static Color primario() {
        return modoOscuro
                ? new Color(20, 140, 194)
                : new Color(25, 135, 84);
    }

    public static Color acento() {
        return modoOscuro
                ? new Color(56, 189, 248)
                : new Color(255, 193, 7);
    }

    // =====================================
    // TEXTOS
    // =====================================

    public static Color texto() {
        return modoOscuro
                ? new Color(229, 242, 254)
                : new Color(23, 55, 83);
    }

    public static Color textoSecundario() {
        return modoOscuro
                ? new Color(148, 175, 200)
                : new Color(88, 113, 139);
    }

    public static Color textoSobrePrimario() {
        return Color.WHITE;
    }

    // =====================================
    // CAMPOS Y BORDES
    // =====================================

    public static Color campo() {
        return modoOscuro
                ? new Color(15, 25, 42)
                : Color.WHITE;
    }

    public static Color borde() {
        return modoOscuro
                ? new Color(43, 64, 93)
                : new Color(207, 219, 231);
    }

    // =====================================
    // ESTADOS
    // =====================================

    public static Color exito() {
        return new Color(20, 184, 166);
    }

    public static Color advertencia() {
        return new Color(245, 158, 11);
    }

    public static Color error() {
        return new Color(239, 68, 68);
    }

    public static Color informacion() {
        return new Color(56, 189, 248);
    }
}
