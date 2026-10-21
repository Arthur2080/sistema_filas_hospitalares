import java.util.*;

/**
 * Cálculos estatísticos do dashboard: soma, média, mediana, desvio padrão, mínimo, máximo,
 * frequências e moda.
 *
 * Os dados são tempos de espera em minutos (da chegada até o médico) ou números que
 * representam categorias (a prioridade da cor de risco).
 * Toda lista recebida precisa ter pelo menos um valor; quem chama confere isso antes.
 */
public class Estatistica {

    /** Soma todos os valores da lista. */
    static double soma(List<Double> nums) {
        double total = 0;
        for (double n : nums) {
            total = total + n;
        }
        return total;
    }

    /** Média aritmética: soma dos valores dividida pela quantidade. */
    static double media(List<Double> nums) {
        return soma(nums) / nums.size();
    }

    /**
     * Mediana: o valor do meio da lista ordenada.
     * Quantidade ímpar: é o elemento do meio. Quantidade par: é a média dos dois do meio.
     * Ex.: [86, 4, 49, 4] → ordenada [4, 4, 49, 86] → (4 + 49) / 2 = 26,5
     */
    static double mediana(List<Double> nums) {
        List<Double> ordenada = new ArrayList<>(nums);
        Collections.sort(ordenada);

        int meio = ordenada.size() / 2;
        if (ordenada.size() % 2 == 1) {
            return ordenada.get(meio);
        }
        return (ordenada.get(meio - 1) + ordenada.get(meio)) / 2;
    }

    /**
     * Desvio padrão populacional: o quanto os valores se afastam da média, em média.
     * 1) calcula a média; 2) soma o quadrado da distância de cada valor até a média;
     * 3) divide pela quantidade (variância); 4) tira a raiz quadrada.
     *
     * Divide por n (e não por n - 1) porque os pacientes simulados são o grupo inteiro
     * (a população), e não uma amostra dela.
     */
    static double desvioPadrao(List<Double> nums) {
        double m = media(nums);
        double somaQuadrados = 0;
        for (double n : nums) {
            double distancia = n - m;
            somaQuadrados = somaQuadrados + distancia * distancia;
        }
        double variancia = somaQuadrados / nums.size();
        return Math.sqrt(variancia);
    }

    /** Menor valor da lista. */
    static double minimo(List<Double> nums) {
        double menor = nums.get(0);
        for (double n : nums) {
            if (n < menor) menor = n;
        }
        return menor;
    }

    /** Maior valor da lista. */
    static double maximo(List<Double> nums) {
        double maior = nums.get(0);
        for (double n : nums) {
            if (n > maior) maior = n;
        }
        return maior;
    }

    /**
     * Tabela de frequências: quantas vezes cada valor aparece.
     * Ex.: [3, 4, 3, 3] → {3=3, 4=1}. O TreeMap mantém os valores em ordem crescente.
     */
    static Map<Double, Integer> frequencias(List<Double> nums) {
        Map<Double, Integer> freq = new TreeMap<>();
        for (double n : nums) {
            if (freq.containsKey(n)) {
                freq.put(n, freq.get(n) + 1);
            } else {
                freq.put(n, 1);
            }
        }
        return freq;
    }

    /**
     * Moda: o valor (ou os valores, em caso de empate) que mais aparece.
     * Se todos aparecem uma vez só, não há moda e a lista volta vazia (amodal).
     */
    static List<Double> moda(List<Double> nums) {
        Map<Double, Integer> freq = frequencias(nums);

        int maiorFrequencia = 0;
        for (int contagem : freq.values()) {
            if (contagem > maiorFrequencia) maiorFrequencia = contagem;
        }

        List<Double> modas = new ArrayList<>();
        if (maiorFrequencia <= 1) return modas;
        for (double valor : freq.keySet()) {
            if (freq.get(valor) == maiorFrequencia) modas.add(valor);
        }
        return modas;
    }
}
