import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class HTML2Markdown {

    public static void main(String[] args) {

        if (args.length < 1 || args.length > 2) {
            mostrarUso();
            return;
        }

        URL url;
        try {
            url = URI.create(args[0]).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            System.err.println("Error: URL inválida -> " + args[0]);
            return;
        }

        String protocolo = url.getProtocol();
        if (!protocolo.equals("http") && !protocolo.equals("https")) {
            System.err.println("Error: solo se admiten URL http o https.");
            return;
        }

        Path destino = (args.length == 2)

                ? Path.of(args[1])
                : Path.of(nombreSalidaPorDefecto(url));

        System.out.println("HTML2Markdown");
        System.out.println("--------------------------------");
        System.out.println("URL: " + url);

        try {
            System.out.println("Conectando con el servidor...");
            WebClient cliente = new WebClient();
            String html = cliente.descargarHTML(url);
            System.out.println("Respuesta HTTP: " + cliente.getCodigoRespuesta());
            System.out.println("HTML descargado: " + html.length() + " caracteres");

            String contenido = html;

            MarkdownWriter escritor = new MarkdownWriter();
            escritor.guardar(destino, contenido);

            System.out.println("Archivo generado: " + destino);
            System.out.println("Tamaño: " + escritor.tamano(destino) + " bytes");
            System.out.println("Conversión finalizada.");


        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void mostrarUso() {
        System.out.println("Uso: java HTML2Markdown <url> [archivo_salida.md]");
        System.out.println("Ejemplo: java HTML2Markdown https://ejemplo.org/articulo.html salida.md");
    }

    private static String nombreSalidaPorDefecto(URL url) {
        String ruta = url.getPath();
        if (ruta == null || ruta.isEmpty() || ruta.equals("/")) {

            return "index.md";
        }
        if (ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        String nombre = ruta.substring(ruta.lastIndexOf('/') + 1);
        int punto = nombre.lastIndexOf('.');
        if (punto > 0) {
            nombre = nombre.substring(0, punto);
        }
        return nombre.isEmpty() ? "index.md" : nombre + ".md";
    }
}