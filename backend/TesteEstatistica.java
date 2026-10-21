import java.util.List;

/**
 * Confere os cálculos da classe Estatistica com resultados feitos à mão.
 *
 *   java backend/TesteEstatistica.java
 *
 * Os primeiros casos usam quatro esperas até o médico, em minutos: 86, 4, 49 e 4.
 * Os últimos usam o simulador, com grupos de pacientes (cor, quantidade e espera).
 */
public class TesteEstatistica {

    static int falhas = 0;

    public static void main(String[] args) {
        List<Double> portaMedico = List.of(86.0, 4.0, 49.0, 4.0);

        // soma = 86 + 4 + 49 + 4 = 143; média = 143 / 4 = 35,75
        conferir("soma", Estatistica.soma(portaMedico), 143);
        conferir("média", Estatistica.media(portaMedico), 35.75);
        // ordenada: [4, 4, 49, 86]; par → (4 + 49) / 2 = 26,5
        conferir("mediana (quantidade par)", Estatistica.mediana(portaMedico), 26.5);
        // distâncias até 35,75: 50,25 | -31,75 | 13,25 | -31,75
        // quadrados: 2525,0625 + 1008,0625 + 175,5625 + 1008,0625 = 4716,75 → variância 4716,75 / 4 = 1179,1875
        conferir("desvio padrão", Estatistica.desvioPadrao(portaMedico), Math.sqrt(1179.1875));
        conferir("mínimo", Estatistica.minimo(portaMedico), 4);
        conferir("máximo", Estatistica.maximo(portaMedico), 86);
        // o 4 aparece duas vezes, os outros uma
        conferir("moda", Estatistica.moda(portaMedico), List.of(4.0));

        // Casos de borda
        conferir("mediana (quantidade ímpar)", Estatistica.mediana(List.of(30.0, 10.0, 20.0)), 20);
        conferir("mediana (um valor)", Estatistica.mediana(List.of(7.0)), 7);
        conferir("desvio padrão (valores iguais)", Estatistica.desvioPadrao(List.of(5.0, 5.0, 5.0)), 0);
        conferir("moda (sem repetição = amodal)", Estatistica.moda(List.of(1.0, 2.0, 3.0)), List.of());
        conferir("moda (empate = bimodal)", Estatistica.moda(List.of(2.0, 2.0, 4.0, 4.0, 5.0)), List.of(2.0, 4.0));
        // prioridades das cores atuais: amarelo (3) aparece mais
        conferir("moda das cores", Estatistica.moda(List.of(3.0, 4.0, 3.0, 2.0, 3.0)), List.of(3.0));

        // Simulador: 5 amarelos que esperaram 30 min + 3 verdes que esperaram 150 min = 8 pacientes
        List<Simulacao.Grupo> grupos = List.of(
                new Simulacao.Grupo(Simulacao.corPorPrioridade(3), 5, 30),
                new Simulacao.Grupo(Simulacao.corPorPrioridade(4), 3, 150));
        List<Double> esperas = Simulacao.esperasDosPacientes(grupos);
        conferir("grupos viram 8 pacientes", esperas, List.of(30.0, 30.0, 30.0, 30.0, 30.0, 150.0, 150.0, 150.0));
        // (5 × 30 + 3 × 150) / 8 = 600 / 8 = 75
        conferir("média com quantidade", Estatistica.media(esperas), 75);
        // 4º e 5º valores da lista ordenada: (30 + 30) / 2 = 30
        conferir("mediana com quantidade", Estatistica.mediana(esperas), 30);
        // 5 × (30 − 75)² + 3 × (150 − 75)² = 10125 + 16875 = 27000 → variância 27000 / 8 = 3375
        conferir("desvio com quantidade", Estatistica.desvioPadrao(esperas), Math.sqrt(3375));
        conferir("cor predominante (amarelo)", Estatistica.moda(Simulacao.prioridadesDosPacientes(grupos)), List.of(3.0));
        // verde: 150 min passa do máximo de 120 min; amarelo: 30 min está dentro dos 60 min
        conferir("verde com 150 min estourou", Simulacao.estourouPrazo(Simulacao.corPorPrioridade(4), 150) ? 1 : 0, 1);
        conferir("amarelo com 30 min no prazo", Simulacao.estourouPrazo(Simulacao.corPorPrioridade(3), 30) ? 1 : 0, 0);

        System.out.println();
        System.out.println(falhas == 0 ? "Todos os cálculos conferem." : falhas + " cálculo(s) com resultado errado.");
        if (falhas > 0) System.exit(1);
    }

    static void conferir(String nome, double obtido, double esperado) {
        boolean ok = Math.abs(obtido - esperado) < 1e-9;
        if (!ok) falhas++;
        System.out.printf("%-8s %-32s obtido %s, esperado %s%n", ok ? "OK" : "FALHOU", nome, obtido, esperado);
    }

    static void conferir(String nome, List<Double> obtido, List<Double> esperado) {
        boolean ok = obtido.size() == esperado.size();
        for (int i = 0; ok && i < obtido.size(); i++) {
            if (Math.abs(obtido.get(i) - esperado.get(i)) >= 1e-9) ok = false;
        }
        if (!ok) falhas++;
        System.out.printf("%-8s %-32s obtido %s, esperado %s%n", ok ? "OK" : "FALHOU", nome, obtido, esperado);
    }
}
