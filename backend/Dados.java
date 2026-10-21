import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dados de teste do pronto-socorro, iguais aos de filas_hospitalares_banco_2_dados.sql (06/10/2026).
 * Cada record espelha uma tabela do banco, só com os campos que o dashboard usa.
 */
public class Dados {

    static final LocalDateTime RELOGIO_INICIAL = t("2026-10-06 10:46");

    record CorRisco(int id, String nome, int prioridade, int tempoMaxMin, String fila) {}
    record Local(int id, String nome) {}
    record Pessoa(int id, String nome, LocalDate nascimento) {}
    record Profissional(int id, int idPessoa, String cargo) {}
    record Atendimento(int id, int idPaciente, String senha, LocalDateTime chegada, String status,
                       String desfecho, LocalDateTime dtDesfecho, String encaminhamento) {}
    record Classificacao(int id, int idAtendimento, int idEnfermeiro, int idCor, String tipo, String motivo,
                         LocalDateTime dt, String queixa, Integer paSistolica, Integer paDiastolica,
                         Integer freqCardiaca, Double temperatura, Integer saturacaoO2, Integer escalaDor) {}
    record Alerta(int id, int idClassificacao, Integer idReavaliacao, LocalDateTime limite,
                  LocalDateTime disparo, LocalDateTime resolucao, String status) {}
    record Chamada(int id, int idAtendimento, int idLocal, int idProfissional, LocalDateTime dt, boolean compareceu) {}
    record Prescricao(int id, int idAtendimento, int idMedico, LocalDateTime inicio, LocalDateTime fim, String anamnese) {}
    record Exame(int id, int idPrescricao, String tipo, String status, LocalDateTime solicitacao,
                 LocalDateTime dtResultado, LocalDateTime visualizacao, String resultado) {}

    static final List<CorRisco> CORES = List.of(
            new CorRisco(1, "Vermelho", 1, 0, "IMEDIATA"),
            new CorRisco(2, "Laranja", 2, 10, "PRINCIPAL"),
            new CorRisco(3, "Amarelo", 3, 60, "PRINCIPAL"),
            new CorRisco(4, "Verde", 4, 120, "FAST_TRACK"),
            new CorRisco(5, "Azul", 5, 240, "FAST_TRACK"));

    static final List<Local> LOCAIS = List.of(
            new Local(1, "Sala de triagem"),
            new Local(2, "Consultório 1"),
            new Local(3, "Consultório 2"),
            new Local(4, "Sala de emergência"));

    static final List<Pessoa> PESSOAS = List.of(
            new Pessoa(1, "Maria Souza", d("1980-04-12")),
            new Pessoa(2, "João Lima", d("1957-09-30")),
            new Pessoa(3, "Ana Pereira", d("2015-01-08")),
            new Pessoa(4, "Carla Mendes", d("1992-06-15")),
            new Pessoa(5, "Paulo Rocha", d("1985-11-02")),
            new Pessoa(6, "Helena Dias", d("1978-03-21")),
            new Pessoa(7, "Ricardo Alves", d("1983-08-09")),
            new Pessoa(8, "Fernanda Lopes", d("1990-02-17")),
            new Pessoa(9, "Lucas Martins", d("2001-07-23")),
            new Pessoa(10, "Beatriz Costa", d("1995-12-05")),
            new Pessoa(11, "Paciente não identificado", null),
            new Pessoa(12, "Roberto Nunes", d("1969-05-30")),
            new Pessoa(13, "Sônia Freitas", d("1975-10-11")));

    static final List<Profissional> PROFISSIONAIS = List.of(
            new Profissional(1, 4, "ATENDENTE"),
            new Profissional(2, 5, "ENFERMEIRO"),
            new Profissional(3, 6, "MEDICO"),
            new Profissional(4, 7, "MEDICO"),
            new Profissional(5, 8, "ENFERMEIRO"),
            new Profissional(6, 13, "ATENDENTE"));

    static final List<Atendimento> ATENDIMENTOS = List.of(
            new Atendimento(1, 1, "A001", t("2026-10-06 08:00"), "FINALIZADO", "ALTA", t("2026-10-06 11:00"), "UBS de referência"),
            new Atendimento(2, 2, "A002", t("2026-10-06 08:06"), "FINALIZADO", "INTERNACAO", t("2026-10-06 09:20"), "UTI coronariana"),
            new Atendimento(3, 3, "A003", t("2026-10-06 08:31"), "FINALIZADO", "EVASAO", t("2026-10-06 10:30"), null),
            new Atendimento(4, 9, "A004", t("2026-10-06 09:02"), "AGUARDANDO_EXAME", null, null, null),
            new Atendimento(5, 10, "A005", t("2026-10-06 09:31"), "AGUARDANDO_CONSULTA", null, null, null),
            new Atendimento(6, 8, "A006", t("2026-10-06 10:02"), "AGUARDANDO_CONSULTA", null, null, null),
            new Atendimento(7, 11, "A007", t("2026-10-06 10:31"), "EM_OBSERVACAO", null, null, null),
            new Atendimento(8, 12, "A008", t("2026-10-06 10:42"), "AGUARDANDO_TRIAGEM", null, null, null));

    static final List<Classificacao> CLASSIFICACOES = List.of(
            new Classificacao(1, 1, 2, 3, "INICIAL", null, t("2026-10-06 08:10"), "Dor abdominal em fossa ilíaca direita há 12 horas", 128, 82, 96, 37.6, 98, 6),
            new Classificacao(2, 1, 2, 2, "REAVALIACAO", null, t("2026-10-06 09:20"), "Piora da dor abdominal durante a espera", 135, 88, 112, 38.2, 97, 8),
            new Classificacao(3, 2, 2, 1, "INICIAL", null, t("2026-10-06 08:08"), "Dor torácica em aperto irradiando para braço esquerdo", 160, 98, 118, 36.8, 93, 9),
            new Classificacao(4, 3, 5, 4, "INICIAL", null, t("2026-10-06 08:40"), "Tosse e coriza há 2 dias", 105, 65, 98, 37.4, 98, 2),
            new Classificacao(5, 4, 2, 3, "INICIAL", null, t("2026-10-06 09:12"), "Tosse produtiva e febre há 3 dias", 122, 78, 104, 38.9, 94, 4),
            new Classificacao(6, 5, 5, 4, "INICIAL", null, t("2026-10-06 09:40"), "Dor lombar após esforço", 118, 76, 84, 36.5, 99, 5),
            new Classificacao(7, 6, 2, 2, "INICIAL", null, t("2026-10-06 10:10"), "Crise de falta de ar com chiado no peito", 138, 86, 120, 36.9, 91, 5),
            new Classificacao(8, 7, 2, 1, "INICIAL", null, t("2026-10-06 10:33"), "Trazido pelo SAMU com rebaixamento do nível de consciência", 90, 55, 130, 35.8, 88, null),
            new Classificacao(9, 5, 5, 5, "REAVALIACAO", "Dor cedeu após repouso; sinais vitais estáveis", t("2026-10-06 10:30"), "Dor lombar após esforço", 116, 74, 78, 36.5, 99, 2));

    static final List<Alerta> ALERTAS = List.of(
            new Alerta(1, 1, 2, t("2026-10-06 09:10"), t("2026-10-06 09:15"), t("2026-10-06 09:20"), "RESOLVIDO"),
            new Alerta(2, 2, null, t("2026-10-06 09:30"), null, null, "CANCELADO"),
            new Alerta(3, 4, null, t("2026-10-06 10:40"), null, null, "CANCELADO"),
            new Alerta(4, 5, null, t("2026-10-06 10:12"), null, null, "CANCELADO"),
            new Alerta(5, 6, null, t("2026-10-06 11:40"), null, null, "CANCELADO"),
            new Alerta(6, 7, null, t("2026-10-06 10:20"), t("2026-10-06 10:25"), null, "DISPARADO"),
            new Alerta(7, 9, null, t("2026-10-06 14:30"), null, null, "AGENDADO"));

    static final List<Chamada> CHAMADAS = List.of(
            new Chamada(1, 2, 4, 4, t("2026-10-06 08:09"), true),
            new Chamada(2, 1, 2, 3, t("2026-10-06 09:25"), true),
            new Chamada(3, 4, 3, 4, t("2026-10-06 09:50"), true),
            new Chamada(4, 3, 3, 3, t("2026-10-06 10:10"), false),
            new Chamada(5, 3, 3, 3, t("2026-10-06 10:20"), false),
            new Chamada(6, 7, 4, 4, t("2026-10-06 10:34"), true),
            new Chamada(7, 1, 2, 3, t("2026-10-06 10:44"), true),
            new Chamada(8, 8, 1, 2, t("2026-10-06 10:45"), true));

    static final List<Prescricao> PRESCRICOES = List.of(
            new Prescricao(1, 2, 4, t("2026-10-06 08:10"), t("2026-10-06 09:20"), "Dor torácica em aperto há 1 hora, irradiando para o braço esquerdo. Hipertenso."),
            new Prescricao(2, 1, 3, t("2026-10-06 09:26"), t("2026-10-06 09:40"), "Dor em fossa ilíaca direita há 12 horas, com náusea. Solicitado hemograma."),
            new Prescricao(3, 4, 4, t("2026-10-06 09:51"), null, "Tosse produtiva e febre há 3 dias. Estertores em base direita."),
            new Prescricao(4, 1, 3, t("2026-10-06 10:45"), t("2026-10-06 11:00"), "Retorno com hemograma sem alterações e dor em melhora. Orientada."),
            new Prescricao(5, 7, 4, t("2026-10-06 10:35"), t("2026-10-06 10:50"), "Rebaixamento do nível de consciência, glicemia normal. Solicitada tomografia."));

    static final List<Exame> EXAMES = List.of(
            new Exame(1, 1, "Eletrocardiograma", "VISUALIZADO", t("2026-10-06 08:12"), t("2026-10-06 08:20"), t("2026-10-06 08:22"), "Supradesnivelamento de ST em parede inferior"),
            new Exame(2, 1, "Troponina", "VISUALIZADO", t("2026-10-06 08:12"), t("2026-10-06 09:10"), t("2026-10-06 09:15"), "Troponina elevada"),
            new Exame(3, 2, "Hemograma", "VISUALIZADO", t("2026-10-06 09:35"), t("2026-10-06 10:30"), t("2026-10-06 10:46"), "Sem alterações significativas"),
            new Exame(4, 3, "Raio-X de tórax", "SOLICITADO", t("2026-10-06 09:55"), null, null, null),
            new Exame(5, 5, "Tomografia de crânio", "PRONTO", t("2026-10-06 10:40"), t("2026-10-06 10:48"), null, "Sem sinais de sangramento"));

    static LocalDateTime t(String texto) {
        return LocalDateTime.parse(texto.replace(' ', 'T'));
    }

    static LocalDate d(String texto) {
        return LocalDate.parse(texto);
    }
}
