import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkdownWriter {

    /**
     * Guarda el contenido en un archivo UTF-8.
     * Crea los directorios de destino si no existen.
     */
    public void guardar(Path destino, String contenido) throws IOException {
        Path padre = destino.toAbsolutePath().getParent();
        if (padre != null) {
            Files.createDirectories(padre);
        }
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
    }

    /** Devuelve el tamaño en bytes del archivo ya guardado. */
    public long tamano(Path destino) throws IOException {
        return Files.size(destino);
    }
}