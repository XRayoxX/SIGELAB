package SIGELAB.repository;

import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;
import SIGELAB.util.ConexionBD;
import SIGELAB.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL de la tabla dbo.Usuario. El JOIN con dbo.Rol trae el nombre del rol.
 */
public class UsuarioRepository {

    private static final String SELECT_USUARIO =
            "SELECT u.IdUsuario, u.Nombre, u.Apellido, u.NombreUsuario, u.ContrasenaHash, u.Correo, "
            + "u.IdRol, r.Nombre AS NombreRol, u.Estado, u.RutaFotoRostro "
            + "FROM dbo.Usuario u INNER JOIN dbo.Rol r ON r.IdRol = u.IdRol ";

    // El IdRol se obtiene del nombre del rol (ADMIN, ENCARGADO, DOCENTE) con una subconsulta
    private static final String ID_ROL = "(SELECT IdRol FROM dbo.Rol WHERE Nombre = ?)";

    public List<Usuario> listar() {
        return consultar(SELECT_USUARIO + "ORDER BY u.NombreUsuario");
    }

    public List<Usuario> buscar(String texto) {
        String patron = "%" + texto + "%";
        return consultar(SELECT_USUARIO + "WHERE u.NombreUsuario LIKE ? OR u.Nombre LIKE ? OR u.Apellido LIKE ? "
                + "ORDER BY u.NombreUsuario", patron, patron, patron);
    }

    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return primero(consultar(SELECT_USUARIO + "WHERE u.NombreUsuario = ?", nombreUsuario));
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return primero(consultar(SELECT_USUARIO + "WHERE u.Correo = ?", correo));
    }

    public int insertar(Usuario u) {
        String sql = "INSERT INTO dbo.Usuario (Nombre, Apellido, NombreUsuario, ContrasenaHash, Correo, IdRol, Estado, RutaFotoRostro) "
                + "VALUES (?, ?, ?, ?, ?, " + ID_ROL + ", ?, ?)";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getNombreUsuario());
            ps.setString(4, u.getContrasenaHash());
            ps.setString(5, u.getCorreo());
            ps.setString(6, u.getRol().name());
            ps.setString(7, u.getEstado().name());
            ps.setString(8, u.getRutaFotoRostro());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el usuario.", e);
        }
    }

    // Si contrasenaHash es null, se conserva la contraseña actual
    public void actualizar(Usuario u) {
        boolean cambiaContrasena = u.getContrasenaHash() != null;
        String sql = "UPDATE dbo.Usuario SET Nombre = ?, Apellido = ?, NombreUsuario = ?, Correo = ?, IdRol = " + ID_ROL
                + ", Estado = ?, RutaFotoRostro = ?" + (cambiaContrasena ? ", ContrasenaHash = ?" : "")
                + " WHERE IdUsuario = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getNombreUsuario());
            ps.setString(4, u.getCorreo());
            ps.setString(5, u.getRol().name());
            ps.setString(6, u.getEstado().name());
            ps.setString(7, u.getRutaFotoRostro());
            int i = 8;
            if (cambiaContrasena) {
                ps.setString(i++, u.getContrasenaHash());
            }
            ps.setInt(i, u.getIdUsuario());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el usuario.", e);
        }
    }

    public void cambiarEstado(int idUsuario, EstadoRegistro estado) {
        String sql = "UPDATE dbo.Usuario SET Estado = ? WHERE IdUsuario = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado del usuario.", e);
        }
    }

    private Optional<Usuario> primero(List<Usuario> lista) {
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private List<Usuario> consultar(String sql, String... parametros) {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            List<Usuario> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el usuario.", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("IdUsuario"));
        u.setNombre(rs.getString("Nombre"));
        u.setApellido(rs.getString("Apellido"));
        u.setNombreUsuario(rs.getString("NombreUsuario"));
        u.setContrasenaHash(rs.getString("ContrasenaHash"));
        u.setCorreo(rs.getString("Correo"));
        u.setIdRol(rs.getInt("IdRol"));
        u.setRol(RolUsuario.valueOf(rs.getString("NombreRol")));
        u.setEstado(EstadoRegistro.valueOf(rs.getString("Estado")));
        u.setRutaFotoRostro(rs.getString("RutaFotoRostro"));
        return u;
    }
}
