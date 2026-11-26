import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MarkdownConverter {

    private final URI base;

    private StringBuilder salida;
    private int ignorar;
    private int enPre;
    private int enCita;
    private final Deque<int[]> listas = new ArrayDeque<>();
    private final Deque<String> enlaces = new ArrayDeque<>();
    private StringBuilder bufferEnlace;

    public MarkdownConverter(URI base) {
        this.base = base;
    }

    public String convertir(List<HtmlParser.Token> tokens) {
        salida = new StringBuilder();
        ignorar = 0;
        enPre = 0;
        enCita = 0;
        listas.clear();
        enlaces.clear();
        bufferEnlace = null;

        for (HtmlParser.Token t : tokens) {

            switch (t.tipo) {
                case INICIO -> inicio(t);
                case FIN -> fin(t);
                case VACIA -> vacia(t);
                case TEXTO -> texto(t.texto);
            }
        }
        return limpiar(salida.toString());
    }

    private void inicio(HtmlParser.Token t) {
        String n = t.nombre;
        if (n.equals("script") || n.equals("style") || n.equals("head")) {
            ignorar++;
            return;
        }
        if (ignorar > 0) return;

        switch (n) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                bloque();
                int nivel = n.charAt(1) - '0';
                emitir("#".repeat(nivel) + " ");
            }
            case "p", "div" -> bloque();
            case "strong", "b" -> emitir("**");
            case "em", "i" -> emitir("*");
            case "a" -> {
                enlaces.push(resolver(t.atributos.get("href")));
                bufferEnlace = new StringBuilder();

                bufferEnlace.append(salida.length());
                emitir("[");
            }
            case "ul" -> {
                if (listas.isEmpty()) bloque(); else asegurarSaltoLinea();
                listas.push(new int[]{0, 0});
            }
            case "ol" -> {
                if (listas.isEmpty()) bloque(); else asegurarSaltoLinea();
                listas.push(new int[]{1, 0});
            }
            case "li" -> {
                asegurarSaltoLinea();
                String sangria = "  ".repeat(Math.max(0, listas.size() - 1));
                if (!listas.isEmpty() && listas.peek()[0] == 1) {
                    listas.peek()[1]++;
                    emitir(sangria + listas.peek()[1] + ". ");
                } else {
                    emitir(sangria + "- ");
                }
            }
            case "blockquote" -> {
                bloque();
                enCita++;
                emitir("> ");
            }
            case "pre" -> {
                bloque();
                enPre++;
                emitir("```\n");
            }
            case "code" -> {

                if (enPre == 0) emitir("`");
            }
        }
    }

    private void fin(HtmlParser.Token t) {
        String n = t.nombre;
        if (n.equals("script") || n.equals("style") || n.equals("head")) {
            if (ignorar > 0) ignorar--;
            return;
        }
        if (ignorar > 0) return;

        switch (n) {
            case "h1", "h2", "h3", "h4", "h5", "h6", "p", "div" -> bloque();
            case "strong", "b" -> emitir("**");
            case "em", "i" -> emitir("*");
            case "a" -> {
                String href = enlaces.isEmpty() ? "" : enlaces.pop();
                bufferEnlace = null;
                emitir("](" + href + ")");
            }
            case "ul", "ol" -> {
                if (!listas.isEmpty()) listas.pop();
                if (listas.isEmpty()) bloque();
            }
            case "li" -> asegurarSaltoLinea();
            case "blockquote" -> {
                if (enCita > 0) enCita--;
                bloque();

            }
            case "pre" -> {
                if (enPre > 0) enPre--;
                asegurarSaltoLinea();
                emitir("```");
                bloque();
            }
            case "code" -> {
                if (enPre == 0) emitir("`");
            }
            default -> { }
        }
    }

    private void vacia(HtmlParser.Token t) {
        if (ignorar > 0) return;
        switch (t.nombre) {
            case "br" -> emitir("  \n");
            case "hr" -> {
                bloque();
                emitir("---");
                bloque();
            }
            case "img" -> {
                String alt = t.atributos.getOrDefault("alt", "");
                String src = resolver(t.atributos.get("src"));
                emitir("![" + escapar(alt) + "](" + src + ")");
            }
            default -> { }
        }
    }

    private void texto(String s) {
        if (ignorar > 0 || s == null) return;
        if (enPre > 0) {
            salida.append(s);
            return;
        }

        String limpio = s.replaceAll("\\s+", " ");
        if (limpio.isBlank()) {

            if (salida.length() > 0 && !terminaEnEspacioOSalto()) salida.append(' ');
            return;
        }

        if (limpio.startsWith(" ") && (salida.length() == 0 || terminaEnEspacioOSalto())) {
            limpio = limpio.stripLeading();
        }
        emitir(escapar(limpio));
    }

    private void emitir(String s) {
        if (enCita > 0 && salida.length() > 0 && salida.charAt(salida.length() - 1) == '\n'
                && !s.startsWith(">")) {
            salida.append("> ");
        }
        salida.append(s);
    }


    private void bloque() {
        if (salida.length() == 0) return;
        recortarEspaciosFinales();
        if (salida.length() == 0) return;
        if (!termina("\n\n")) {
            if (!termina("\n")) salida.append('\n');
            salida.append('\n');
        }
    }

    private void asegurarSaltoLinea() {
        if (salida.length() == 0) return;
        recortarEspaciosFinales();
        if (salida.length() > 0 && !termina("\n")) salida.append('\n');
    }

    private void recortarEspaciosFinales() {
        while (salida.length() > 0 && salida.charAt(salida.length() - 1) == ' ') {
            salida.setLength(salida.length() - 1);
        }
    }

    private boolean termina(String s) {
        int n = s.length();
        return salida.length() >= n && salida.substring(salida.length() - n).equals(s);
    }

    private boolean terminaEnEspacioOSalto() {
        char c = salida.charAt(salida.length() - 1);
        return c == ' ' || c == '\n';
    }

    private static String escapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ("\\`*_[]#<>".indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    private String resolver(String ref) {
        if (ref == null || ref.isBlank()) return "";
        try {
            return base.resolve(ref.trim().replace(" ", "%20")).toString();
        } catch (IllegalArgumentException e) {
            return ref;
        }
    }

    private static String limpiar(String md) {
        List<String> lineas = new ArrayList<>();
        for (String linea : md.split("\n", -1)) {
            lineas.add(linea.stripTrailing().isEmpty() ? "" : linea.replaceAll("\\s+$", linea.endsWith("  ") ? "  " : ""));
        }
        String resultado = String.join("\n", lineas).replaceAll("\n{3,}", "\n\n").strip();
        return resultado.isEmpty() ? "" : resultado + "\n";
    }
}