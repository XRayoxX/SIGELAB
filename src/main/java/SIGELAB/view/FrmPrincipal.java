
package SIGELAB.view;

import SIGELAB.controller.DashboardController;
import SIGELAB.controller.LoginController;

import SIGELAB.enums.RolUsuario;

import SIGELAB.model.ResumenDashboard;
import SIGELAB.model.Usuario;

import SIGELAB.util.Dialogos;
import SIGELAB.util.Sesion;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class FrmPrincipal {

    // ==========================================
    // COMPONENTES DEL FORMULARIO
    // ==========================================

    private JPanel panelPrincipal;
    private JPanel panelNavegacion;
    private JPanel panelContenido;
    private JPanel panelResumen;

    private JLabel lblMarca;
    private JLabel lblTitulo;
    private JLabel lblUsuarioActual;

    // ==========================================
    // INDICADORES DEL DASHBOARD
    // ==========================================

    private JLabel lblTotalEquipos;
    private JLabel lblDisponibles;
    private JLabel lblPrestados;
    private JLabel lblDanados;

    private JLabel lblMantenimiento;
    private JLabel lblBaja;

    private JLabel lblPrestamosActivos;
    private JLabel lblPrestamosVencidos;

    // ==========================================
    // BOTONES DE NAVEGACIÓN
    // ==========================================

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

    // ==========================================
    // BOTONES GENERALES
    // ==========================================

    private JButton btnActualizarResumen;
    private JButton btnCerrarSesion;
    private JButton btnSalir;

    private JCheckBox chkModoOscuro;

    // ==========================================
    // CONTROLADORES
    // ==========================================

    private final JFrame ventana;

    private final DashboardController dashboard =
            new DashboardController();

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public FrmPrincipal() {

        ventana = new JFrame(
                "SIGELAB - Panel principal"
        );

        ventana.setContentPane(
                panelPrincipal
        );

        ventana.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        // --------------------------------------
        // USUARIO ACTUAL
        // --------------------------------------

        Usuario usuario =
                Sesion.getUsuarioActual();

        lblUsuarioActual.setText(
                "Usuario: "
                        + usuario.getNombreCompleto()
                        + " ("
                        + usuario.getNombreUsuario()
                        + ") - Rol: "
                        + usuario.getRol()
        );

        // --------------------------------------
        // PERMISOS POR ROL
        // --------------------------------------

        adaptarMenuAlRol();

        // --------------------------------------
        // CONFIGURAR VENTANA
        // --------------------------------------

        ventana.pack();

        ventana.setExtendedState(
                JFrame.MAXIMIZED_BOTH
        );

        // --------------------------------------
        // EVENTOS
        // --------------------------------------

        configurarEventos();

        // --------------------------------------
        // TEMA GUARDADO
        // --------------------------------------

        chkModoOscuro.setSelected(
                TemaSIGELAB.esOscuro()
        );

        aplicarTema();

        // --------------------------------------
        // ACTUALIZACIÓN DEL DASHBOARD
        // --------------------------------------

        ventana.addComponentListener(
                new ComponentAdapter() {

                    @Override
                    public void componentShown(
                            ComponentEvent e
                    ) {

                        cargarResumen();
                    }
                }
        );
    }

    // ==========================================
    // CONFIGURAR EVENTOS
    // ==========================================

    private void configurarEventos() {

        btnActualizarResumen.addActionListener(
                e -> cargarResumen()
        );

        // --------------------------------------
        // NAVEGACIÓN
        // --------------------------------------

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

        btnMantenimiento.addActionListener(
                e -> abrir(
                        new FrmMantenimiento(ventana)::mostrar
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

        // --------------------------------------
        // CERRAR SESIÓN
        // --------------------------------------

        btnCerrarSesion.addActionListener(e -> {

            new LoginController().cerrarSesion();

            ventana.dispose();

            new Login().mostrar();
        });

        // --------------------------------------
        // SALIR
        // --------------------------------------

        btnSalir.addActionListener(
                e -> System.exit(0)
        );

        // --------------------------------------
        // CAMBIAR TEMA
        // --------------------------------------

        chkModoOscuro.addActionListener(e -> {

            TemaSIGELAB.establecerModoOscuro(
                    chkModoOscuro.isSelected()
            );

            aplicarTema();
        });
    }

    // ==========================================
    // APLICAR TEMA DARK TECH
    // ==========================================

    private void aplicarTema() {

        Color fondo =
                TemaSIGELAB.fondo();

        Color panel =
                TemaSIGELAB.panel();

        Color navegacion =
                TemaSIGELAB.navegacion();

        Color texto =
                TemaSIGELAB.texto();

        Color secundario =
                TemaSIGELAB.textoSecundario();

        Color primario =
                TemaSIGELAB.primario();

        Color acento =
                TemaSIGELAB.acento();

        Color borde =
                TemaSIGELAB.borde();

        // --------------------------------------
        // PANELES PRINCIPALES
        // --------------------------------------

        panelPrincipal.setBackground(fondo);

        panelNavegacion.setBackground(
                navegacion
        );

        panelContenido.setBackground(
                fondo
        );

        panelResumen.setBackground(
                panel
        );

        // --------------------------------------
        // ENCABEZADOS
        // --------------------------------------

        lblMarca.setForeground(
                Color.WHITE
        );

        lblTitulo.setForeground(
                texto
        );

        lblUsuarioActual.setForeground(
                secundario
        );

        // --------------------------------------
        // CHECKBOX TEMA
        // --------------------------------------

        chkModoOscuro.setForeground(
                texto
        );

        chkModoOscuro.setOpaque(false);

        // --------------------------------------
        // INDICADORES DEL DASHBOARD
        // --------------------------------------

        configurarIndicador(
                lblTotalEquipos,
                acento
        );

        configurarIndicador(
                lblDisponibles,
                TemaSIGELAB.exito()
        );

        configurarIndicador(
                lblPrestados,
                TemaSIGELAB.advertencia()
        );

        configurarIndicador(
                lblDanados,
                TemaSIGELAB.error()
        );

        configurarIndicador(
                lblMantenimiento,
                TemaSIGELAB.informacion()
        );

        configurarIndicador(
                lblBaja,
                secundario
        );

        configurarIndicador(
                lblPrestamosActivos,
                TemaSIGELAB.exito()
        );

        configurarIndicador(
                lblPrestamosVencidos,
                TemaSIGELAB.error()
        );

        // --------------------------------------
        // ETIQUETAS DEL RESUMEN
        // --------------------------------------

        for (Component componente :
                panelResumen.getComponents()) {

            if (componente instanceof JLabel etiqueta) {

                if (!esIndicador(etiqueta)) {

                    etiqueta.setForeground(
                            texto
                    );
                }
            }
        }

        // --------------------------------------
        // BOTONES DE NAVEGACIÓN
        // --------------------------------------

        JButton[] botonesNavegacion = {

                btnPrestamos,
                btnDevoluciones,
                btnEquipos,
                btnTiposEquipo,
                btnLaboratorios,
                btnSolicitantes,
                btnMantenimiento,
                btnReportes,
                btnUsuarios,
                btnAuditoria

        };

        for (JButton boton : botonesNavegacion) {

            configurarBotonNavegacion(
                    boton,
                    navegacion
            );
        }

        // --------------------------------------
        // BOTONES GENERALES
        // --------------------------------------

        configurarBotonPrimario(
                btnActualizarResumen,
                primario
        );

        configurarBotonNavegacion(
                btnCerrarSesion,
                navegacion
        );

        btnSalir.setBackground(
                new Color(153, 40, 55)
        );

        btnSalir.setForeground(
                Color.WHITE
        );

        btnSalir.setFocusPainted(false);

        btnSalir.setOpaque(true);

        // --------------------------------------
        // BORDE DEL DASHBOARD
        // --------------------------------------

        Border linea =
                BorderFactory.createLineBorder(
                        borde,
                        1
                );

        TitledBorder bordeTitulo =
                BorderFactory.createTitledBorder(
                        linea,
                        "Resumen general del sistema"
                );

        bordeTitulo.setTitleColor(
                texto
        );

        bordeTitulo.setTitleFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        panelResumen.setBorder(
                bordeTitulo
        );

        // --------------------------------------
        // ACTUALIZAR VENTANA
        // --------------------------------------

        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    // ==========================================
    // CONFIGURAR INDICADORES
    // ==========================================

    private void configurarIndicador(
            JLabel etiqueta,
            Color color
    ) {

        etiqueta.setForeground(
                color
        );

        etiqueta.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22
                )
        );
    }

    // ==========================================
    // IDENTIFICAR INDICADORES
    // ==========================================

    private boolean esIndicador(
            JLabel etiqueta
    ) {

        return etiqueta == lblTotalEquipos
                || etiqueta == lblDisponibles
                || etiqueta == lblPrestados
                || etiqueta == lblDanados
                || etiqueta == lblMantenimiento
                || etiqueta == lblBaja
                || etiqueta == lblPrestamosActivos
                || etiqueta == lblPrestamosVencidos;
    }

    // ==========================================
    // BOTONES DE NAVEGACIÓN
    // ==========================================

    private void configurarBotonNavegacion(
            JButton boton,
            Color fondo
    ) {

        boton.setBackground(
                fondo
        );

        boton.setForeground(
                Color.WHITE
        );

        boton.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        boton.setFocusPainted(
                false
        );

        boton.setOpaque(
                true
        );

        boton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    // ==========================================
    // BOTÓN PRIMARIO
    // ==========================================

    private void configurarBotonPrimario(
            JButton boton,
            Color color
    ) {

        boton.setBackground(
                color
        );

        boton.setForeground(
                Color.WHITE
        );

        boton.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        boton.setFocusPainted(
                false
        );

        boton.setOpaque(
                true
        );

        boton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    // ==========================================
    // PERMISOS POR ROL
    // ==========================================

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

        // ADMINISTRACIÓN
        btnUsuarios.setVisible(admin);
        btnAuditoria.setVisible(admin);
        btnTiposEquipo.setVisible(admin);
        btnLaboratorios.setVisible(admin);

        // OPERACIÓN
        btnPrestamos.setVisible(operador);
        btnDevoluciones.setVisible(operador);
        btnEquipos.setVisible(operador);
        btnSolicitantes.setVisible(operador);
        btnMantenimiento.setVisible(operador);

        // CONSULTAS
        btnReportes.setVisible(true);
    }

    // ==========================================
    // CARGAR DATOS DEL DASHBOARD
    // ==========================================

    private void cargarResumen() {

        Dialogos.ejecutar(ventana, () -> {

            ResumenDashboard r =
                    dashboard.resumen();

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

            lblDanados.setText(
                    String.valueOf(
                            r.danados()
                    )
            );

            lblMantenimiento.setText(
                    String.valueOf(
                            r.enMantenimiento()
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
        });
    }

    // ==========================================
    // ABRIR FORMULARIOS
    // ==========================================

    private void abrir(
            Runnable mostrarFormulario
    ) {

        ventana.setVisible(false);

        mostrarFormulario.run();
    }

    // ==========================================
    // MOSTRAR MENÚ PRINCIPAL
    // ==========================================

    public void mostrar() {

        ventana.setVisible(true);
    }
}
