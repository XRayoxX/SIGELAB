package SIGELAB.view;

import SIGELAB.controller.UsuarioController;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;
import SIGELAB.util.Dialogos;
import SIGELAB.util.TablaUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class FrmUsuarios {

    // Componentes vinculados desde FrmUsuarios.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtId;
    private JTextField txtUsuario;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtCorreo;
    private JComboBox cmbRol;
    private JPasswordField pwdContrasena;
    private JPasswordField pwdConfirmarContrasena;
    private JComboBox cmbEstado;
    private JTextField txtRutaFotoRostro;
    private JButton btnSeleccionarFoto;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tblUsuarios;
    private JTextField Txt_Segundo_nombre;
    private JTextField TXT_segundoApellido;

    private final JFrame ventana;
    private final DefaultTableModel modeloTabla;
    private final UsuarioController controller = new UsuarioController();
    private List<Usuario> usuariosEnTabla = List.of();

    public FrmUsuarios(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Gestión de Usuarios");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        modeloTabla = TablaUtil.configurar(tblUsuarios, "ID", "Usuario", "Nombre", "Apellido", "Correo", "Rol", "Estado");
        pwdContrasena.setToolTipText("Al actualizar, déjela vacía para conservar la contraseña actual");

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

        btnNuevo.addActionListener(e -> limpiar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnSeleccionarFoto.addActionListener(e -> seleccionarFoto());
        btnGuardar.addActionListener(e -> ejecutar(() -> {
            Usuario u = controller.guardar(leerFormulario(), pwdContrasena.getPassword(), pwdConfirmarContrasena.getPassword());
            Dialogos.info(ventana, "Usuario " + u.getNombreUsuario() + " creado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        }));
        btnActualizar.addActionListener(e -> ejecutar(() -> {
            controller.actualizar(leerFormulario(), pwdContrasena.getPassword(), pwdConfirmarContrasena.getPassword());
            Dialogos.info(ventana, "Usuario actualizado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        }));
        btnDesactivar.addActionListener(e -> {
            int id = idSeleccionado();
            if (id > 0 && !Dialogos.confirmar(ventana, "¿Desea desactivar el usuario seleccionado?")) {
                return;
            }
            ejecutar(() -> {
                controller.desactivar(id);
                limpiar();
                mostrarEnTabla(controller.listar());
            });
        });
        btnBuscar.addActionListener(e -> ejecutar(() -> mostrarEnTabla(controller.buscar(txtBuscar.getText()))));
        txtBuscar.addActionListener(e -> btnBuscar.doClick());
        tblUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionado();
            }
        });

        ejecutar(() -> mostrarEnTabla(controller.listar()));
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    // Imagen de referencia que usará el servicio de reconocimiento facial
    private void seleccionarFoto() {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new FileNameExtensionFilter("Imágenes (jpg, png)", "jpg", "jpeg", "png"));
        if (selector.showOpenDialog(ventana) == JFileChooser.APPROVE_OPTION) {
            txtRutaFotoRostro.setText(selector.getSelectedFile().getAbsolutePath());
        }
    }

    private Usuario leerFormulario() {
        Usuario u = new Usuario();
        u.setIdUsuario(idSeleccionado());
        u.setNombreUsuario(txtUsuario.getText());
        u.setNombre(txtNombre.getText());
        u.setApellido(txtApellido.getText());
        u.setCorreo(txtCorreo.getText());
        u.setRol(RolUsuario.valueOf(String.valueOf(cmbRol.getSelectedItem())));
        u.setEstado(EstadoRegistro.valueOf(String.valueOf(cmbEstado.getSelectedItem())));
        u.setRutaFotoRostro(txtRutaFotoRostro.getText());
        return u;
    }

    private int idSeleccionado() {
        return txtId.getText().isBlank() ? 0 : Integer.parseInt(txtId.getText());
    }

    private void mostrarEnTabla(List<Usuario> usuarios) {
        usuariosEnTabla = usuarios;
        modeloTabla.setRowCount(0);
        for (Usuario u : usuarios) {
            modeloTabla.addRow(new Object[]{u.getIdUsuario(), u.getNombreUsuario(), u.getNombre(), u.getApellido(),
                    u.getCorreo(), u.getRol(), u.getEstado()});
        }
    }

    private void cargarSeleccionado() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Usuario u = usuariosEnTabla.get(fila);
        txtId.setText(String.valueOf(u.getIdUsuario()));
        txtUsuario.setText(u.getNombreUsuario());
        txtNombre.setText(u.getNombre());
        txtApellido.setText(u.getApellido());
        txtCorreo.setText(u.getCorreo());
        cmbRol.setSelectedItem(u.getRol().name());
        cmbEstado.setSelectedItem(u.getEstado().name());
        txtRutaFotoRostro.setText(u.getRutaFotoRostro() == null ? "" : u.getRutaFotoRostro());
        pwdContrasena.setText("");            // la contraseña nunca se muestra
        pwdConfirmarContrasena.setText("");
    }

    private void limpiar() {
        tblUsuarios.clearSelection();
        txtId.setText("");
        txtUsuario.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtCorreo.setText("");
        txtRutaFotoRostro.setText("");
        pwdContrasena.setText("");
        pwdConfirmarContrasena.setText("");
        cmbRol.setSelectedItem(RolUsuario.ENCARGADO.name());
        cmbEstado.setSelectedItem(EstadoRegistro.ACTIVO.name());
        txtUsuario.requestFocus();
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
