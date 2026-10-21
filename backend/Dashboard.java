import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Servidor do dashboard do pronto-socorro.
 *
 *   GET  /api/painel?agora=2026-10-06T10:46   indicadores dos dados de teste no horário do relógio simulado
 *   POST /calcular                            simulador: "prioridade:minutos" separados por vírgula
 *   GET  /*                                   arquivos da pasta frontend
 */
public class Dashboard {

    static final Path FRONTEND = (Files.isDirectory(Path.of("frontend")) ? Path.of("frontend") : Path.of("../frontend"))
            .toAbsolutePath().normalize();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/", Dashboard::arquivo);
        server.createContext("/api/painel", Dashboard::painel);
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

    static void painel(HttpExchange ex) throws IOException {
        LocalDateTime agora = Dados.RELOGIO_INICIAL;
        String query = ex.getRequestURI().getQuery();
        if (query != null && query.startsWith("agora=")) {
            try {
                agora = LocalDateTime.parse(query.substring(6));
            } catch (DateTimeParseException e) {
                erro(ex, "data e hora inválidas");
                return;
            }
        }
        responder(ex, 200, "application/json", Indicadores.painel(agora).getBytes(StandardCharsets.UTF_8));
    }

    /** Simulador: cada valor é um paciente, com a prioridade da cor e a espera até o médico em minutos. */
    static void calcular(HttpExchange ex) throws IOException {
        String corpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        List<Dados.CorRisco> cores = new ArrayList<>();
        List<Double> esperas = new ArrayList<>();
        try {
            for (String s : corpo.split(",")) {
                if (s.isBlank()) continue;
                String[] partes = s.split(":");
                if (partes.length != 2) throw new IllegalArgumentException("formato inválido: " + s.trim());
                cores.add(corPorPrioridade(Integer.parseInt(partes[0].trim())));
                esperas.add(espera(Double.parseDouble(partes[1].trim())));
            }
        } catch (IllegalArgumentException e) {
            erro(ex, e instanceof NumberFormatException ? "número inválido" : e.getMessage());
            return;
        }

        if (esperas.isEmpty()) {
            responder(ex, 200, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
            return;
        }

        List<Double> prioridades = new ArrayList<>();
        for (Dados.CorRisco c : cores) prioridades.add((double) c.prioridade());
        Map<Double, Integer> freqCores = Estatistica.frequencias(prioridades);

        int comPrazo = 0, estourados = 0, fastTrack = 0;
        List<String> situacoes = new ArrayList<>();
        for (int i = 0; i < esperas.size(); i++) {
            Dados.CorRisco c = cores.get(i);
            if (c.fila().equals("FAST_TRACK")) fastTrack++;
            if (c.fila().equals("IMEDIATA")) {
                situacoes.add("\"IMEDIATO\"");
                continue;
            }
            comPrazo++;
            boolean estourou = estourouPrazo(c, esperas.get(i));
            if (estourou) estourados++;
            situacoes.add(estourou ? "\"ESTOURADO\"" : "\"NO_PRAZO\"");
        }

        List<String> porCor = new ArrayList<>();
        for (Dados.CorRisco c : Dados.CORES) {
            List<Double> daCor = new ArrayList<>();
            int estouradosCor = 0;
            for (int i = 0; i < esperas.size(); i++) {
                if (cores.get(i) != c) continue;
                daCor.add(esperas.get(i));
                if (estourouPrazo(c, esperas.get(i))) estouradosCor++;
            }
            porCor.add("{\"prioridade\":" + c.prioridade()
                    + ",\"pacientes\":" + freqCores.getOrDefault((double) c.prioridade(), 0)
                    + ",\"mediaEspera\":" + (daCor.isEmpty() ? "null" : Indicadores.num(Estatistica.media(daCor)))
                    + ",\"estourados\":" + estouradosCor + "}");
        }

        String json = "{"
                + "\"quantidade\":" + esperas.size()
                + ",\"resumo\":" + Indicadores.resumo(esperas)
                + ",\"mediaAcumulada\":" + Indicadores.lista(Estatistica.mediaAcumulada(esperas))
                + ",\"situacoes\":" + situacoes
                + ",\"comPrazo\":" + comPrazo
                + ",\"noPrazo\":" + (comPrazo - estourados)
                + ",\"estourados\":" + estourados
                + ",\"percentualNoPrazo\":" + (comPrazo == 0 ? "null" : Indicadores.num(100.0 * (comPrazo - estourados) / comPrazo))
                + ",\"percentualFastTrack\":" + Indicadores.num(100.0 * fastTrack / esperas.size())
                + ",\"corPredominante\":" + Indicadores.lista(Estatistica.moda(prioridades))
                + ",\"porCor\":[" + String.join(",", porCor) + "]"
                + "}";
        responder(ex, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    // Mesma regra do AlertaPrazo: o prazo estoura quando a espera atinge o tempo máximo da cor.
    // O vermelho é atendido na hora e não gera alerta.
    static boolean estourouPrazo(Dados.CorRisco c, double espera) {
        return !c.fila().equals("IMEDIATA") && espera >= c.tempoMaxMin();
    }

    static Dados.CorRisco corPorPrioridade(int prioridade) {
        for (Dados.CorRisco c : Dados.CORES) if (c.prioridade() == prioridade) return c;
        throw new IllegalArgumentException("cor de risco inexistente: " + prioridade);
    }

    // O médico não atende antes da chegada do paciente.
    static double espera(double minutos) {
        if (!Double.isFinite(minutos)) throw new NumberFormatException();
        if (minutos < 0) throw new IllegalArgumentException("a espera não pode ser negativa");
        return minutos;
    }

    static void erro(HttpExchange ex, String mensagem) throws IOException {
        responder(ex, 400, "application/json", ("{\"erro\":" + Indicadores.str(mensagem) + "}").getBytes(StandardCharsets.UTF_8));
    }

    static void responder(HttpExchange ex, int status, String tipo, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
