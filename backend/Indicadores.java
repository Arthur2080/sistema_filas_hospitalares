import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

/**
 * Aplica as regras do sistema de filas (projeto filas_hospitalares_java) aos dados de teste
 * e calcula os indicadores do dashboard para um horário "agora" do relógio simulado.
 */
public class Indicadores {

    static final List<String> DESFECHOS = List.of("ALTA", "INTERNACAO", "TRANSFERENCIA", "EVASAO", "OBITO");
    static final List<String> FILAS = List.of("IMEDIATA", "PRINCIPAL", "FAST_TRACK");

    // ---------------------------------------------------------------- regras de negócio

    static Dados.CorRisco cor(int idCor) {
        for (Dados.CorRisco c : Dados.CORES) if (c.id() == idCor) return c;
        throw new IllegalArgumentException("cor de risco inexistente: " + idCor);
    }

    static Dados.Atendimento atendimento(int id) {
        for (Dados.Atendimento a : Dados.ATENDIMENTOS) if (a.id() == id) return a;
        return null;
    }

    static Dados.Classificacao classificacao(int id) {
        for (Dados.Classificacao c : Dados.CLASSIFICACOES) if (c.id() == id) return c;
        return null;
    }

    static String nomePessoa(int idPessoa) {
        for (Dados.Pessoa p : Dados.PESSOAS) if (p.id() == idPessoa) return p.nome();
        return "?";
    }

    static Dados.Profissional profissional(int id) {
        for (Dados.Profissional p : Dados.PROFISSIONAIS) if (p.id() == id) return p;
        return null;
    }

    static String nomeLocal(int id) {
        for (Dados.Local l : Dados.LOCAIS) if (l.id() == id) return l.nome();
        return "?";
    }

    // Nenhuma classificação é apagada: cada reavaliação é um registro novo e vale a mais recente.
    static List<Dados.Classificacao> classificacoes(Dados.Atendimento a) {
        List<Dados.Classificacao> l = new ArrayList<>();
        for (Dados.Classificacao c : Dados.CLASSIFICACOES) if (c.idAtendimento() == a.id()) l.add(c);
        l.sort(Comparator.comparing(Dados.Classificacao::dt));
        return l;
    }

    static Dados.Classificacao classificacaoAtual(Dados.Atendimento a) {
        List<Dados.Classificacao> l = classificacoes(a);
        return l.isEmpty() ? null : l.get(l.size() - 1);
    }

    static Dados.CorRisco corAtual(Dados.Atendimento a) {
        Dados.Classificacao c = classificacaoAtual(a);
        return c == null ? null : cor(c.idCor());
    }

    static List<Dados.Prescricao> prescricoes(Dados.Atendimento a) {
        List<Dados.Prescricao> l = new ArrayList<>();
        for (Dados.Prescricao p : Dados.PRESCRICOES) if (p.idAtendimento() == a.id()) l.add(p);
        l.sort(Comparator.comparing(Dados.Prescricao::inicio));
        return l;
    }

    static List<Dados.Chamada> chamadas(Dados.Atendimento a) {
        List<Dados.Chamada> l = new ArrayList<>();
        for (Dados.Chamada c : Dados.CHAMADAS) if (c.idAtendimento() == a.id()) l.add(c);
        l.sort(Comparator.comparing(Dados.Chamada::dt));
        return l;
    }

    static Dados.Alerta alertaDa(Dados.Classificacao c) {
        for (Dados.Alerta al : Dados.ALERTAS) if (al.idClassificacao() == c.id()) return al;
        return null;
    }

    // AlertaPrazo.verificar: um alerta agendado dispara quando o relógio atinge o limite
    // e o paciente continua aguardando consulta. O sistema avisa, mas não muda a cor sozinho.
    static String statusAlerta(Dados.Alerta al, LocalDateTime agora) {
        Dados.Classificacao c = classificacao(al.idClassificacao());
        Dados.Atendimento a = atendimento(c.idAtendimento());
        boolean aguarda = a.status().equals("AGUARDANDO_CONSULTA") && classificacaoAtual(a) == c;
        if (al.status().equals("AGENDADO") && !agora.isBefore(al.limite()) && aguarda) return "DISPARADO";
        return al.status();
    }

    static LocalDateTime disparoAlerta(Dados.Alerta al, LocalDateTime agora) {
        if (al.disparo() != null) return al.disparo();
        return statusAlerta(al, agora).equals("DISPARADO") ? agora : null;
    }

    static boolean alertaAberto(String status) {
        return status.equals("AGENDADO") || status.equals("DISPARADO");
    }

    static Double minutos(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) return null;
        return (double) Duration.between(inicio, fim).toMinutes();
    }

    // Atendimento.tempoPortaClassificacao: da chegada até a primeira classificação.
    static Double portaClassificacao(Dados.Atendimento a) {
        List<Dados.Classificacao> l = classificacoes(a);
        return l.isEmpty() ? null : minutos(a.chegada(), l.get(0).dt());
    }

    // Atendimento.tempoPortaMedico: da chegada até o início da primeira prescrição.
    static Double portaMedico(Dados.Atendimento a) {
        List<Dados.Prescricao> l = prescricoes(a);
        return l.isEmpty() ? null : minutos(a.chegada(), l.get(0).inicio());
    }

    // Classificação que estava valendo quando o médico iniciou o atendimento (é dela o prazo que conta).
    static Dados.Classificacao classificacaoNoMedico(Dados.Atendimento a) {
        List<Dados.Prescricao> pr = prescricoes(a);
        if (pr.isEmpty()) return null;
        Dados.Classificacao vigente = null;
        for (Dados.Classificacao c : classificacoes(a)) if (!c.dt().isAfter(pr.get(0).inicio())) vigente = c;
        return vigente;
    }

    // FilaAtendimento.filaTriagem: antes da triagem, a fila é por ordem de chegada.
    static List<Dados.Atendimento> filaTriagem() {
        List<Dados.Atendimento> l = new ArrayList<>();
        for (Dados.Atendimento a : Dados.ATENDIMENTOS) if (a.status().equals("AGUARDANDO_TRIAGEM")) l.add(a);
        l.sort(Comparator.comparing(Dados.Atendimento::chegada));
        return l;
    }

    // FilaAtendimento.filaMedica: pela prioridade da cor e, empatando, por quem chegou primeiro.
    static List<Dados.Atendimento> filaMedica(String fila) {
        List<Dados.Atendimento> l = new ArrayList<>();
        for (Dados.Atendimento a : Dados.ATENDIMENTOS) {
            Dados.CorRisco c = corAtual(a);
            if (a.status().equals("AGUARDANDO_CONSULTA") && c != null && c.fila().equals(fila)) l.add(a);
        }
        l.sort(Comparator.comparing((Dados.Atendimento a) -> corAtual(a).prioridade()).thenComparing(Dados.Atendimento::chegada));
        return l;
    }

    // ---------------------------------------------------------------- indicadores

    static String painel(LocalDateTime agora) {
        return obj("agora", dt(agora),
                "cores", cores(),
                "atendimentos", atendimentos(agora),
                "alertas", alertas(agora),
                "filaTriagem", ids(filaTriagem()),
                "filas", obj("IMEDIATA", ids(filaMedica("IMEDIATA")),
                        "PRINCIPAL", ids(filaMedica("PRINCIPAL")),
                        "FAST_TRACK", ids(filaMedica("FAST_TRACK"))),
                "indicadores", indicadores(agora));
    }

    static String indicadores(LocalDateTime agora) {
        List<Double> tPortaClass = new ArrayList<>(), tPortaMedico = new ArrayList<>(),
                tClassMedico = new ArrayList<>(), tPermanencia = new ArrayList<>();
        List<String> serieMedico = new ArrayList<>();
        Map<String, Integer> status = new HashMap<>(), desfechos = new HashMap<>(), porFila = new HashMap<>();
        List<Double> coresAtuais = new ArrayList<>(), horas = new ArrayList<>();
        Map<Integer, List<Double>> classMedicoPorCor = new TreeMap<>();

        for (Dados.Atendimento a : Dados.ATENDIMENTOS) {
            status.merge(a.status(), 1, Integer::sum);
            horas.add((double) a.chegada().getHour());
            if (a.desfecho() != null) desfechos.merge(a.desfecho(), 1, Integer::sum);

            Double pc = portaClassificacao(a), pm = portaMedico(a);
            if (pc != null) tPortaClass.add(pc);
            if (pm != null) {
                tPortaMedico.add(pm);
                serieMedico.add(a.senha());
            }
            if (a.dtDesfecho() != null) tPermanencia.add(minutos(a.chegada(), a.dtDesfecho()));

            Dados.Classificacao noMedico = classificacaoNoMedico(a);
            if (noMedico != null) {
                double espera = minutos(noMedico.dt(), prescricoes(a).get(0).inicio());
                tClassMedico.add(espera);
                classMedicoPorCor.computeIfAbsent(cor(noMedico.idCor()).prioridade(), k -> new ArrayList<>()).add(espera);
            }

            Dados.CorRisco c = corAtual(a);
            if (c != null) {
                coresAtuais.add((double) c.prioridade());
                porFila.merge(c.fila(), 1, Integer::sum);
            }
        }

        // Prazos: todo alerta fica guardado e vira dado de qualidade. Estourou = foi disparado (AlertaPrazo.foiDisparado).
        int avaliados = 0, estourados = 0, emAndamento = 0;
        for (Dados.Alerta al : Dados.ALERTAS) {
            String st = statusAlerta(al, agora);
            if (st.equals("AGENDADO")) {
                emAndamento++;
                continue;
            }
            avaliados++;
            if (disparoAlerta(al, agora) != null) estourados++;
        }

        Map<Double, Integer> freqCores = Estatistica.frequencias(coresAtuais);
        Map<Double, Integer> freqHoras = Estatistica.frequencias(horas);
        // Chegadas em cada hora, da primeira à última, contando as horas sem nenhuma chegada.
        List<Double> chegadasHora = new ArrayList<>();
        if (!horas.isEmpty()) {
            for (int h = (int) Estatistica.minimo(horas); h <= (int) Estatistica.maximo(horas); h++) {
                chegadasHora.add((double) freqHoras.getOrDefault((double) h, 0));
            }
        }

        List<String> porCor = new ArrayList<>(), classMedicoCor = new ArrayList<>();
        for (Dados.CorRisco c : Dados.CORES) {
            porCor.add(obj("prioridade", "" + c.prioridade(), "pacientes", "" + freqCores.getOrDefault((double) c.prioridade(), 0)));
            List<Double> l = classMedicoPorCor.getOrDefault(c.prioridade(), List.of());
            classMedicoCor.add(obj("prioridade", "" + c.prioridade(), "quantidade", "" + l.size(),
                    "media", l.isEmpty() ? "null" : num(Estatistica.media(l))));
        }

        List<String> evolucao = new ArrayList<>();
        List<Double> acumulada = Estatistica.mediaAcumulada(tPortaMedico);
        for (int i = 0; i < tPortaMedico.size(); i++) {
            evolucao.add(obj("senha", str(serieMedico.get(i)), "valor", num(tPortaMedico.get(i)),
                    "media", num(acumulada.get(i))));
        }

        List<String> listaDesfechos = new ArrayList<>();
        for (String d : DESFECHOS) listaDesfechos.add(obj("tipo", str(d), "quantidade", "" + desfechos.getOrDefault(d, 0)));

        int finalizados = status.getOrDefault("FINALIZADO", 0), evasoes = desfechos.getOrDefault("EVASAO", 0);
        int semComparecer = 0;
        for (Dados.Chamada c : Dados.CHAMADAS) if (!c.compareceu()) semComparecer++;
        int classificados = coresAtuais.size();

        return obj(
                "totais", obj("atendimentos", "" + Dados.ATENDIMENTOS.size(),
                        "triagem", "" + filaTriagem().size(),
                        "imediata", "" + filaMedica("IMEDIATA").size(),
                        "principal", "" + filaMedica("PRINCIPAL").size(),
                        "fastTrack", "" + filaMedica("FAST_TRACK").size(),
                        "emConsulta", "" + status.getOrDefault("EM_CONSULTA", 0),
                        "aguardandoExame", "" + status.getOrDefault("AGUARDANDO_EXAME", 0),
                        "observacao", "" + status.getOrDefault("EM_OBSERVACAO", 0),
                        "finalizados", "" + finalizados),
                "portaClassificacao", resumo(tPortaClass),
                "portaMedico", resumo(tPortaMedico),
                "classificacaoMedico", resumo(tClassMedico),
                "permanencia", resumo(tPermanencia),
                "evolucaoPortaMedico", arr(evolucao),
                "prazos", obj("avaliados", "" + avaliados, "estourados", "" + estourados,
                        "noPrazo", "" + (avaliados - estourados), "emAndamento", "" + emAndamento,
                        "percentualNoPrazo", avaliados == 0 ? "null" : num(100.0 * (avaliados - estourados) / avaliados)),
                "porCor", arr(porCor),
                "corPredominante", lista(Estatistica.moda(coresAtuais)),
                "classificacaoMedicoPorCor", arr(classMedicoCor),
                "porFila", obj("IMEDIATA", "" + porFila.getOrDefault("IMEDIATA", 0),
                        "PRINCIPAL", "" + porFila.getOrDefault("PRINCIPAL", 0),
                        "FAST_TRACK", "" + porFila.getOrDefault("FAST_TRACK", 0)),
                "percentualFastTrack", classificados == 0 ? "null"
                        : num(100.0 * porFila.getOrDefault("FAST_TRACK", 0) / classificados),
                "chegadasPorHora", frequenciasJson(freqHoras),
                "picoChegadas", lista(Estatistica.moda(horas)),
                "mediaChegadasHora", chegadasHora.isEmpty() ? "null" : num(Estatistica.media(chegadasHora)),
                "desfechos", arr(listaDesfechos),
                "evasoes", "" + evasoes,
                "taxaEvasao", finalizados == 0 ? "null" : num(100.0 * evasoes / finalizados),
                "chamadas", "" + Dados.CHAMADAS.size(),
                "chamadasSemComparecimento", "" + semComparecer);
    }

    // ---------------------------------------------------------------- listas detalhadas

    static String cores() {
        List<String> l = new ArrayList<>();
        for (Dados.CorRisco c : Dados.CORES) {
            l.add(obj("prioridade", "" + c.prioridade(), "nome", str(c.nome()),
                    "tempoMax", "" + c.tempoMaxMin(), "fila", str(c.fila())));
        }
        return arr(l);
    }

    static String atendimentos(LocalDateTime agora) {
        List<String> l = new ArrayList<>();
        for (Dados.Atendimento a : Dados.ATENDIMENTOS) {
            Dados.Classificacao atual = classificacaoAtual(a);
            Dados.Classificacao noMedico = classificacaoNoMedico(a);
            Dados.Pessoa p = null;
            for (Dados.Pessoa x : Dados.PESSOAS) if (x.id() == a.idPaciente()) p = x;
            boolean aguardando = a.status().equals("AGUARDANDO_TRIAGEM") || a.status().equals("AGUARDANDO_CONSULTA");

            String alerta = "null";
            Dados.Alerta al = atual == null ? null : alertaDa(atual);
            if (al != null && alertaAberto(statusAlerta(al, agora))) {
                alerta = obj("id", "" + al.id(), "limite", dt(al.limite()), "status", str(statusAlerta(al, agora)),
                        "restante", n(minutos(agora, al.limite())));
            }

            l.add(obj("id", "" + a.id(),
                    "senha", str(a.senha()),
                    "paciente", str(p.nome()),
                    "idade", p.nascimento() == null ? "null" : "" + Period.between(p.nascimento(), agora.toLocalDate()).getYears(),
                    "chegada", dt(a.chegada()),
                    "status", str(a.status()),
                    "desfecho", str(a.desfecho()),
                    "dtDesfecho", dt(a.dtDesfecho()),
                    "encaminhamento", str(a.encaminhamento()),
                    "cor", atual == null ? "null" : "" + cor(atual.idCor()).prioridade(),
                    "queixa", str(atual == null ? null : atual.queixa()),
                    "portaClassificacao", n(portaClassificacao(a)),
                    "portaMedico", n(portaMedico(a)),
                    "corNoMedico", noMedico == null ? "null" : "" + cor(noMedico.idCor()).prioridade(),
                    "classificacaoMedico", noMedico == null ? "null" : n(minutos(noMedico.dt(), prescricoes(a).get(0).inicio())),
                    "permanencia", n(minutos(a.chegada(), a.dtDesfecho())),
                    "esperaAtual", aguardando && !agora.isBefore(a.chegada()) ? n(minutos(a.chegada(), agora)) : "null",
                    "alerta", alerta,
                    "classificacoes", classificacoesJson(a),
                    "chamadas", chamadasJson(a),
                    "prescricoes", prescricoesJson(a)));
        }
        return arr(l);
    }

    static String classificacoesJson(Dados.Atendimento a) {
        List<String> l = new ArrayList<>();
        for (Dados.Classificacao c : classificacoes(a)) {
            l.add(obj("id", "" + c.id(), "cor", "" + cor(c.idCor()).prioridade(), "tipo", str(c.tipo()),
                    "motivo", str(c.motivo()), "dt", dt(c.dt()), "queixa", str(c.queixa()),
                    "enfermeiro", str(nomePessoa(profissional(c.idEnfermeiro()).idPessoa())),
                    "paSistolica", n(c.paSistolica()), "paDiastolica", n(c.paDiastolica()),
                    "freqCardiaca", n(c.freqCardiaca()), "temperatura", n(c.temperatura()),
                    "saturacaoO2", n(c.saturacaoO2()), "escalaDor", n(c.escalaDor())));
        }
        return arr(l);
    }

    static String chamadasJson(Dados.Atendimento a) {
        List<String> l = new ArrayList<>();
        for (Dados.Chamada c : chamadas(a)) {
            Dados.Profissional p = profissional(c.idProfissional());
            l.add(obj("dt", dt(c.dt()), "local", str(nomeLocal(c.idLocal())),
                    "profissional", str(nomePessoa(p.idPessoa())), "cargo", str(p.cargo()),
                    "compareceu", "" + c.compareceu()));
        }
        return arr(l);
    }

    static String prescricoesJson(Dados.Atendimento a) {
        List<String> l = new ArrayList<>();
        for (Dados.Prescricao p : prescricoes(a)) {
            List<String> exames = new ArrayList<>();
            for (Dados.Exame e : Dados.EXAMES) {
                if (e.idPrescricao() != p.id()) continue;
                exames.add(obj("tipo", str(e.tipo()), "status", str(e.status()), "solicitacao", dt(e.solicitacao()),
                        "dtResultado", dt(e.dtResultado()), "visualizacao", dt(e.visualizacao()), "resultado", str(e.resultado())));
            }
            l.add(obj("id", "" + p.id(), "medico", str(nomePessoa(profissional(p.idMedico()).idPessoa())),
                    "inicio", dt(p.inicio()), "fim", dt(p.fim()), "anamnese", str(p.anamnese()), "exames", arr(exames)));
        }
        return arr(l);
    }

    static String alertas(LocalDateTime agora) {
        List<String> l = new ArrayList<>();
        for (Dados.Alerta al : Dados.ALERTAS) {
            Dados.Classificacao c = classificacao(al.idClassificacao());
            Dados.Atendimento a = atendimento(c.idAtendimento());
            l.add(obj("id", "" + al.id(), "atendimento", "" + a.id(), "senha", str(a.senha()),
                    "paciente", str(nomePessoa(a.idPaciente())), "cor", "" + cor(c.idCor()).prioridade(),
                    "classificacao", "" + c.id(), "tipo", str(c.tipo()), "limite", dt(al.limite()),
                    "disparo", dt(disparoAlerta(al, agora)), "resolucao", dt(al.resolucao()),
                    "reavaliacao", al.idReavaliacao() == null ? "null" : "" + al.idReavaliacao(),
                    "status", str(statusAlerta(al, agora))));
        }
        return arr(l);
    }

    // ---------------------------------------------------------------- JSON

    static String obj(String... chaveValor) {
        StringJoiner j = new StringJoiner(",", "{", "}");
        for (int i = 0; i < chaveValor.length; i += 2) j.add("\"" + chaveValor[i] + "\":" + chaveValor[i + 1]);
        return j.toString();
    }

    static String arr(List<String> itens) {
        return "[" + String.join(",", itens) + "]";
    }

    static String ids(List<Dados.Atendimento> l) {
        List<String> ids = new ArrayList<>();
        for (Dados.Atendimento a : l) ids.add("" + a.id());
        return arr(ids);
    }

    static String str(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static String dt(LocalDateTime t) {
        return t == null ? "null" : "\"" + t + "\"";
    }

    static String n(Number v) {
        return v == null ? "null" : num(v.doubleValue());
    }

    /** Número sem casas decimais quando é inteiro; senão, com até 4 casas. */
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

    /** Resumo de uma lista de tempos: quantidade, média, mediana, desvio padrão, mínimo e máximo. */
    static String resumo(List<Double> nums) {
        if (nums.isEmpty()) return "{\"quantidade\":0}";
        return obj("quantidade", "" + nums.size(),
                "media", num(Estatistica.media(nums)),
                "mediana", num(Estatistica.mediana(nums)),
                "desvioPadrao", num(Estatistica.desvioPadrao(nums)),
                "minimo", num(Estatistica.minimo(nums)),
                "maximo", num(Estatistica.maximo(nums)));
    }
}
