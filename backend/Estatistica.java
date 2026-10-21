import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

/**
 * Cálculos estatísticos do dashboard: soma, média, mediana, desvio padrão, mínimo, máximo,
 * frequências e moda.
 *
 * Os dados são tempos de espera em minutos ou números que representam categorias
 * (a prioridade da cor de risco). Toda lista recebida precisa ter pelo menos um valor.
 */
public class Estatistica {

    /** Soma todos os valores da lista. */
    public static double soma(ArrayList<Double> numeros) {
        double total = 0;
        for (double n : numeros) {
            total = total + n;
        }
        return total;
    }

    /** Média aritmética: soma dos valores dividida pela quantidade. */
    public static double media(ArrayList<Double> numeros) {
        return soma(numeros) / numeros.size();
    }

    /**
     * Mediana: o valor do meio da lista ordenada.
     * Quantidade ímpar: é o elemento do meio. Quantidade par: é a média dos dois do meio.
     * Ex.: [86, 4, 49, 4] → ordenada [4, 4, 49, 86] → (4 + 49) / 2 = 26,5
     */
    public static double mediana(ArrayList<Double> numeros) {
        ArrayList<Double> ordenada = new ArrayList<>(numeros); // cópia, para não mudar a lista original
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
    public static double desvioPadrao(ArrayList<Double> numeros) {
        double media = media(numeros);
        double somaQuadrados = 0;
        for (double n : numeros) {
            double distancia = n - media;
            somaQuadrados = somaQuadrados + distancia * distancia;
        }
        double variancia = somaQuadrados / numeros.size();
        return Math.sqrt(variancia);
    }

    /** Menor valor da lista. */
    public static double minimo(ArrayList<Double> numeros) {
        double menor = numeros.get(0);
        for (double n : numeros) {
            if (n < menor) {
                menor = n;
            }
        }
        return menor;
    }

    /** Maior valor da lista. */
    public static double maximo(ArrayList<Double> numeros) {
        double maior = numeros.get(0);
        for (double n : numeros) {
            if (n > maior) {
                maior = n;
            }
        }
        return maior;
    }

    /**
     * Tabela de frequências: quantas vezes cada valor aparece.
     * Ex.: [3, 4, 3, 3] → {3=3, 4=1}. O TreeMap mantém os valores em ordem crescente.
     */
    public static TreeMap<Double, Integer> frequencias(ArrayList<Double> numeros) {
        TreeMap<Double, Integer> frequencia = new TreeMap<>();
        for (double n : numeros) {
            if (frequencia.containsKey(n)) {
                frequencia.put(n, frequencia.get(n) + 1);
            } else {
                frequencia.put(n, 1);
            }
        }
        return frequencia;
    }

    /**
     * Moda: o valor (ou os valores, em caso de empate) que mais aparece.
     * Se todos aparecem uma vez só, não há moda e a lista volta vazia (amodal).
     */
    public static ArrayList<Double> moda(ArrayList<Double> numeros) {
        TreeMap<Double, Integer> frequencia = frequencias(numeros);

        int maiorFrequencia = 0;
        for (int contagem : frequencia.values()) {
            if (contagem > maiorFrequencia) {
                maiorFrequencia = contagem;
            }
        }

        ArrayList<Double> modas = new ArrayList<>();
        if (maiorFrequencia <= 1) {
            return modas;
        }
        for (double valor : frequencia.keySet()) {
            if (frequencia.get(valor) == maiorFrequencia) {
                modas.add(valor);
            }
        }
        return modas;
    }
}
