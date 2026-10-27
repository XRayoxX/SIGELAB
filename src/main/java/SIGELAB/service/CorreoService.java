package SIGELAB.service;

import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.model.Equipo;
import SIGELAB.model.Prestamo;
import SIGELAB.model.Solicitante;
import SIGELAB.model.Usuario;
import SIGELAB.util.Configuracion;
import SIGELAB.util.Fechas;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

/**
 * Notificaciones por correo (Gmail) de SIGELAB:
 *  - Intento fallido de inicio de sesión por reconocimiento facial (al usuario).
 *  - Préstamo registrado (al solicitante).
 *  - Equipo devuelto (al solicitante).
 *
 * Los datos de la cuenta se leen de config.properties:
 *
 *   correo.habilitado=true
 *   correo.remitente=sigelab03@gmail.com
 *   correo.contrasena=CONTRASENA_DE_APLICACION
 *
 * El envío se hace en un hilo aparte: la ventana no se congela mientras Gmail responde
 * y, si el correo falla (sin internet, correo mal escrito), el préstamo o la devolución
 * igual quedan guardados.
 */
public class CorreoService {

    private static final String SERVIDOR = "smtp.gmail.com";
    private static final String PUERTO = "587";

    // ========================================
    // NOTIFICACIONES
    // ========================================

    public void notificarFalloFacial(Usuario usuario) {
        String cuerpo = "<p>Hola <b>" + html(usuario.getNombre()) + "</b>,</p>"
                + "<p>Se intentó iniciar sesión en SIGELAB con su usuario <b>" + html(usuario.getNombreUsuario())
                + "</b>, pero el <b>reconocimiento facial no fue aprobado</b> (el rostro no coincide o no pasó la prueba de vida).</p>"
                + "<p><b>Fecha y hora:</b> " + Fechas.formatear(LocalDateTime.now()) + "</p>"
                + "<p>Si fue usted, puede ignorar este mensaje. Si no fue usted, alguien conoce su contraseña: "
                + "cámbiela y comuníquese con el administrador.</p>";
        enviar(usuario.getCorreo(), "SIGELAB - Intento de inicio de sesión no reconocido", cuerpo);
    }

    public void notificarPrestamo(int idPrestamo, Prestamo prestamo, List<Equipo> equipos) {
        Solicitante s = prestamo.getSolicitante();
        StringBuilder filas = new StringBuilder();
        for (Equipo e : equipos) {
            filas.append("<tr><td>").append(html(e.getCodigoInventario())).append("</td><td>")
                    .append(html(e.getNombre())).append("</td></tr>");
        }
        String cuerpo = "<p>Hola <b>" + html(s.getNombreCompleto()) + "</b>,</p>"
                + "<p>Se registró a su nombre el <b>préstamo No. " + idPrestamo + "</b> con los siguientes equipos:</p>"
                + tabla(filas.toString())
                + "<p><b>Fecha del préstamo:</b> " + Fechas.formatear(LocalDateTime.now()) + "<br>"
                + "<b>Fecha esperada de devolución:</b> " + Fechas.formatear(prestamo.getFechaEsperadaDevolucion()) + "</p>"
                + "<p>Recuerde devolver los equipos a tiempo y en buen estado.</p>";
        enviar(s.getCorreo(), "SIGELAB - Préstamo No. " + idPrestamo + " registrado", cuerpo);
    }

    public void notificarDevolucion(Solicitante s, int idPrestamo, Equipo equipo,
                                    CondicionDevolucion condicion, int pendientes) {
        String cuerpo = "<p>Hola <b>" + html(s.getNombreCompleto()) + "</b>,</p>"
                + "<p>Se registró la devolución del siguiente equipo del <b>préstamo No. " + idPrestamo + "</b>:</p>"
                + tabla("<tr><td>" + html(equipo.getCodigoInventario()) + "</td><td>" + html(equipo.getNombre()) + "</td></tr>")
                + "<p><b>Condición:</b> " + condicion + "<br>"
                + "<b>Fecha de devolución:</b> " + Fechas.formatear(LocalDateTime.now()) + "</p>"
                + (pendientes == 0
                ? "<p>Ya entregó <b>todos</b> los equipos: el préstamo quedó <b>FINALIZADO</b>. ¡Gracias!</p>"
                : "<p>Todavía tiene <b>" + pendientes + "</b> equipo(s) pendiente(s) de devolver en este préstamo.</p>");
        enviar(s.getCorreo(), "SIGELAB - Equipo " + equipo.getCodigoInventario() + " entregado", cuerpo);
    }

    // ========================================
    // ENVIO
    // ========================================

    /** false si se desactivó en config.properties (correo.habilitado=false) o faltan los datos de la cuenta. */
    public static boolean habilitado() {
        return Boolean.parseBoolean(Configuracion.valor("correo.habilitado", "true"))
                && Configuracion.valor("correo.remitente") != null
                && Configuracion.valor("correo.contrasena") != null;
    }

    private void enviar(String destinatario, String asunto, String cuerpoHtml) {
        // El solicitante puede no tener correo (la columna admite NULL)
        if (!habilitado() || destinatario == null || destinatario.isBlank()) {
            return;
        }
        // Hilo NO daemon: si se cierra la aplicación, el correo termina de enviarse primero
        Thread hilo = new Thread(() -> {
            try {
                enviarAhora(destinatario.trim(), asunto, cuerpoHtml);
            } catch (MessagingException | UnsupportedEncodingException e) {
                System.err.println("No se pudo enviar el correo a " + destinatario + ": " + e.getMessage());
            }
        }, "enviar-correo");
        hilo.start();
    }

    private void enviarAhora(String destinatario, String asunto, String cuerpoHtml)
            throws MessagingException, UnsupportedEncodingException {
        String remitente = Configuracion.valor("correo.remitente");
        // Google muestra la contraseña de aplicación con espacios ("abcd efgh ..."); se quitan
        String contrasena = Configuracion.valor("correo.contrasena").replace(" ", "");

        Properties p = new Properties();
        p.put("mail.smtp.host", SERVIDOR);
        p.put("mail.smtp.port", PUERTO);
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.starttls.enable", "true"); // conexión cifrada
        p.put("mail.smtp.connectiontimeout", "15000");
        p.put("mail.smtp.timeout", "15000");

        Session sesion = Session.getInstance(p, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remitente, contrasena);
            }
        });

        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(remitente, "SIGELAB", "UTF-8"));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto, "UTF-8");
        mensaje.setContent(plantilla(cuerpoHtml), "text/html; charset=UTF-8");
        Transport.send(mensaje);
    }

    // ========================================
    // FORMATO HTML
    // ========================================

    private String plantilla(String cuerpo) {
        return "<div style='font-family:Arial,sans-serif;font-size:14px;color:#222;max-width:600px'>"
                + "<h2 style='color:#1f4e79;margin-bottom:4px'>SIGELAB</h2>"
                + "<p style='color:#666;margin-top:0'>Sistema de Gestión de Laboratorios</p>"
                + cuerpo
                + "<hr><p style='color:#888;font-size:12px'>Este es un mensaje automático, por favor no responda.</p>"
                + "</div>";
    }

    private String tabla(String filas) {
        return "<table border='1' cellpadding='6' style='border-collapse:collapse'>"
                + "<tr style='background:#1f4e79;color:#fff'><th>Código</th><th>Equipo</th></tr>"
                + filas + "</table>";
    }

    // Evita que un nombre con < o > rompa el HTML del correo
    private String html(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
