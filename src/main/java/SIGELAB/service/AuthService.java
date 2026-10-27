package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.model.Usuario;
import SIGELAB.repository.AuditoriaRepository;
import SIGELAB.repository.UsuarioRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Seguridad;
import SIGELAB.util.Sesion;

import java.util.Optional;

/**
 * Reglas del inicio de sesión.
 * Paso 1: usuario + contraseña + estado ACTIVO (RN-01).
 * Paso 2: validación facial (RN-02) -> API de reconocimiento facial en Azure (ver SIGELAB.facial).
 */
public class AuthService {

    private static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();
    private final CorreoService correoService = new CorreoService();

    public Usuario validarCredenciales(String nombreUsuario, String contrasena) {
        String usuarioEscrito = nombreUsuario == null ? "" : nombreUsuario.trim();
        if (usuarioEscrito.isEmpty() || contrasena == null || contrasena.isEmpty()) {
            throw new BusinessException("Ingrese su usuario y su contraseña.");
        }

        Optional<Usuario> encontrado = usuarioRepository.buscarPorNombreUsuario(usuarioEscrito);
        if (encontrado.isEmpty()) {
            auditoriaRepository.registrar(null, recortar(usuarioEscrito), AccionAuditoria.LOGIN_FALLIDO, "Usuario", null,
                    "Usuario no registrado");
            // No se dice si el usuario existe o no, para no dar pistas a un atacante
            throw new BusinessException(MENSAJE_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();
        if (!usuario.getContrasenaHash().equalsIgnoreCase(Seguridad.sha256(contrasena))) {
            auditoriaRepository.registrar(usuario.getIdUsuario(), usuario.getNombreUsuario(), AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario", usuario.getIdUsuario(), "Contraseña incorrecta");
            throw new BusinessException(MENSAJE_CREDENCIALES);
        }

        // RN-01: un usuario INACTIVO no puede iniciar sesión aunque su contraseña sea correcta
        if (usuario.getEstado() != EstadoRegistro.ACTIVO) {
            auditoriaRepository.registrar(usuario.getIdUsuario(), usuario.getNombreUsuario(), AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario", usuario.getIdUsuario(), "Intento de ingreso con usuario INACTIVO");
            throw new BusinessException("El usuario " + usuario.getNombreUsuario()
                    + " está INACTIVO. Comuníquese con el administrador.");
        }
        return usuario;
    }

    // Se llama solo cuando las credenciales y la validación facial fueron aprobadas
    public void completarLogin(Usuario usuario) {
        Sesion.iniciar(usuario);
        auditoriaRepository.registrar(usuario.getIdUsuario(), usuario.getNombreUsuario(), AccionAuditoria.LOGIN_EXITOSO,
                "Usuario", usuario.getIdUsuario(), "Inicio de sesión correcto");
    }

    // RN-02: la contraseña era correcta pero el rostro no coincidió (o no pasó la prueba de vida)
    public void registrarFalloFacial(Usuario usuario, String detalle) {
        auditoriaRepository.registrar(usuario.getIdUsuario(), usuario.getNombreUsuario(), AccionAuditoria.LOGIN_FACIAL_FALLIDO,
                "Usuario", usuario.getIdUsuario(), detalle);
        // Se avisa al dueño de la cuenta por si no fue él quien intentó ingresar
        correoService.notificarFalloFacial(usuario);
    }

    public void cerrarSesion() {
        Sesion.cerrar();
    }

    // La columna Auditoria.NombreUsuario admite máximo 30 caracteres
    private String recortar(String texto) {
        return texto.length() > 30 ? texto.substring(0, 30) : texto;
    }
}
