import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entrega os arquivos da pasta frontend (HTML, CSS e JavaScript) para o navegador.
 */
public class ArquivoHandler implements HttpHandler {

    private Path pastaFrontend;

    public ArquivoHandler() {
        // Funciona rodando da pasta do projeto ou de dentro da pasta backend.
        if (Files.isDirectory(Path.of("frontend"))) {
            pastaFrontend = Path.of("frontend").toAbsolutePath().normalize();
        } else {
            pastaFrontend = Path.of("../frontend").toAbsolutePath().normalize();
        }
    }

    @Override
    public void handle(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.equals("/")) {
            caminho = "/index.html";
        }

        // Só entrega arquivos que estão dentro da pasta frontend.
        Path arquivo = pastaFrontend.resolve(caminho.substring(1)).normalize();
        if (!arquivo.startsWith(pastaFrontend) || !Files.isRegularFile(arquivo)) {
            troca.sendResponseHeaders(404, -1);
            troca.close();
            return;
        }

        String tipo = "text/html";
        if (caminho.endsWith(".css")) {
            tipo = "text/css";
        } else if (caminho.endsWith(".js")) {
            tipo = "application/javascript";
        }
        Dashboard.enviarResposta(troca, 200, tipo, Files.readAllBytes(arquivo));
    }
}
