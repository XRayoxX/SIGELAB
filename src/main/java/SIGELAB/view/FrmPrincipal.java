package SIGELAB.view;

import SIGELAB.controller.DashboardController;
import SIGELAB.controller.LoginController;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.ResumenDashboard;
import SIGELAB.model.Usuario;
import SIGELAB.util.Dialogos;
import SIGELAB.util.Sesion;
import SIGELAB.util.EstiloFormulario;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class FrmPrincipal {

    // =====================================================
    // CONTENEDORES DEFINIDOS EN EL .FORM
    // =====================================================

    private JPanel panelPrincipal;
    private JPanel panelNavegacion;
    private JPanel panelContenido;
    private JPanel panelEncabezado;
    private JPanel panelIndicadores;
    private JPanel panelInventario;
    private JPanel panelIndicadoresSecundarios;

    // Nuevos contenedores desplazables
    private JScrollPane scrollNavegacion;
    private JScrollPane scrollContenido;

    // Tarjetas individuales
    private JPanel cardTotal;
    private JPanel cardDisponibles;
    private JPanel cardPrestados;
    private JPanel cardMantenimiento;

    private JPanel cardDanados;
    private JPanel cardBaja;
    private JPanel cardPrestamosActivos;
    private JPanel cardPrestamosVencidos;

    // =====================================================
    // ETIQUETAS
    // =====================================================

    private JLabel lblMarca;
    private JLabel lblSubMarca;
    private JLabel lblGeneral;
    private JLabel lblDashboardMenu;
    private JLabel lblOperaciones;
    private JLabel lblCatalogos;
    private JLabel lblGestion;

    private JLabel lblTitulo;
    private JLabel lblUsuarioActual;
    private JLabel lblTituloResumen;
    private JLabel lblTituloInventario;
    private JLabel lblDescripcionInventario;
    private JLabel lblTituloSecundarios;

    // Los ocho indicadores originales
    private JLabel lblTotalEquipos;
    private JLabel lblDisponibles;
    private JLabel lblPrestados;
    private JLabel lblDanados;
    private JLabel lblMantenimiento;
    private JLabel lblBaja;
    private JLabel lblPrestamosActivos;
    private JLabel lblPrestamosVencidos;

    // =====================================================
    // BOTONES
    // =====================================================

    private JButton btnPrestamos;
    private JButton btnDevoluciones;
    private JButton btnEquipos;
    private JButton btnTiposEquipo;
    private JButton btnLaboratorios;
    private JButton btnSolicitantes;
    private JButton btnMantenimiento;
    private JButton btnReportes;
    private JButton btnUsuarios;
    private JButton btnAuditoria;

    private JButton btnActualizarResumen;
    private JButton btnCerrarSesion;
    private JButton btnSalir;

    private JButton btnModoClaro;
    private JButton btnModoOscuro;

    // =====================================================
    // INVENTARIO
    // =====================================================

    private JProgressBar barDisponibles;
    private JProgressBar barPrestados;
    private JProgressBar barDanados;
    private JProgressBar barMantenimiento;
    private JProgressBar barBaja;

    // =====================================================
    // VENTANA Y CONTROLADOR
    // =====================================================

    private final JFrame ventana;

    private final DashboardController dashboard =
            new DashboardController();

    // =====================================================
    // PALETA OFICIAL FORMAL
    // =====================================================

    private static final Color OSCURO_FONDO =
            new Color(13, 20, 32);

    private static final Color OSCURO_LATERAL =
            new Color(17, 28, 43);

    private static final Color OSCURO_TARJETA =
            new Color(24, 35, 51);

    private static final Color OSCURO_BORDE =
            new Color(42, 58, 76);

    private static final Color OSCURO_TEXTO =
            new Color(227, 234, 242);

    private static final Color OSCURO_SECUNDARIO =
            new Color(151, 167, 184);

    private static final Color CLARO_FONDO =
            new Color(244, 247, 250);

    private static final Color CLARO_LATERAL =
            new Color(236, 242, 247);

    private static final Color CLARO_TARJETA =
            Color.WHITE;

    private static final Color CLARO_BORDE =
            new Color(215, 224, 233);

    private static final Color CLARO_TEXTO =
            new Color(35, 51, 67);

    private static final Color CLARO_SECUNDARIO =
            new Color(100, 117, 135);

    private static final Color AZUL_FORMAL =
            new Color(54, 125, 170);

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FrmPrincipal() {

        ventana = new JFrame(
                "SIGELAB - Centro de control"
        );

        ventana.setContentPane(panelPrincipal);

        ventana.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        // Permite reducir la ventana sin perder acceso
        // a los módulos.
        ventana.setMinimumSize(
                new Dimension(800, 500)
        );

        // Pantalla completa (maximizada), respetando la barra de tareas de Windows.
        EstiloFormulario.pantallaCompleta(ventana);

        configurarScroll();

        cargarUsuarioActual();

        adaptarMenuAlRol();

        configurarEventos();

        aplicarTema();

        ventana.addComponentListener(
                new ComponentAdapter() {

                    @Override
                    public void componentShown(
                            ComponentEvent e
                    ) {
                        // Si el tema se cambió dentro de un módulo, el menú lo refleja al volver
                        aplicarTema();
                        cargarResumen();
                    }
                }
        );
    }

    // =====================================================
    // SCROLL AUTOMÁTICO
    // =====================================================

    private void configurarScroll() {

        // MENÚ LATERAL

        scrollNavegacion.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scrollNavegacion.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollNavegacion.getVerticalScrollBar()
                .setUnitIncrement(20);

        scrollNavegacion.setBorder(null);

        scrollNavegacion.getViewport()
                .setScrollMode(
                        JViewport.BLIT_SCROLL_MODE
                );

        // ÁREA CENTRAL

        scrollContenido.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scrollContenido.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scrollContenido.getVerticalScrollBar()
                .setUnitIncrement(20);

        scrollContenido.getHorizontalScrollBar()
                .setUnitIncrement(20);

        scrollContenido.setBorder(null);
    }

    // =====================================================
    // USUARIO ACTUAL
    // =====================================================

    private void cargarUsuarioActual() {

        Usuario usuario =
                Sesion.getUsuarioActual();

        if (usuario == null) {

            lblUsuarioActual.setText(
                    "Sin sesión activa"
            );

            return;
        }

        lblUsuarioActual.setText(
                "Usuario: "
                        + usuario.getNombreCompleto()
                        + "  |  "
                        + usuario.getNombreUsuario()
                        + "  |  Rol: "
                        + usuario.getRol()
        );
    }

    // =====================================================
    // PERMISOS ORIGINALES
    // =====================================================

    private void adaptarMenuAlRol() {

        boolean admin =
                Sesion.tieneRol(
                        RolUsuario.ADMIN
                );

        boolean operador =
                Sesion.tieneRol(
                        RolUsuario.ADMIN,
                        RolUsuario.ENCARGADO
                );

        btnUsuarios.setVisible(admin);
        btnAuditoria.setVisible(admin);
        btnTiposEquipo.setVisible(admin);
        btnLaboratorios.setVisible(admin);

        btnPrestamos.setVisible(operador);
        btnDevoluciones.setVisible(operador);
        btnEquipos.setVisible(operador);
        btnSolicitantes.setVisible(operador);
        btnMantenimiento.setVisible(operador);

        btnReportes.setVisible(true);

        lblOperaciones.setVisible(operador);

        lblCatalogos.setVisible(
                admin || operador
        );
    }

    // =====================================================
    // EVENTOS
    // =====================================================

    private void configurarEventos() {

        btnActualizarResumen.addActionListener(
                e -> cargarResumen()
        );

        btnPrestamos.addActionListener(
                e -> abrir(
                        new FrmPrestamos(ventana)::mostrar
                )
        );

        btnDevoluciones.addActionListener(
                e -> abrir(
                        new FrmDevoluciones(ventana)::mostrar
                )
        );

        btnEquipos.addActionListener(
                e -> abrir(
                        new FrmEquipos(ventana)::mostrar
                )
        );

        btnMantenimiento.addActionListener(
                e -> abrir(
                        new FrmMantenimiento(ventana)::mostrar
                )
        );

        btnTiposEquipo.addActionListener(
                e -> abrir(
                        new FrmTiposEquipo(ventana)::mostrar
                )
        );

        btnLaboratorios.addActionListener(
                e -> abrir(
                        new FrmLaboratorios(ventana)::mostrar
                )
        );

        btnSolicitantes.addActionListener(
                e -> abrir(
                        new FrmSolicitantes(ventana)::mostrar
                )
        );

        btnReportes.addActionListener(
                e -> abrir(
                        new FrmReportes(ventana)::mostrar
                )
        );

        btnUsuarios.addActionListener(
                e -> abrir(
                        new FrmUsuarios(ventana)::mostrar
                )
        );

        btnAuditoria.addActionListener(
                e -> abrir(
                        new FrmAuditoria(ventana)::mostrar
                )
        );

        // TEMA

        btnModoClaro.addActionListener(
                e -> cambiarTema(false)
        );

        btnModoOscuro.addActionListener(
                e -> cambiarTema(true)
        );

        // SESIÓN

        btnCerrarSesion.addActionListener(
                e -> cerrarSesion()
        );

        btnSalir.addActionListener(
                e -> salirSistema()
        );
    }

    // =====================================================
    // TEMA CLARO / OSCURO
    // =====================================================

    private void cambiarTema(
            boolean modoOscuro
    ) {

        TemaSIGELAB.establecerModoOscuro(
                modoOscuro
        );

        aplicarTema();
    }

    private void aplicarTema() {

        boolean oscuro =
                TemaSIGELAB.esOscuro();

        Color fondo = oscuro
                ? OSCURO_FONDO
                : CLARO_FONDO;

        Color lateral = oscuro
                ? OSCURO_LATERAL
                : CLARO_LATERAL;

        Color tarjeta = oscuro
                ? OSCURO_TARJETA
                : CLARO_TARJETA;

        Color borde = oscuro
                ? OSCURO_BORDE
                : CLARO_BORDE;

        Color texto = oscuro
                ? OSCURO_TEXTO
                : CLARO_TEXTO;

        Color secundario = oscuro
                ? OSCURO_SECUNDARIO
                : CLARO_SECUNDARIO;

        // FONDOS

        panelPrincipal.setBackground(fondo);

        panelNavegacion.setBackground(lateral);

        panelContenido.setBackground(fondo);

        panelEncabezado.setBackground(fondo);

        panelIndicadores.setBackground(fondo);

        panelIndicadoresSecundarios.setBackground(fondo);

        scrollNavegacion.setBackground(lateral);

        scrollContenido.setBackground(fondo);

        scrollNavegacion.getViewport()
                .setBackground(lateral);

        scrollContenido.getViewport()
                .setBackground(fondo);

        // SCROLLBARS

        scrollNavegacion.getVerticalScrollBar()
                .setBackground(lateral);

        scrollContenido.getVerticalScrollBar()
                .setBackground(fondo);

        scrollContenido.getHorizontalScrollBar()
                .setBackground(fondo);

        // TEXTO GENERAL DENTRO DE LOS PANELES

        aplicarTextoBase(
                panelContenido,
                secundario
        );

        aplicarTextoBase(
                panelNavegacion,
                secundario
        );

        // TARJETAS PRINCIPALES

        JPanel[] tarjetasPrincipales = {
                cardTotal,
                cardDisponibles,
                cardPrestados,
                cardMantenimiento
        };

        for (JPanel panel : tarjetasPrincipales) {

            configurarTarjeta(
                    panel,
                    tarjeta,
                    borde
            );
        }

        // TARJETAS SECUNDARIAS

        JPanel[] tarjetasSecundarias = {
                cardDanados,
                cardBaja,
                cardPrestamosActivos,
                cardPrestamosVencidos
        };

        for (JPanel panel : tarjetasSecundarias) {

            configurarTarjeta(
                    panel,
                    tarjeta,
                    borde
            );
        }

        configurarTarjeta(
                panelInventario,
                tarjeta,
                borde
        );

        // MARCA

        lblMarca.setFont(
                fuente(Font.BOLD, 29)
        );

        lblMarca.setForeground(texto);

        lblSubMarca.setFont(
                fuente(Font.PLAIN, 10)
        );

        lblSubMarca.setForeground(
                secundario
        );

        // CATEGORÍAS LATERALES

        JLabel[] categorias = {
                lblGeneral,
                lblOperaciones,
                lblCatalogos,
                lblGestion
        };

        for (JLabel categoria : categorias) {

            categoria.setFont(
                    fuente(Font.BOLD, 10)
            );

            categoria.setForeground(
                    secundario
            );

            categoria.setBorder(
                    new EmptyBorder(
                            13, 7, 5, 0
                    )
            );
        }

        // OPCIÓN ACTIVA

        lblDashboardMenu.setOpaque(true);

        lblDashboardMenu.setBackground(
                oscuro
                        ? new Color(32, 69, 96)
                        : new Color(216, 231, 241)
        );

        lblDashboardMenu.setForeground(
                oscuro
                        ? Color.WHITE
                        : new Color(38, 89, 122)
        );

        lblDashboardMenu.setFont(
                fuente(Font.BOLD, 13)
        );

        lblDashboardMenu.setBorder(
                new EmptyBorder(
                        10, 11, 10, 11
                )
        );

        // BOTONES DE NAVEGACIÓN

        JButton[] botones = {
                btnPrestamos,
                btnDevoluciones,
                btnEquipos,
                btnMantenimiento,
                btnTiposEquipo,
                btnLaboratorios,
                btnSolicitantes,
                btnReportes,
                btnUsuarios,
                btnAuditoria,
                btnCerrarSesion,
                btnSalir
        };

        for (JButton boton : botones) {

            configurarBotonMenu(
                    boton,
                    lateral,
                    texto
            );
        }

        btnSalir.setForeground(
                oscuro
                        ? new Color(219, 158, 158)
                        : new Color(156, 70, 70)
        );

        // TÍTULOS

        lblTitulo.setFont(
                fuente(Font.BOLD, 28)
        );

        lblTitulo.setForeground(texto);

        lblUsuarioActual.setFont(
                fuente(Font.PLAIN, 12)
        );

        lblUsuarioActual.setForeground(
                secundario
        );

        lblTituloResumen.setFont(
                fuente(Font.BOLD, 12)
        );

        // Contraste corregido
        lblTituloResumen.setForeground(
                texto
        );

        lblTituloInventario.setFont(
                fuente(Font.BOLD, 18)
        );

        lblTituloInventario.setForeground(
                texto
        );

        lblDescripcionInventario.setFont(
                fuente(Font.PLAIN, 12)
        );

        lblDescripcionInventario.setForeground(
                secundario
        );

        lblTituloSecundarios.setFont(
                fuente(Font.BOLD, 16)
        );

        lblTituloSecundarios.setForeground(
                texto
        );

        // VALORES NUMÉRICOS

        JLabel[] valores = {
                lblTotalEquipos,
                lblDisponibles,
                lblPrestados,
                lblMantenimiento,
                lblDanados,
                lblBaja,
                lblPrestamosActivos,
                lblPrestamosVencidos
        };

        for (JLabel valor : valores) {

            valor.setFont(
                    fuente(Font.BOLD, 30)
            );

            valor.setForeground(texto);
        }

        // BOTÓN ACTUALIZAR

        btnActualizarResumen.setOpaque(true);

        btnActualizarResumen.setBackground(
                AZUL_FORMAL
        );

        btnActualizarResumen.setForeground(
                Color.WHITE
        );

        btnActualizarResumen.setFont(
                fuente(Font.BOLD, 12)
        );

        btnActualizarResumen.setFocusPainted(
                false
        );

        btnActualizarResumen.setBorder(
                new EmptyBorder(
                        9, 16, 9, 16
                )
        );

        // SOL Y LUNA

        configurarBotonTema(
                btnModoClaro,
                !oscuro,
                tarjeta,
                texto,
                borde
        );

        configurarBotonTema(
                btnModoOscuro,
                oscuro,
                tarjeta,
                texto,
                borde
        );

        // DISTRIBUCIÓN DEL INVENTARIO

        JProgressBar[] barras = {
                barDisponibles,
                barPrestados,
                barDanados,
                barMantenimiento,
                barBaja
        };

        for (JProgressBar barra : barras) {

            barra.setForeground(
                    AZUL_FORMAL
            );

            barra.setBackground(
                    oscuro
                            ? new Color(41, 56, 73)
                            : new Color(228, 235, 242)
            );

            barra.setStringPainted(true);

            barra.setFont(
                    fuente(Font.PLAIN, 11)
            );

            barra.setBorderPainted(false);

            barra.setPreferredSize(
                    new Dimension(250, 18)
            );
        }

        // Actualización visual

        panelPrincipal.revalidate();

        panelPrincipal.repaint();
    }

    // =====================================================
    // FUENTE
    // =====================================================

    private Font fuente(
            int estilo,
            int tamano
    ) {

        return new Font(
                "Segoe UI",
                estilo,
                tamano
        );
    }

    // =====================================================
    // TARJETAS
    // =====================================================

    private void configurarTarjeta(
            JPanel panel,
            Color fondo,
            Color borde
    ) {

        panel.setOpaque(true);

        panel.setBackground(fondo);

        panel.setBorder(
                new LineBorder(
                        borde,
                        1,
                        true
                )
        );
    }

    // =====================================================
    // BOTONES DE MENÚ
    // =====================================================

    private void configurarBotonMenu(
            JButton boton,
            Color fondo,
            Color texto
    ) {

        boton.setBackground(fondo);

        boton.setForeground(texto);

        boton.setFont(
                fuente(Font.PLAIN, 13)
        );

        boton.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        boton.setOpaque(true);

        boton.setContentAreaFilled(true);

        boton.setFocusPainted(false);

        boton.setBorder(
                new EmptyBorder(
                        8, 11, 8, 11
                )
        );

        boton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    // =====================================================
    // BOTONES DE TEMA
    // =====================================================

    private void configurarBotonTema(
            JButton boton,
            boolean activo,
            Color fondo,
            Color texto,
            Color borde
    ) {

        boton.setFont(
                new Font(
                        "Segoe UI Symbol",
                        Font.PLAIN,
                        21
                )
        );

        boton.setOpaque(true);

        boton.setFocusPainted(false);

        boton.setBackground(
                activo
                        ? AZUL_FORMAL
                        : fondo
        );

        boton.setForeground(
                activo
                        ? Color.WHITE
                        : texto
        );

        boton.setBorder(
                new LineBorder(
                        activo
                                ? AZUL_FORMAL
                                : borde,
                        1,
                        true
                )
        );

        boton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    // =====================================================
    // ETIQUETAS SECUNDARIAS
    // =====================================================

    private void aplicarTextoBase(
            Container contenedor,
            Color secundario
    ) {

        for (Component componente
                : contenedor.getComponents()) {

            if (componente instanceof JLabel) {

                JLabel etiqueta =
                        (JLabel) componente;

                etiqueta.setForeground(
                        secundario
                );

                etiqueta.setFont(
                        fuente(Font.PLAIN, 12)
                );
            }

            if (componente instanceof Container) {

                aplicarTextoBase(
                        (Container) componente,
                        secundario
                );
            }
        }
    }

    // =====================================================
    // DATOS DEL DASHBOARD
    // =====================================================

    private void cargarResumen() {

        Dialogos.ejecutar(
                ventana,
                () -> {

                    ResumenDashboard r =
                            dashboard.resumen();

                    // 4 TARJETAS PRINCIPALES

                    lblTotalEquipos.setText(
                            String.valueOf(
                                    r.totalEquipos()
                            )
                    );

                    lblDisponibles.setText(
                            String.valueOf(
                                    r.disponibles()
                            )
                    );

                    lblPrestados.setText(
                            String.valueOf(
                                    r.prestados()
                            )
                    );

                    lblMantenimiento.setText(
                            String.valueOf(
                                    r.enMantenimiento()
                            )
                    );

                    // 4 TARJETAS SECUNDARIAS

                    lblDanados.setText(
                            String.valueOf(
                                    r.danados()
                            )
                    );

                    lblBaja.setText(
                            String.valueOf(
                                    r.deBaja()
                            )
                    );

                    lblPrestamosActivos.setText(
                            String.valueOf(
                                    r.prestamosActivos()
                            )
                    );

                    lblPrestamosVencidos.setText(
                            String.valueOf(
                                    r.prestamosVencidos()
                            )
                    );

                    // BARRAS DE DISTRIBUCIÓN

                    actualizarDistribucion(r);
                }
        );
    }

    // =====================================================
    // DISTRIBUCIÓN DEL INVENTARIO
    // =====================================================

    private void actualizarDistribucion(
            ResumenDashboard r
    ) {

        int total =
                Math.max(
                        1,
                        r.totalEquipos()
                );

        actualizarBarra(
                barDisponibles,
                r.disponibles(),
                total
        );

        actualizarBarra(
                barPrestados,
                r.prestados(),
                total
        );

        actualizarBarra(
                barDanados,
                r.danados(),
                total
        );

        actualizarBarra(
                barMantenimiento,
                r.enMantenimiento(),
                total
        );

        actualizarBarra(
                barBaja,
                r.deBaja(),
                total
        );
    }

    private void actualizarBarra(
            JProgressBar barra,
            int valor,
            int total
    ) {

        barra.setMinimum(0);

        barra.setMaximum(total);

        barra.setValue(
                Math.max(
                        0,
                        Math.min(valor, total)
                )
        );

        barra.setString(
                valor + " equipo(s)"
        );
    }

    // =====================================================
    // NAVEGACIÓN
    // =====================================================

    private void abrir(
            Runnable mostrarFormulario
    ) {

        ventana.setVisible(false);

        mostrarFormulario.run();
    }

    // =====================================================
    // CERRAR SESIÓN
    // =====================================================

    private void cerrarSesion() {

        int respuesta =
                JOptionPane.showConfirmDialog(
                        ventana,
                        "¿Deseas cerrar la sesión actual?",
                        "Cerrar sesión",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        new LoginController().cerrarSesion();

        ventana.dispose();

        new Login().mostrar();
    }

    // =====================================================
    // SALIR
    // =====================================================

    private void salirSistema() {

        int respuesta =
                JOptionPane.showConfirmDialog(
                        ventana,
                        "¿Deseas salir de SIGELAB?",
                        "Salir del sistema",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (respuesta == JOptionPane.YES_OPTION) {

            System.exit(0);
        }
    }

    // =====================================================
    // MOSTRAR VENTANA
    // =====================================================

    public void mostrar() {

        EstiloFormulario.pantallaCompleta(ventana);

        ventana.setVisible(true);
    }
}