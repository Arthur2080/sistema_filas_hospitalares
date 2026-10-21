/**
 * Um grupo de pacientes da mesma cor de risco que esperaram o mesmo tempo até o médico.
 * Ex.: 5 pacientes amarelos que esperaram 30 minutos.
 */
public class GrupoPacientes {

    private CorRisco cor;
    private int quantidade;
    private double espera; // em minutos

    public GrupoPacientes(CorRisco cor, int quantidade, double espera) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("a quantidade de pacientes precisa ser pelo menos 1");
        }
        if (Double.isNaN(espera) || Double.isInfinite(espera)) {
            throw new IllegalArgumentException("número inválido");
        }
        if (espera < 0) {
            throw new IllegalArgumentException("a espera não pode ser negativa");
        }
        this.cor = cor;
        this.quantidade = quantidade;
        this.espera = espera;
    }

    public CorRisco getCor() {
        return cor;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public double getEspera() {
        return espera;
    }

    public boolean prazoEstourado() {
        return cor.prazoEstourado(espera);
    }

    // Situação do grupo, mostrada no dashboard: IMEDIATO, ESTOURADO ou NO_PRAZO.
    public String getSituacao() {
        if (!cor.temPrazo()) {
            return "IMEDIATO";
        }
        if (prazoEstourado()) {
            return "ESTOURADO";
        }
        return "NO_PRAZO";
    }
}
