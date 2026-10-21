import java.util.*;

/**
 * Regras do pronto-socorro e cálculo do simulador.
 *
 * O usuário informa grupos de pacientes: a cor de risco, quantos pacientes e quanto tempo esperaram até o médico.
 * Cada grupo vira "quantidade" pacientes com a mesma espera, e a classe Estatistica calcula sobre essa lista.
 * Ex.: 5 amarelos com 30 min + 3 verdes com 150 min → [30, 30, 30, 30, 30, 150, 150, 150]
 */
public class Simulacao {

    record CorRisco(String nome, int prioridade, int tempoMaxMin, String fila) {}

    record Grupo(CorRisco cor, int quantidade, double espera) {}

    /** Protocolo de Manchester: prioridade 1 é a mais grave. O tempo máximo é a espera aceitável até o médico. */
    static final List<CorRisco> CORES = List.of(
            new CorRisco("Vermelho", 1, 0, "IMEDIATA"),
            new CorRisco("Laranja", 2, 10, "PRINCIPAL"),
            new CorRisco("Amarelo", 3, 60, "PRINCIPAL"),
            new CorRisco("Verde", 4, 120, "FAST_TRACK"),
            new CorRisco("Azul", 5, 240, "FAST_TRACK"));

    static CorRisco corPorPrioridade(int prioridade) {
        for (CorRisco c : CORES) {
            if (c.prioridade() == prioridade) return c;
        }
        throw new IllegalArgumentException("cor de risco inexistente: " + prioridade);
    }

    /** O prazo estoura quando a espera atinge o tempo máximo da cor. O vermelho é atendido na hora e não tem prazo. */
    static boolean estourouPrazo(CorRisco c, double espera) {
        return !c.fila().equals("IMEDIATA") && espera >= c.tempoMaxMin();
    }

    /** A espera de cada paciente: o grupo se repete "quantidade" vezes. */
    static List<Double> esperasDosPacientes(List<Grupo> grupos) {
        List<Double> esperas = new ArrayList<>();
        for (Grupo g : grupos) {
            for (int i = 0; i < g.quantidade(); i++) {
                esperas.add(g.espera());
            }
        }
        return esperas;
    }

    /** A prioridade da cor de cada paciente, para achar a cor predominante (moda). */
    static List<Double> prioridadesDosPacientes(List<Grupo> grupos) {
        List<Double> prioridades = new ArrayList<>();
        for (Grupo g : grupos) {
            for (int i = 0; i < g.quantidade(); i++) {
                prioridades.add((double) g.cor().prioridade());
            }
        }
        return prioridades;
    }

    /** Calcula todos os indicadores da simulação e devolve em JSON para o dashboard. */
    static String calcular(List<Grupo> grupos) {
        List<Double> esperas = esperasDosPacientes(grupos);
        int total = esperas.size();

        // Prazos e filas, contando pacientes (cada grupo vale "quantidade" pacientes).
        int comPrazo = 0, estourados = 0, fastTrack = 0;
        List<String> situacoes = new ArrayList<>();
        for (Grupo g : grupos) {
            if (g.cor().fila().equals("FAST_TRACK")) fastTrack += g.quantidade();
            if (g.cor().fila().equals("IMEDIATA")) {
                situacoes.add("\"IMEDIATO\"");
            } else {
                comPrazo += g.quantidade();
                if (estourouPrazo(g.cor(), g.espera())) {
                    estourados += g.quantidade();
                    situacoes.add("\"ESTOURADO\"");
                } else {
                    situacoes.add("\"NO_PRAZO\"");
                }
            }
        }

        // Pacientes, espera média e estourados de cada cor.
        List<String> porCor = new ArrayList<>();
        for (CorRisco c : CORES) {
            List<Grupo> daCor = new ArrayList<>();
            int estouradosCor = 0;
            for (Grupo g : grupos) {
                if (g.cor() != c) continue;
                daCor.add(g);
                if (estourouPrazo(c, g.espera())) estouradosCor += g.quantidade();
            }
            List<Double> esperasCor = esperasDosPacientes(daCor);
            porCor.add("{\"nome\":\"" + c.nome() + "\""
                    + ",\"prioridade\":" + c.prioridade()
                    + ",\"tempoMax\":" + c.tempoMaxMin()
                    + ",\"pacientes\":" + esperasCor.size()
                    + ",\"mediaEspera\":" + (esperasCor.isEmpty() ? "null" : num(Estatistica.media(esperasCor)))
                    + ",\"estourados\":" + estouradosCor + "}");
        }

        return "{"
                + "\"pacientes\":" + total
                + ",\"media\":" + num(Estatistica.media(esperas))
                + ",\"mediana\":" + num(Estatistica.mediana(esperas))
                + ",\"desvioPadrao\":" + num(Estatistica.desvioPadrao(esperas))
                + ",\"minimo\":" + num(Estatistica.minimo(esperas))
                + ",\"maximo\":" + num(Estatistica.maximo(esperas))
                + ",\"comPrazo\":" + comPrazo
                + ",\"noPrazo\":" + (comPrazo - estourados)
                + ",\"estourados\":" + estourados
                + ",\"percentualNoPrazo\":" + (comPrazo == 0 ? "null" : num(100.0 * (comPrazo - estourados) / comPrazo))
                + ",\"percentualFastTrack\":" + num(100.0 * fastTrack / total)
                + ",\"corPredominante\":" + lista(Estatistica.moda(prioridadesDosPacientes(grupos)))
                + ",\"situacoes\":" + situacoes
                + ",\"porCor\":[" + String.join(",", porCor) + "]"
                + "}";
    }

    // ---------------------------------------------------------------- JSON

    /** Número sem casas decimais quando é inteiro; senão, com até 4 casas. */
    static String num(double n) {
        return n == Math.rint(n) && Math.abs(n) < 1e15
                ? String.valueOf((long) n)
                : String.valueOf(Math.round(n * 10000) / 10000.0);
    }

    static String lista(List<Double> nums) {
        StringJoiner j = new StringJoiner(",", "[", "]");
        for (double n : nums) j.add(num(n));
        return j.toString();
    }
}
