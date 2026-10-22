package SIGELAB.view;

import SIGELAB.controller.AuditoriaController;
import SIGELAB.model.Auditoria;
import SIGELAB.util.Dialogos;
import SIGELAB.util.ExportarUtil;
import SIGELAB.util.Fechas;
import SIGELAB.util.TablaUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

public class FrmAuditoria {

    // Componentes vinculados desde FrmAuditoria.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JComboBox cmbUsuario;
    private JComboBox cmbEntidad;
    private JComboBox cmbAccion;
    private JTextField txtFechaDesde;
    private JTextField txtFechaHasta;
    private JButton btnBuscar;
    private JButton btnLimpiar;
    private JButton btnExportar;
    private JButton btnRegresar;
    private JTable tblAuditoria;
    private JLabel lblTotalRegistros;

    private final JFrame ventana;
    private final DefaultTableModel modeloTabla;
    private final AuditoriaController controller = new AuditoriaController();

    public FrmAuditoria(JFrame menuPrincipal) {
        ventana = new JFrame("Auditoría del Sistema ");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        modeloTabla = TablaUtil.configurar(tblAuditoria, "ID", "Fecha/Hora", "Usuario", "Acción", "Entidad", "ID registro", "Descripción");

        ventana.pack();
        ventana.setLocationRelativeTo(null);

        // Al cerrar esta ventana (con Regresar o con la X) se vuelve a mostrar el menú
        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                menuPrincipal.setVisible(true);
            }
        });
        btnRegresar.addActionListener(e -> ventana.dispose());

        btnBuscar.addActionListener(e -> ejecutar(this::buscar));
        btnLimpiar.addActionListener(e -> ejecutar(() -> {
            cmbUsuario.setSelectedIndex(0);
            cmbEntidad.setSelectedIndex(0);
            cmbAccion.setSelectedIndex(0);
            txtFechaDesde.setText("");
            txtFechaHasta.setText("");
            buscar();
        }));
        btnExportar.addActionListener(e -> ejecutar(() -> {
            File archivo = ExportarUtil.exportarCsv(ventana, tblAuditoria, "auditoria-sigelab");
            if (archivo != null) {
                Dialogos.info(ventana, "Auditoría exportada en " + archivo.getAbsolutePath());
            }
        }));

        ejecutar(() -> {
            DefaultComboBoxModel<Object> usuarios = new DefaultComboBoxModel<>();
            usuarios.addElement("TODOS");
            controller.usuarios().forEach(u -> usuarios.addElement(u.getNombreUsuario()));
            cmbUsuario.setModel(usuarios);
            buscar();
        });
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    private void buscar() {
        List<Auditoria> registros = controller.consultar(cmbUsuario.getSelectedItem(), cmbEntidad.getSelectedItem(),
                cmbAccion.getSelectedItem(), txtFechaDesde.getText(), txtFechaHasta.getText());
        modeloTabla.setRowCount(0);
        for (Auditoria a : registros) {
            modeloTabla.addRow(new Object[]{a.getIdAuditoria(), Fechas.formatear(a.getFechaHora()), a.getNombreUsuario(),
                    a.getAccion(), a.getEntidad(), a.getIdRegistro(), a.getDescripcion()});
        }
        lblTotalRegistros.setText("Total de registros: " + registros.size());
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
