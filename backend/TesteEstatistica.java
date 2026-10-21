import java.util.List;

/**
 * Confere os cálculos da classe Estatistica com resultados feitos à mão.
 *
 *   java backend/TesteEstatistica.java
 *
 * Os dados principais são os tempos porta–médico (minutos) dos dados de teste, na ordem dos atendimentos:
 *   A001 08:00 → 09:26 = 86 | A002 08:06 → 08:10 = 4 | A004 09:02 → 09:51 = 49 | A007 10:31 → 10:35 = 4
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
        // 86 | (86+4)/2 = 45 | (90+49)/3 = 46,33 | 143/4 = 35,75
        conferir("média acumulada", Estatistica.mediaAcumulada(portaMedico), List.of(86.0, 45.0, 139.0 / 3, 35.75));

        // Casos de borda
        conferir("mediana (quantidade ímpar)", Estatistica.mediana(List.of(30.0, 10.0, 20.0)), 20);
        conferir("mediana (um valor)", Estatistica.mediana(List.of(7.0)), 7);
        conferir("desvio padrão (valores iguais)", Estatistica.desvioPadrao(List.of(5.0, 5.0, 5.0)), 0);
        conferir("moda (sem repetição = amodal)", Estatistica.moda(List.of(1.0, 2.0, 3.0)), List.of());
        conferir("moda (empate = bimodal)", Estatistica.moda(List.of(2.0, 2.0, 4.0, 4.0, 5.0)), List.of(2.0, 4.0));
        // prioridades das cores atuais: amarelo (3) aparece mais
        conferir("moda das cores", Estatistica.moda(List.of(3.0, 4.0, 3.0, 2.0, 3.0)), List.of(3.0));

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
