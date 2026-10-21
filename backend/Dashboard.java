import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Servidor do simulador de espera do pronto-socorro.
 *
 *   POST /calcular   grupos "prioridade:quantidade:espera" separados por vírgula (ex.: "3:5:30,4:3:150")
 *   GET  /*          arquivos da pasta frontend
 */
public class Dashboard {

    static final int MAX_PACIENTES = 10_000;

    static final Path FRONTEND = (Files.isDirectory(Path.of("frontend")) ? Path.of("frontend") : Path.of("../frontend"))
            .toAbsolutePath().normalize();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/", Dashboard::arquivo);
        server.createContext("/calcular", Dashboard::calcular);
        server.start();
        System.out.println("Dashboard rodando em http://localhost:8081");
    }

    static void arquivo(HttpExchange ex) throws IOException {
        String caminho = ex.getRequestURI().getPath();
        if (caminho.equals("/")) caminho = "/index.html";

        Path arquivo = FRONTEND.resolve(caminho.substring(1)).normalize();
        if (!arquivo.startsWith(FRONTEND) || !Files.isRegularFile(arquivo)) {
            ex.sendResponseHeaders(404, -1);
            ex.close();
            return;
        }

        String tipo = caminho.endsWith(".css") ? "text/css"
                : caminho.endsWith(".js") ? "application/javascript"
                : "text/html";
        responder(ex, 200, tipo, Files.readAllBytes(arquivo));
    }

    /** Lê os grupos enviados pelo dashboard, valida e devolve os indicadores calculados. */
    static void calcular(HttpExchange ex) throws IOException {
        String corpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        List<Simulacao.Grupo> grupos = new ArrayList<>();
        int total = 0;
        try {
            for (String s : corpo.split(",")) {
                if (s.isBlank()) continue;
                String[] partes = s.split(":");
                if (partes.length != 3) throw new IllegalArgumentException("formato inválido: " + s.trim());
                Simulacao.CorRisco cor = Simulacao.corPorPrioridade(Integer.parseInt(partes[0].trim()));
                int quantidade = quantidade(Integer.parseInt(partes[1].trim()));
                double espera = espera(Double.parseDouble(partes[2].trim()));
                total += quantidade;
                if (total > MAX_PACIENTES) throw new IllegalArgumentException("máximo de " + MAX_PACIENTES + " pacientes");
                grupos.add(new Simulacao.Grupo(cor, quantidade, espera));
            }
        } catch (IllegalArgumentException e) {
            erro(ex, e instanceof NumberFormatException ? "número inválido" : e.getMessage());
            return;
        }

        String json = grupos.isEmpty() ? "{}" : Simulacao.calcular(grupos);
        responder(ex, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    static int quantidade(int n) {
        if (n < 1) throw new IllegalArgumentException("a quantidade de pacientes precisa ser pelo menos 1");
        return n;
    }

    // O médico não atende antes da chegada do paciente.
    static double espera(double minutos) {
        if (!Double.isFinite(minutos)) throw new NumberFormatException();
        if (minutos < 0) throw new IllegalArgumentException("a espera não pode ser negativa");
        return minutos;
    }

    static void erro(HttpExchange ex, String mensagem) throws IOException {
        String json = "{\"erro\":\"" + mensagem.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        responder(ex, 400, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    static void responder(HttpExchange ex, int status, String tipo, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
