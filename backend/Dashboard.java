import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Inicia o servidor do simulador de espera.
 *
 *   GET  /           página do dashboard (pasta frontend)
 *   POST /calcular   recebe os grupos de pacientes e devolve os indicadores
 */
public class Dashboard {

    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8081), 0);
        servidor.createContext("/", new ArquivoHandler());
        servidor.createContext("/calcular", new CalcularHandler());
        servidor.start();
        System.out.println("Dashboard rodando em http://localhost:8081");
    }

    /** Envia uma resposta para o navegador. Usado pelos dois handlers. */
    public static void enviarResposta(HttpExchange troca, int status, String tipo, byte[] conteudo) throws IOException {
        troca.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        troca.sendResponseHeaders(status, conteudo.length);
        OutputStream saida = troca.getResponseBody();
        saida.write(conteudo);
        saida.close();
    }

    public static void enviarJson(HttpExchange troca, int status, String json) throws IOException {
        enviarResposta(troca, status, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }
}
