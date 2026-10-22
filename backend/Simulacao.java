import java.util.ArrayList;

/**
 * Guarda os grupos de pacientes informados no dashboard e calcula os indicadores.
 *
 * Cada grupo vira "quantidade" pacientes com a mesma espera.
 * Ex.: 5 amarelos com 30 min + 3 verdes com 150 min → [30, 30, 30, 30, 30, 150, 150, 150]
 */
public class Simulacao {

    private ArrayList<GrupoPacientes> grupos = new ArrayList<>();

    public void adicionarGrupo(GrupoPacientes grupo) {
        grupos.add(grupo);
    }

    public ArrayList<GrupoPacientes> getGrupos() {
        return grupos;
    }

    public boolean estaVazia() {
        return grupos.isEmpty();
    }

    // ---------------------------------------------------------------- listas para a Estatistica

    /** A espera de cada paciente da simulação. */
    public ArrayList<Double> getEsperas() {
        ArrayList<Double> esperas = new ArrayList<>();
        for (GrupoPacientes grupo : grupos) {
            for (int i = 0; i < grupo.getQuantidade(); i++) {
                esperas.add(grupo.getEspera());
            }
        }
        return esperas;
    }

    /** A espera de cada paciente de uma cor. */
    public ArrayList<Double> getEsperas(CorRisco cor) {
        ArrayList<Double> esperas = new ArrayList<>();
        for (GrupoPacientes grupo : grupos) {
            if (grupo.getCor() == cor) {
                for (int i = 0; i < grupo.getQuantidade(); i++) {
                    esperas.add(grupo.getEspera());
                }
            }
        }
        return esperas;
    }

    /** A prioridade da cor de cada paciente, para achar a cor predominante (moda). */
    public ArrayList<Double> getPrioridades() {
        ArrayList<Double> prioridades = new ArrayList<>();
        for (GrupoPacientes grupo : grupos) {
            for (int i = 0; i < grupo.getQuantidade(); i++) {
                prioridades.add((double) grupo.getCor().getPrioridade());
            }
        }
        return prioridades;
    }

    // ---------------------------------------------------------------- contagens

    public int getTotalPacientes() {
        int total = 0;
        for (GrupoPacientes grupo : grupos) {
            total = total + grupo.getQuantidade();
        }
        return total;
    }

    /** Pacientes que têm tempo máximo (todos menos os vermelhos). */
    public int getPacientesComPrazo() {
        int total = 0;
        for (GrupoPacientes grupo : grupos) {
            if (grupo.getCor().temPrazo()) {
                total = total + grupo.getQuantidade();
            }
        }
        return total;
    }

    public int getPacientesEstourados() {
        int total = 0;
        for (GrupoPacientes grupo : grupos) {
            if (grupo.prazoEstourado()) {
                total = total + grupo.getQuantidade();
            }
        }
        return total;
    }

    public int getPacientesEstourados(CorRisco cor) {
        int total = 0;
        for (GrupoPacientes grupo : grupos) {
            if (grupo.getCor() == cor && grupo.prazoEstourado()) {
                total = total + grupo.getQuantidade();
            }
        }
        return total;
    }

    public int getPacientesFastTrack() {
        return getPacientesNaFila("Fast Track");
    }

    public int getPacientesNaFila(String fila) {
        int total = 0;
        for (GrupoPacientes grupo : grupos) {
            if (grupo.getCor().getFila().equals(fila)) {
                total = total + grupo.getQuantidade();
            }
        }
        return total;
    }

    // ---------------------------------------------------------------- faixas de espera

    // Limites das faixas, em minutos: 0 a 15, 15 a 30, 30 a 60, 60 a 120, 120 a 240 e 240 ou mais.
    private static final int[] LIMITES_FAIXAS = {15, 30, 60, 120, 240};

    public static String[] getNomesFaixas() {
        String[] nomes = new String[LIMITES_FAIXAS.length + 1];
        int inicio = 0;
        for (int i = 0; i < LIMITES_FAIXAS.length; i++) {
            nomes[i] = inicio + " a " + LIMITES_FAIXAS[i] + " min";
            inicio = LIMITES_FAIXAS[i];
        }
        nomes[LIMITES_FAIXAS.length] = inicio + " min ou mais";
        return nomes;
    }

    /**
     * Distribuição de frequências: quantos pacientes esperaram em cada faixa de tempo.
     * Cada espera entra na primeira faixa cujo limite ela não atinge. Ex.: 30 min fica em "30 a 60 min".
     */
    public int[] getPacientesPorFaixa() {
        int[] contagem = new int[LIMITES_FAIXAS.length + 1];
        for (double espera : getEsperas()) {
            int faixa = LIMITES_FAIXAS.length; // última faixa: "240 min ou mais"
            for (int i = 0; i < LIMITES_FAIXAS.length; i++) {
                if (espera < LIMITES_FAIXAS[i]) {
                    faixa = i;
                    break;
                }
            }
            contagem[faixa]++;
        }
        return contagem;
    }

    // ---------------------------------------------------------------- resultado em JSON

    /** Monta o resultado que o dashboard mostra. A simulação precisa ter pelo menos um grupo. */
    public String toJson() {
        ArrayList<Double> esperas = getEsperas();
        int total = getTotalPacientes();
        int comPrazo = getPacientesComPrazo();
        int estourados = getPacientesEstourados();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"pacientes\":").append(total);
        json.append(",\"media\":").append(arredondar(Estatistica.media(esperas)));
        json.append(",\"mediana\":").append(arredondar(Estatistica.mediana(esperas)));
        json.append(",\"desvioPadrao\":").append(arredondar(Estatistica.desvioPadrao(esperas)));
        json.append(",\"minimo\":").append(arredondar(Estatistica.minimo(esperas)));
        json.append(",\"maximo\":").append(arredondar(Estatistica.maximo(esperas)));
        json.append(",\"comPrazo\":").append(comPrazo);
        json.append(",\"noPrazo\":").append(comPrazo - estourados);
        json.append(",\"estourados\":").append(estourados);

        // Sem pacientes com prazo (só vermelhos), não existe percentual no prazo.
        if (comPrazo == 0) {
            json.append(",\"percentualNoPrazo\":null");
        } else {
            json.append(",\"percentualNoPrazo\":").append(arredondar(100.0 * (comPrazo - estourados) / comPrazo));
        }
        json.append(",\"percentualFastTrack\":").append(arredondar(100.0 * getPacientesFastTrack() / total));

        // Cor predominante: a moda das prioridades, convertida para o nome da cor.
        json.append(",\"corPredominante\":[");
        ArrayList<Double> modas = Estatistica.moda(getPrioridades());
        for (int i = 0; i < modas.size(); i++) {
            if (i > 0) json.append(",");
            CorRisco cor = ProtocoloManchester.buscarCor(modas.get(i).intValue());
            json.append("\"").append(cor.getNome()).append("\"");
        }
        json.append("]");

        // Situação de cada grupo, na mesma ordem em que foram adicionados.
        json.append(",\"situacoes\":[");
        for (int i = 0; i < grupos.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(grupos.get(i).getSituacao()).append("\"");
        }
        json.append("]");

        // Pacientes, espera média e estourados de cada cor.
        json.append(",\"porCor\":[");
        CorRisco[] cores = ProtocoloManchester.getCores();
        for (int i = 0; i < cores.length; i++) {
            CorRisco cor = cores[i];
            ArrayList<Double> esperasDaCor = getEsperas(cor);
            if (i > 0) json.append(",");
            json.append("{\"nome\":\"").append(cor.getNome()).append("\"");
            json.append(",\"prioridade\":").append(cor.getPrioridade());
            json.append(",\"tempoMaximo\":").append(cor.getTempoMaximo());
            json.append(",\"pacientes\":").append(esperasDaCor.size());
            if (esperasDaCor.isEmpty()) {
                json.append(",\"mediaEspera\":null");
            } else {
                json.append(",\"mediaEspera\":").append(arredondar(Estatistica.media(esperasDaCor)));
            }
            json.append(",\"estourados\":").append(getPacientesEstourados(cor));
            json.append("}");
        }
        json.append("]");

        // Pacientes em cada fila de atendimento.
        String[] filas = {"Emergência", "Principal", "Fast Track"};
        json.append(",\"porFila\":[");
        for (int i = 0; i < filas.length; i++) {
            if (i > 0) json.append(",");
            json.append("{\"fila\":\"").append(filas[i]).append("\"");
            json.append(",\"pacientes\":").append(getPacientesNaFila(filas[i])).append("}");
        }
        json.append("]");

        // Quantos pacientes em cada faixa de espera.
        String[] nomesFaixas = getNomesFaixas();
        int[] pacientesPorFaixa = getPacientesPorFaixa();
        json.append(",\"faixasEspera\":[");
        for (int i = 0; i < nomesFaixas.length; i++) {
            if (i > 0) json.append(",");
            json.append("{\"faixa\":\"").append(nomesFaixas[i]).append("\"");
            json.append(",\"pacientes\":").append(pacientesPorFaixa[i]).append("}");
        }
        json.append("]");

        json.append("}");
        return json.toString();
    }

    // Duas casas decimais bastam para minutos e percentuais.
    private double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
