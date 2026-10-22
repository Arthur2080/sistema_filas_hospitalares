import java.util.ArrayList;

/**
 * Confere os cálculos com resultados feitos à mão.
 *
 *   java backend/TesteEstatistica.java
 */
public class TesteEstatistica {

    static int falhas = 0;

    public static void main(String[] args) {
        // Quatro esperas até o médico, em minutos
        ArrayList<Double> esperas = lista(86, 4, 49, 4);

        // soma = 86 + 4 + 49 + 4 = 143; média = 143 / 4 = 35,75
        conferir("soma", Estatistica.soma(esperas), 143);
        conferir("média", Estatistica.media(esperas), 35.75);
        // ordenada: [4, 4, 49, 86]; par → (4 + 49) / 2 = 26,5
        conferir("mediana (quantidade par)", Estatistica.mediana(esperas), 26.5);
        // distâncias até 35,75: 50,25 | -31,75 | 13,25 | -31,75
        // quadrados: 2525,0625 + 1008,0625 + 175,5625 + 1008,0625 = 4716,75 → variância 4716,75 / 4 = 1179,1875
        conferir("desvio padrão", Estatistica.desvioPadrao(esperas), Math.sqrt(1179.1875));
        conferir("mínimo", Estatistica.minimo(esperas), 4);
        conferir("máximo", Estatistica.maximo(esperas), 86);
        // o 4 aparece duas vezes, os outros uma
        conferir("moda", Estatistica.moda(esperas), lista(4));

        // Casos de borda
        conferir("mediana (quantidade ímpar)", Estatistica.mediana(lista(30, 10, 20)), 20);
        conferir("mediana (um valor)", Estatistica.mediana(lista(7)), 7);
        conferir("desvio padrão (valores iguais)", Estatistica.desvioPadrao(lista(5, 5, 5)), 0);
        conferir("moda (sem repetição = amodal)", Estatistica.moda(lista(1, 2, 3)), lista());
        conferir("moda (empate = bimodal)", Estatistica.moda(lista(2, 2, 4, 4, 5)), lista(2, 4));

        // Simulação: 5 amarelos que esperaram 30 min + 3 verdes que esperaram 150 min = 8 pacientes
        Simulacao simulacao = new Simulacao();
        simulacao.adicionarGrupo(new GrupoPacientes(ProtocoloManchester.buscarCor(3), 5, 30));
        simulacao.adicionarGrupo(new GrupoPacientes(ProtocoloManchester.buscarCor(4), 3, 150));
        ArrayList<Double> esperasSimulacao = simulacao.getEsperas();

        conferir("grupos viram 8 pacientes", esperasSimulacao, lista(30, 30, 30, 30, 30, 150, 150, 150));
        // (5 × 30 + 3 × 150) / 8 = 600 / 8 = 75
        conferir("média com quantidade", Estatistica.media(esperasSimulacao), 75);
        // 4º e 5º valores da lista ordenada: (30 + 30) / 2 = 30
        conferir("mediana com quantidade", Estatistica.mediana(esperasSimulacao), 30);
        // 5 × (30 − 75)² + 3 × (150 − 75)² = 10125 + 16875 = 27000 → variância 27000 / 8 = 3375
        conferir("desvio com quantidade", Estatistica.desvioPadrao(esperasSimulacao), Math.sqrt(3375));
        conferir("cor predominante (amarelo = 3)", Estatistica.moda(simulacao.getPrioridades()), lista(3));
        // verde: 150 min passa do máximo de 120 min; amarelo: 30 min está dentro dos 60 min
        conferir("pacientes estourados (3 verdes)", simulacao.getPacientesEstourados(), 3);
        conferir("pacientes com prazo", simulacao.getPacientesComPrazo(), 8);
        conferir("pacientes no fast track", simulacao.getPacientesFastTrack(), 3);
        conferir("pacientes na fila principal", simulacao.getPacientesNaFila("Principal"), 5);
        // faixas: 0-15 | 15-30 | 30-60 | 60-120 | 120-240 | 240+
        // 30 min fica em "30 a 60" (o limite 30 já foi atingido); 150 min fica em "120 a 240"
        int[] faixas = simulacao.getPacientesPorFaixa();
        conferir("faixa 30 a 60 min (5 amarelos)", faixas[2], 5);
        conferir("faixa 120 a 240 min (3 verdes)", faixas[4], 3);
        conferir("faixas somam 8 pacientes", faixas[0] + faixas[1] + faixas[2] + faixas[3] + faixas[4] + faixas[5], 8);

        System.out.println();
        if (falhas == 0) {
            System.out.println("Todos os cálculos conferem.");
        } else {
            System.out.println(falhas + " cálculo(s) com resultado errado.");
            System.exit(1);
        }
    }

    static ArrayList<Double> lista(double... valores) {
        ArrayList<Double> lista = new ArrayList<>();
        for (double v : valores) {
            lista.add(v);
        }
        return lista;
    }

    static void conferir(String nome, double obtido, double esperado) {
        boolean ok = Math.abs(obtido - esperado) < 0.000000001;
        mostrar(nome, ok, String.valueOf(obtido), String.valueOf(esperado));
    }

    static void conferir(String nome, ArrayList<Double> obtido, ArrayList<Double> esperado) {
        mostrar(nome, obtido.equals(esperado), obtido.toString(), esperado.toString());
    }

    static void mostrar(String nome, boolean ok, String obtido, String esperado) {
        if (!ok) {
            falhas++;
        }
        String resultado = ok ? "OK" : "FALHOU";
        System.out.printf("%-8s %-34s obtido %s, esperado %s%n", resultado, nome, obtido, esperado);
    }
}
