import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Recebe os grupos de pacientes do dashboard e devolve os indicadores calculados.
 *
 * Formato recebido: "prioridade:quantidade:espera" separados por vírgula.
 * Ex.: "3:5:30,4:3:150" = 5 amarelos que esperaram 30 min e 3 verdes que esperaram 150 min.
 */
public class CalcularHandler implements HttpHandler {

    private static final int MAXIMO_PACIENTES = 10000;

    @Override
    public void handle(HttpExchange troca) throws IOException {
        String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        Simulacao simulacao = new Simulacao();
        try {
            String[] grupos = corpo.split(",");
            for (String texto : grupos) {
                if (!texto.isBlank()) {
                    simulacao.adicionarGrupo(lerGrupo(texto.trim()));
                }
            }
            if (simulacao.getTotalPacientes() > MAXIMO_PACIENTES) {
                throw new IllegalArgumentException("máximo de " + MAXIMO_PACIENTES + " pacientes");
            }
        } catch (NumberFormatException e) {
            enviarErro(troca, "número inválido");
            return;
        } catch (IllegalArgumentException e) {
            enviarErro(troca, e.getMessage());
            return;
        }

        if (simulacao.estaVazia()) {
            Dashboard.enviarJson(troca, 200, "{}");
        } else {
            Dashboard.enviarJson(troca, 200, simulacao.toJson());
        }
    }

    // Transforma "3:5:30" em um grupo: cor de prioridade 3, 5 pacientes, 30 minutos.
    private GrupoPacientes lerGrupo(String texto) {
        String[] partes = texto.split(":");
        if (partes.length != 3) {
            throw new IllegalArgumentException("formato inválido: " + texto);
        }
        CorRisco cor = ProtocoloManchester.buscarCor(Integer.parseInt(partes[0].trim()));
        int quantidade = Integer.parseInt(partes[1].trim());
        double espera = Double.parseDouble(partes[2].trim());
        return new GrupoPacientes(cor, quantidade, espera);
    }

    private void enviarErro(HttpExchange troca, String mensagem) throws IOException {
        // Troca aspas e barras para a mensagem não quebrar o JSON.
        mensagem = mensagem.replace("\\", "/").replace("\"", "'");
        Dashboard.enviarJson(troca, 400, "{\"erro\":\"" + mensagem + "\"}");
    }
}
