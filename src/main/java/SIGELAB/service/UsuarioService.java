package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;
import SIGELAB.repository.UsuarioRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Seguridad;
import SIGELAB.util.Sesion;

import java.util.List;
import java.util.Optional;

/**
 * Administración de usuarios (solo ADMIN). La contraseña se guarda como hash SHA-256.
 */
public class UsuarioService {

    private static final int LARGO_MINIMO_CONTRASENA = 6;

    private final UsuarioRepository repository = new UsuarioRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    public List<Usuario> listar() {
        Sesion.exigirRol(RolUsuario.ADMIN);
        return repository.listar();
    }

    public List<Usuario> buscar(String texto) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        return texto == null || texto.isBlank() ? repository.listar() : repository.buscar(texto.trim());
    }

    public Usuario registrar(Usuario u, String contrasena, String confirmacion) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        validarDatos(u);
        if (contrasena == null || contrasena.isEmpty()) {
            throw new BusinessException("La contraseña es obligatoria para un usuario nuevo.");
        }
        u.setContrasenaHash(hashValidado(contrasena, confirmacion));
        validarUnicos(u);
        u.setIdUsuario(repository.insertar(u));
        auditoria.registrar(AccionAuditoria.USUARIO_CREADO, "Usuario", u.getIdUsuario(),
                "Usuario " + u.getNombreUsuario() + " creado con rol " + u.getRol());
        return u;
    }

    // Si la contraseña se deja vacía, se conserva la actual
    public void actualizar(Usuario u, String contrasena, String confirmacion) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (u.getIdUsuario() <= 0) {
            throw new BusinessException("Seleccione en la tabla el usuario que desea actualizar.");
        }
        validarDatos(u);
        boolean cambiaContrasena = contrasena != null && !contrasena.isEmpty();
        u.setContrasenaHash(cambiaContrasena ? hashValidado(contrasena, confirmacion) : null);
        if (u.getIdUsuario() == Sesion.getIdUsuario() && u.getEstado() == EstadoRegistro.INACTIVO) {
            throw new BusinessException("No puede desactivar su propio usuario.");
        }
        validarUnicos(u);
        repository.actualizar(u);
        auditoria.registrar(u.getEstado() == EstadoRegistro.INACTIVO
                        ? AccionAuditoria.USUARIO_DESACTIVADO : AccionAuditoria.USUARIO_MODIFICADO,
                "Usuario", u.getIdUsuario(), "Usuario " + u.getNombreUsuario() + " actualizado"
                        + (cambiaContrasena ? " (con cambio de contraseña)" : ""));
    }

    // Desactivación lógica para conservar el historial (sección 6.2)
    public void desactivar(int idUsuario) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (idUsuario <= 0) {
            throw new BusinessException("Seleccione en la tabla el usuario que desea desactivar.");
        }
        if (idUsuario == Sesion.getIdUsuario()) {
            throw new BusinessException("No puede desactivar su propio usuario.");
        }
        repository.cambiarEstado(idUsuario, EstadoRegistro.INACTIVO);
        auditoria.registrar(AccionAuditoria.USUARIO_DESACTIVADO, "Usuario", idUsuario, "Usuario desactivado");
    }

    private void validarDatos(Usuario u) {
        u.setNombre(limpio(u.getNombre()));
        u.setApellido(limpio(u.getApellido()));
        u.setNombreUsuario(limpio(u.getNombreUsuario()));
        u.setCorreo(limpio(u.getCorreo()));
        u.setRutaFotoRostro(limpio(u.getRutaFotoRostro()));

        if (u.getNombreUsuario() == null) {
            throw new BusinessException("El nombre de usuario es obligatorio.");
        }
        if (!u.getNombreUsuario().matches("^[A-Za-z0-9._-]{3,30}$")) {
            throw new BusinessException("El nombre de usuario debe tener de 3 a 30 caracteres, sin espacios (letras, números, punto, guion).");
        }
        if (u.getNombre() == null || u.getApellido() == null) {
            throw new BusinessException("El nombre y el apellido son obligatorios.");
        }
        if (u.getCorreo() == null || !u.getCorreo().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("Ingrese un correo electrónico válido.");
        }
        if (u.getRol() == null) {
            throw new BusinessException("Seleccione un rol válido.");
        }
        if (u.getEstado() == null) {
            throw new BusinessException("Seleccione un estado válido.");
        }
    }

    private String hashValidado(String contrasena, String confirmacion) {
        if (contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            throw new BusinessException("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
        if (!contrasena.equals(confirmacion)) {
            throw new BusinessException("La contraseña y su confirmación no coinciden.");
        }
        return Seguridad.sha256(contrasena);
    }

    private void validarUnicos(Usuario u) {
        Optional<Usuario> mismoUsuario = repository.buscarPorNombreUsuario(u.getNombreUsuario());
        if (mismoUsuario.isPresent() && mismoUsuario.get().getIdUsuario() != u.getIdUsuario()) {
            throw new BusinessException("Ya existe un usuario con el nombre " + u.getNombreUsuario() + ".");
        }
        Optional<Usuario> mismoCorreo = repository.buscarPorCorreo(u.getCorreo());
        if (mismoCorreo.isPresent() && mismoCorreo.get().getIdUsuario() != u.getIdUsuario()) {
            throw new BusinessException("Ya existe un usuario con el correo " + u.getCorreo() + ".");
        }
    }

    private String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
