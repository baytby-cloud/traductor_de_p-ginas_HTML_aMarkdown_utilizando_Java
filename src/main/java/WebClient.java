import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebClient {

    private static final int TIMEOUT_CONEXION_MS = 10_000;
    private static final int TIMEOUT_LECTURA_MS = 15_000;
    private static final int MAX_BYTES = 5 * 1024 * 1024; // 5 MB

    private static final Pattern CHARSET =
            Pattern.compile("charset=\"?([^;\\s\"]+)\"?", Pattern.CASE_INSENSITIVE);

    private int codigoRespuesta;

    public int getCodigoRespuesta() {
        return codigoRespuesta;
    }

    public String descargarHTML(URL url) throws IOException {
        String protocolo = url.getProtocol();
        if (!protocolo.equals("http") && !protocolo.equals("https")) {
            throw new IOException("Protocolo no soportado: " + protocolo);
        }


        URLConnection conexion = url.openConnection();
        if (!(conexion instanceof HttpURLConnection http)) {
            throw new IOException("La conexión no es HTTP");
        }

        http.setConnectTimeout(TIMEOUT_CONEXION_MS);
        http.setReadTimeout(TIMEOUT_LECTURA_MS);
        http.setRequestMethod("GET");
        http.setRequestProperty("User-Agent", "HTML2Markdown/1.0");
        http.setRequestProperty("Accept", "text/html");
        http.setInstanceFollowRedirects(true);

        try {
            codigoRespuesta = http.getResponseCode();
            if (codigoRespuesta < 200 || codigoRespuesta >= 300) {
                throw new IOException("Respuesta HTTP no exitosa: " + codigoRespuesta);
            }

            String tipo = http.getContentType();
            if (tipo == null || !tipo.toLowerCase().contains("text/html")) {
                throw new IOException("El contenido no es HTML. Content-Type: " + tipo);
            }

            Charset charset = detectarCharset(tipo);

            try (InputStream in = http.getInputStream()) {
                return new String(leerLimitado(in), charset);
            }
        } finally {
            http.disconnect();
        }
    }


    private Charset detectarCharset(String contentType) {
        Matcher m = CHARSET.matcher(contentType);
        if (m.find()) {
            try {
                return Charset.forName(m.group(1));
            } catch (Exception e) {
                // charset desconocido: usar UTF-8
            }
        }
        return StandardCharsets.UTF_8;
    }

    private byte[] leerLimitado(InputStream in) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int leidos;
        while ((leidos = in.read(buffer)) != -1) {
            total += leidos;
            if (total > MAX_BYTES) {
                throw new IOException("El documento excede el límite de " + MAX_BYTES + " bytes");
            }
            salida.write(buffer, 0, leidos);
        }
        return salida.toByteArray();
    }
}