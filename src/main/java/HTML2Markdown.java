import java.net.URI;
import java.net.URL;

public class HTML2Markdown {
    public static void main(String[] args) throws Exception {
        URL url = URI.create("https://example.com").toURL();
        WebClient cliente = new WebClient();
        String html = cliente.descargarHTML(url);
        System.out.println("HTTP " + cliente.getCodigoRespuesta());
        System.out.println(html.substring(0, Math.min(300, html.length())));
    }
}