/**
 * Uma cor de risco do Protocolo de Manchester.
 * Cada cor tem uma prioridade (1 é a mais grave), um tempo máximo de espera até o médico e uma fila.
 */
public class CorRisco {

    private String nome;
    private int prioridade;
    private int tempoMaximo; // em minutos (0 = atendimento imediato)
    private String fila;

    public CorRisco(String nome, int prioridade, int tempoMaximo, String fila) {
        this.nome = nome;
        this.prioridade = prioridade;
        this.tempoMaximo = tempoMaximo;
        this.fila = fila;
    }

    public String getNome() {
        return nome;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public int getTempoMaximo() {
        return tempoMaximo;
    }

    public String getFila() {
        return fila;
    }

    // O vermelho é atendido na hora, então não tem prazo.
    public boolean temPrazo() {
        return tempoMaximo > 0;
    }

    // O prazo estoura quando a espera chega no tempo máximo da cor.
    public boolean prazoEstourado(double espera) {
        return temPrazo() && espera >= tempoMaximo;
    }
}
