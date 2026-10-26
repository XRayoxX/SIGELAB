package SIGELAB.facial;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Cliente de la SIGELAB Facial API (Azure Container Apps).
 *
 * <p>No necesita librerías externas: usa java.net.http (Java 11 o superior).
 * Todas las llamadas son bloqueantes; desde Swing llámelas dentro de un
 * SwingWorker para no congelar la interfaz.</p>
 *
 * <pre>
 * FacialApiClient api = new FacialApiClient(
 *         "https://sigelab-facial-api.xxxx.eastus.azurecontainerapps.io",
 *         "SU_API_KEY", "sigelab-desktop");
 * FacialApiClient.Desafio d = api.desafio(idUsuario);
 * // ... capturar fotos de frente y girando según d.instruccion ...
 * FacialApiClient.Respuesta r = api.verificar(idUsuario, d.token, fotos);
 * if (r.esExitosa() &amp;&amp; r.getBoolean("matched")) { ... }
 * </pre>
 */
public class FacialApiClient {

    /** Lado mayor de cada foto enviada (px). Fotos más pequeñas = subida más rápida. */
    private static final int MAX_LADO = 960;

    private final String baseUrl;
    private final String apiKey;
    private final String nombreCliente;
    private final HttpClient http;

    public FacialApiClient(String baseUrl, String apiKey, String nombreCliente) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.nombreCliente = nombreCliente;
        // HTTP/1.1: evita el "upgrade" a HTTP/2 que algunos servidores rechazan.
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // ------------------------------------------------------------ endpoints

    /** Despierta el servicio (si Azure lo tenía apagado) y devuelve su estado. Llámelo al abrir la app. */
    public Respuesta salud() throws IOException, InterruptedException {
        return enviar(peticion("/health").GET().build());
    }

    /** Pide un desafío aleatorio de prueba de vida (girar a la izquierda o a la derecha). */
    public Desafio desafio(Integer idUsuario) throws IOException, InterruptedException {
        String ruta = "/api/face/challenge" + (idUsuario != null ? "?userId=" + idUsuario : "");
        Respuesta r = enviar(peticion(ruta).GET().build());
        if (!r.esExitosa()) {
            throw new IOException(r.mensaje());
        }
        return new Desafio(r.getString("challengeToken"), r.getString("challenge"), r.getString("instruction"));
    }

    /** Registra el rostro: envíe ~3 fotos de frente, 2 girando a la izquierda y 2 a la derecha. */
    public Respuesta registrar(int idUsuario, boolean reemplazar, List<BufferedImage> fotos)
            throws IOException, InterruptedException {
        Multipart m = new Multipart();
        m.campo("userId", String.valueOf(idUsuario));
        m.campo("replace", String.valueOf(reemplazar));
        m.campo("mirrored", "false");
        m.fotos(fotos);
        return enviar(m.post(peticion("/api/face/enroll")));
    }

    /** Verifica (1:1) que la persona frente a la cámara sea el usuario indicado. */
    public Respuesta verificar(int idUsuario, String tokenDesafio, List<BufferedImage> fotos)
            throws IOException, InterruptedException {
        Multipart m = new Multipart();
        m.campo("userId", String.valueOf(idUsuario));
        if (tokenDesafio != null) {
            m.campo("challengeToken", tokenDesafio);
        }
        m.campo("mirrored", "false");
        m.fotos(fotos);
        return enviar(m.post(peticion("/api/face/verify")));
    }

    /** Identifica (1:N) quién es la persona entre todos los usuarios registrados. */
    public Respuesta identificar(String tokenDesafio, List<BufferedImage> fotos)
            throws IOException, InterruptedException {
        Multipart m = new Multipart();
        if (tokenDesafio != null) {
            m.campo("challengeToken", tokenDesafio);
        }
        m.campo("mirrored", "false");
        m.fotos(fotos);
        return enviar(m.post(peticion("/api/face/identify")));
    }

    /** Revisa calidad y pose de las fotos sin guardar nada (útil para calibrar). */
    public Respuesta analizar(List<BufferedImage> fotos) throws IOException, InterruptedException {
        Multipart m = new Multipart();
        m.campo("mirrored", "false");
        m.fotos(fotos);
        return enviar(m.post(peticion("/api/face/analyze")));
    }

    /** ¿El usuario ya tiene rostro registrado? Use r.getBoolean("enrolled"). */
    public Respuesta estado(int idUsuario) throws IOException, InterruptedException {
        return enviar(peticion("/api/face/status/" + idUsuario).GET().build());
    }

    /** Elimina el perfil facial (solo un administrador). */
    public Respuesta eliminarPerfil(int idUsuario, int idAdministrador) throws IOException, InterruptedException {
        String ruta = "/api/face/profile/" + idUsuario + "?adminUserId="
                + URLEncoder.encode(String.valueOf(idAdministrador), StandardCharsets.UTF_8);
        return enviar(peticion(ruta).DELETE().build());
    }

    // ------------------------------------------------------------- internos

    private HttpRequest.Builder peticion(String ruta) {
        // 90 s: si Azure tenía la API apagada (escala a cero) el primer uso tarda en arrancar.
        return HttpRequest.newBuilder(URI.create(baseUrl + ruta))
                .timeout(Duration.ofSeconds(90))
                .header("X-API-Key", apiKey)
                .header("X-Client", nombreCliente)
                .header("Accept", "application/json");
    }

    private Respuesta enviar(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Respuesta(res.statusCode(), res.body());
    }

    static byte[] aJpeg(BufferedImage original) throws IOException {
        BufferedImage img = original;
        int lado = Math.max(img.getWidth(), img.getHeight());
        if (lado > MAX_LADO || img.getType() != BufferedImage.TYPE_INT_RGB) {
            double escala = Math.min(1.0, (double) MAX_LADO / lado);
            int w = Math.max(1, (int) Math.round(img.getWidth() * escala));
            int h = Math.max(1, (int) Math.round(img.getHeight() * escala));
            BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            img = rgb;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    /** Arma un cuerpo multipart/form-data con campos de texto y fotos JPEG. */
    private static final class Multipart {
        private final String limite = "----SigelabFacial" + UUID.randomUUID().toString().replace("-", "");
        private final ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();

        void campo(String nombre, String valor) throws IOException {
            escribir("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n" + valor + "\r\n");
        }

        void fotos(List<BufferedImage> fotos) throws IOException {
            int i = 0;
            for (BufferedImage foto : fotos) {
                escribir("--" + limite + "\r\nContent-Disposition: form-data; name=\"images\"; filename=\"foto" + (i++)
                        + ".jpg\"\r\nContent-Type: image/jpeg\r\n\r\n");
                cuerpo.write(aJpeg(foto));
                escribir("\r\n");
            }
        }

        HttpRequest post(HttpRequest.Builder builder) throws IOException {
            escribir("--" + limite + "--\r\n");
            return builder.header("Content-Type", "multipart/form-data; boundary=" + limite)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()))
                    .build();
        }

        private void escribir(String texto) throws IOException {
            cuerpo.write(texto.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ---------------------------------------------------------- resultados

    /** Desafío de prueba de vida emitido por la API. */
    public static final class Desafio {
        public final String token;
        /** "turn_left" o "turn_right". */
        public final String tipo;
        /** Texto para mostrar al usuario. */
        public final String instruccion;

        Desafio(String token, String tipo, String instruccion) {
            this.token = token;
            this.tipo = tipo;
            this.instruccion = instruccion;
        }

        public boolean esIzquierda() {
            return "turn_left".equals(tipo);
        }
    }

    /** Respuesta JSON de la API con utilidades simples para leer campos (sin librerías). */
    public static final class Respuesta {
        public final int status;
        public final String json;

        Respuesta(int status, String json) {
            this.status = status;
            this.json = json == null ? "" : json;
        }

        public boolean esExitosa() {
            return status >= 200 && status < 300;
        }

        /** Código de error de la API (ej. LIVENESS_FALLIDO, PERFIL_NO_REGISTRADO) o null. */
        public String codigo() {
            return getString("code");
        }

        /** Mensaje para mostrar al usuario. */
        public String mensaje() {
            String m = getString(esExitosa() ? "message" : "detail");
            if (m == null && status == 422 && json.contains("\"detail\":[")) {
                return "Datos inválidos enviados a la API.";
            }
            return m != null ? m : "Error HTTP " + status;
        }

        /** Motivos por los que se rechazaron fotos individuales ("Foto 2: Acérquese un poco a la cámara."). */
        public List<String> motivosFotos() {
            List<String> motivos = new ArrayList<>();
            Matcher m = Pattern.compile("\\{\"index\":(\\d+),\"faces\":\\d+,\"accepted\":false,\"reason\":\"((?:[^\"\\\\]|\\\\.)*)\"")
                    .matcher(json);
            while (m.find()) {
                motivos.add("Foto " + (Integer.parseInt(m.group(1)) + 1) + ": " + desescapar(m.group(2)));
            }
            return motivos;
        }

        public String getString(String clave) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(clave) + "\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
            return m.find() ? desescapar(m.group(1)) : null;
        }

        public boolean getBoolean(String clave) {
            return "true".equals(valorCrudo(clave));
        }

        public Double getDouble(String clave) {
            String v = valorCrudo(clave);
            return v == null || "null".equals(v) ? null : Double.valueOf(v);
        }

        public Integer getInt(String clave) {
            Double v = getDouble(clave);
            return v == null ? null : v.intValue();
        }

        private String valorCrudo(String clave) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(clave) + "\":(true|false|null|-?[0-9.eE+-]+)").matcher(json);
            return m.find() ? m.group(1) : null;
        }

        private static String desescapar(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < s.length()) {
                    char n = s.charAt(++i);
                    switch (n) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                            break;
                        default: sb.append(n);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return status + " " + json;
        }
    }
}
