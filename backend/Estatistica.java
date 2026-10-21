import java.util.*;

/** Métodos de cálculo do dashboard (os mesmos de antes) e a formatação dos números para JSON. */
public class Estatistica {

    static double soma(List<Double> nums) {
        double s = 0;
        for (double n : nums) s += n;
        return s;
    }

    static double media(List<Double> nums) {
        return soma(nums) / nums.size();
    }

    static double percentil(List<Double> ord, double p) {
        double pos = p * (ord.size() - 1);
        int baixo = (int) Math.floor(pos), alto = (int) Math.ceil(pos);
        return ord.get(baixo) + (ord.get(alto) - ord.get(baixo)) * (pos - baixo);
    }

    static double desvioPadrao(List<Double> nums) {
        double m = media(nums), s = 0;
        for (double n : nums) s += (n - m) * (n - m);
        return Math.sqrt(s / nums.size());
    }

    static Map<Double, Integer> frequencias(List<Double> nums) {
        Map<Double, Integer> freq = new TreeMap<>();
        for (double n : nums) freq.merge(n, 1, Integer::sum);
        return freq;
    }

    static List<Double> moda(Map<Double, Integer> freq) {
        List<Double> modas = new ArrayList<>();
        if (freq.isEmpty()) return modas;
        int max = Collections.max(freq.values());
        if (max == 1) return modas;
        for (var e : freq.entrySet()) {
            if (e.getValue() == max) modas.add(e.getKey());
        }
        return modas;
    }

    static List<Double> mediaAcumulada(List<Double> nums) {
        List<Double> medias = new ArrayList<>();
        double s = 0;
        for (int i = 0; i < nums.size(); i++) {
            s += nums.get(i);
            medias.add(s / (i + 1));
        }
        return medias;
    }

    /** Resumo de uma lista de tempos: quantidade, média, mediana, desvio padrão, mínimo e máximo. */
    static String resumo(List<Double> nums) {
        if (nums.isEmpty()) return "{\"quantidade\":0}";
        List<Double> ord = new ArrayList<>(nums);
        Collections.sort(ord);
        return "{\"quantidade\":" + nums.size()
                + ",\"media\":" + num(media(nums))
                + ",\"mediana\":" + num(percentil(ord, 0.5))
                + ",\"desvioPadrao\":" + num(desvioPadrao(nums))
                + ",\"minimo\":" + num(ord.get(0))
                + ",\"maximo\":" + num(ord.get(ord.size() - 1))
                + "}";
    }

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

    static String frequenciasJson(Map<Double, Integer> freq) {
        StringJoiner j = new StringJoiner(",", "[", "]");
        for (var e : freq.entrySet()) {
            j.add("{\"valor\":" + num(e.getKey()) + ",\"contagem\":" + e.getValue() + "}");
        }
        return j.toString();
    }
}
