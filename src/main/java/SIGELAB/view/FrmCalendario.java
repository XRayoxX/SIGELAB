
package SIGELAB.view;

import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FrmCalendario {

    // Componentes de FrmCalendario.form
    private JPanel panelPrincipal;
    private JPanel panelDias;
    private JLabel lblMes;

    private JButton btnAnterior;
    private JButton btnSiguiente;
    private JButton btnHoy;
    private JButton btnCancelar;

    private final JDialog ventana;

    private YearMonth mesActual;
    private LocalDate fechaSeleccionada;

    private static final Color AZUL =
            new Color(54, 125, 170);

    private FrmCalendario(
            Window propietario,
            LocalDate fechaInicial) {

        ventana = new JDialog(
                propietario,
                "SIGELAB - Seleccionar fecha",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        ventana.setContentPane(panelPrincipal);

        ventana.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );

        ventana.setSize(410, 410);
        ventana.setMinimumSize(
                new Dimension(380, 380)
        );

        ventana.setLocationRelativeTo(propietario);

        mesActual = YearMonth.from(
                fechaInicial == null
                        ? LocalDate.now()
                        : fechaInicial
        );

        panelDias.setLayout(
                new GridLayout(0, 7, 5, 5)
        );

        btnAnterior.addActionListener(e -> {
            mesActual = mesActual.minusMonths(1);
            actualizarCalendario();
        });

        btnSiguiente.addActionListener(e -> {
            mesActual = mesActual.plusMonths(1);
            actualizarCalendario();
        });

        btnHoy.addActionListener(e -> {
            seleccionar(LocalDate.now());
        });

        btnCancelar.addActionListener(e -> {
            ventana.dispose();
        });

        actualizarCalendario();
    }

    public static LocalDate seleccionarFecha(
            Window propietario,
            LocalDate fechaInicial) {

        FrmCalendario calendario =
                new FrmCalendario(
                        propietario,
                        fechaInicial
                );

        calendario.ventana.setVisible(true);

        return calendario.fechaSeleccionada;
    }

    private void actualizarCalendario() {

        boolean oscuro = TemaSIGELAB.esOscuro();

        Color fondo = oscuro
                ? new Color(24, 35, 51)
                : Color.WHITE;

        Color texto = oscuro
                ? new Color(227, 234, 242)
                : new Color(35, 51, 67);

        Color secundario = oscuro
                ? new Color(151, 167, 184)
                : new Color(100, 117, 135);

        panelPrincipal.setBackground(fondo);
        panelDias.setBackground(fondo);

        lblMes.setForeground(texto);

        lblMes.setFont(
                new Font("Segoe UI", Font.BOLD, 16)
        );

        Locale idioma =
                Locale.forLanguageTag("es-GT");

        lblMes.setText(
                mesActual.atDay(1).format(
                        DateTimeFormatter.ofPattern(
                                "MMMM yyyy",
                                idioma
                        )
                ).toUpperCase(idioma)
        );

        panelDias.removeAll();

        String[] dias = {
                "L", "M", "M", "J", "V", "S", "D"
        };

        for (String dia : dias) {

            JLabel etiqueta =
                    new JLabel(dia, SwingConstants.CENTER);

            etiqueta.setForeground(secundario);

            etiqueta.setFont(
                    new Font("Segoe UI", Font.BOLD, 12)
            );

            panelDias.add(etiqueta);
        }

        int desplazamiento =
                mesActual.atDay(1)
                        .getDayOfWeek()
                        .getValue() - 1;

        for (int i = 0; i < desplazamiento; i++) {

            panelDias.add(new JLabel(""));
        }

        for (int dia = 1;
             dia <= mesActual.lengthOfMonth();
             dia++) {

            final int numeroDia = dia;

            LocalDate fecha =
                    mesActual.atDay(dia);

            JButton boton = new JButton(
                    String.valueOf(dia)
            );

            boton.setFocusPainted(false);

            boton.setFont(
                    new Font("Segoe UI", Font.PLAIN, 12)
            );

            boton.setBorder(
                    new EmptyBorder(5, 5, 5, 5)
            );

            boolean esHoy =
                    fecha.equals(LocalDate.now());

            boton.setOpaque(true);

            boton.setBackground(
                    esHoy ? AZUL : fondo
            );

            boton.setForeground(
                    esHoy ? Color.WHITE : texto
            );

            boton.addActionListener(e -> {
                seleccionar(
                        mesActual.atDay(numeroDia)
                );
            });

            panelDias.add(boton);
        }

        int celdas =
                7 + desplazamiento
                        + mesActual.lengthOfMonth();

        while ((celdas - 7) % 7 != 0) {
            panelDias.add(new JLabel(""));
            celdas++;
        }

        estilizarBoton(btnAnterior, fondo, texto);
        estilizarBoton(btnSiguiente, fondo, texto);
        estilizarBoton(btnCancelar, fondo, texto);

        btnHoy.setBackground(AZUL);
        btnHoy.setForeground(Color.WHITE);
        btnHoy.setFocusPainted(false);

        panelDias.revalidate();
        panelDias.repaint();
    }

    private void seleccionar(LocalDate fecha) {

        fechaSeleccionada = fecha;
        ventana.dispose();
    }

    private void estilizarBoton(
            JButton boton,
            Color fondo,
            Color texto) {

        boton.setOpaque(true);
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setFocusPainted(false);
    }
}
