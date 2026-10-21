/**
 * As cinco cores de risco usadas na triagem do pronto-socorro.
 */
public class ProtocoloManchester {

    private static final CorRisco[] CORES = {
            new CorRisco("Vermelho", 1, 0, "Emergência"),
            new CorRisco("Laranja", 2, 10, "Principal"),
            new CorRisco("Amarelo", 3, 60, "Principal"),
            new CorRisco("Verde", 4, 120, "Fast Track"),
            new CorRisco("Azul", 5, 240, "Fast Track")
    };

    public static CorRisco[] getCores() {
        return CORES;
    }

    public static CorRisco buscarCor(int prioridade) {
        for (CorRisco cor : CORES) {
            if (cor.getPrioridade() == prioridade) {
                return cor;
            }
        }
        throw new IllegalArgumentException("cor de risco inexistente: " + prioridade);
    }
}
