package SIGELAB.model;

import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;

/**
 * Representa una fila de dbo.Usuario junto con el nombre de su rol (dbo.Rol).
 */
public class Usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String contrasenaHash;
    private String correo;
    private int idRol;
    private RolUsuario rol;
    private EstadoRegistro estado;
    private String rutaFotoRostro;

    public Usuario() {
        this.estado = EstadoRegistro.ACTIVO;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public EstadoRegistro getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistro estado) {
        this.estado = estado;
    }

    public String getRutaFotoRostro() {
        return rutaFotoRostro;
    }

    public void setRutaFotoRostro(String rutaFotoRostro) {
        this.rutaFotoRostro = rutaFotoRostro;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Override
    public String toString() {
        return nombreUsuario;
    }
}
