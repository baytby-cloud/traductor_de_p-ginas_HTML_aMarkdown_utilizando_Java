import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlParser {

    public enum Tipo { INICIO, FIN, VACIA, TEXTO }

    /** Un evento del documento: etiqueta o texto. */
    public static class Token {
        public final Tipo tipo;
        public final String nombre;
        public final String texto;
        public final Map<String, String> atributos;

        Token(Tipo tipo, String nombre, String texto, Map<String, String> atributos) {
            this.tipo = tipo;
            this.nombre = nombre;
            this.texto = texto;
            this.atributos = atributos;
        }

        @Override
        public String toString() {
            return switch (tipo) {

                case TEXTO -> "TEXTO[" + texto + "]";
                case INICIO -> "<" + nombre + (atributos.isEmpty() ? "" : " " + atributos) + ">";
                case FIN -> "</" + nombre + ">";
                case VACIA -> "<" + nombre + (atributos.isEmpty() ? "" : " " + atributos) + "/>";
            };
        }
    }

    public List<Token> analizar(String html) throws IOException {
        List<Token> tokens = new ArrayList<>();

        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            @Override
            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                tokens.add(new Token(Tipo.INICIO, t.toString(), null, copiar(a)));
            }

            @Override
            public void handleEndTag(HTML.Tag t, int pos) {
                tokens.add(new Token(Tipo.FIN, t.toString(), null, Map.of()));
            }

            @Override
            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                tokens.add(new Token(Tipo.VACIA, t.toString(), null, copiar(a)));
            }

            @Override
            public void handleText(char[] data, int pos) {
                tokens.add(new Token(Tipo.TEXTO, null, new String(data), Map.of()));
            }
        };


        new ParserDelegator().parse(new StringReader(html), callback, true);
        return tokens;
    }

    private static Map<String, String> copiar(MutableAttributeSet a) {
        Map<String, String> mapa = new HashMap<>();
        var nombres = a.getAttributeNames();
        while (nombres.hasMoreElements()) {
            Object clave = nombres.nextElement();
            mapa.put(clave.toString().toLowerCase(), String.valueOf(a.getAttribute(clave)));
        }
        return mapa;
    }
}