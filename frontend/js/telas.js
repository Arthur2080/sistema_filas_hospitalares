/* =========================================================================
   Telas do dashboard. Cada tela tem html(d) e, se tiver gráficos, graficos(d).
   "d" é o JSON de /api/painel: atendimentos, alertas, filas e indicadores.
   ========================================================================= */

const ROTULO = {
  AGUARDANDO_TRIAGEM: "Aguardando triagem", AGUARDANDO_CONSULTA: "Aguardando consulta", EM_CONSULTA: "Em consulta",
  AGUARDANDO_EXAME: "Aguardando exame", EM_OBSERVACAO: "Em observação", FINALIZADO: "Finalizado",
  IMEDIATA: "Emergência", PRINCIPAL: "Principal", FAST_TRACK: "Fast Track",
  ALTA: "Alta", INTERNACAO: "Internação", TRANSFERENCIA: "Transferência", EVASAO: "Evasão", OBITO: "Óbito",
  AGENDADO: "Agendado", DISPARADO: "Disparado", RESOLVIDO: "Resolvido", CANCELADO: "Cancelado",
  SOLICITADO: "Solicitado", PRONTO: "Pronto", VISUALIZADO: "Visualizado", INICIAL: "Inicial", REAVALIACAO: "Reavaliação",
  ATENDENTE: "Atendente", ENFERMEIRO: "Enfermeiro(a)", MEDICO: "Médico(a)"
};
const STATUS = ["AGUARDANDO_TRIAGEM", "AGUARDANDO_CONSULTA", "EM_CONSULTA", "AGUARDANDO_EXAME", "EM_OBSERVACAO", "FINALIZADO"];
const STATUS_ALERTA = ["AGENDADO", "DISPARADO", "RESOLVIDO", "CANCELADO"];
const COR_FILA = { IMEDIATA: 1, PRINCIPAL: 2, FAST_TRACK: 4 };

let CORES = [];
const ui = { filtroAlerta: "", filtroStatus: "" };

const rot = v => ROTULO[v] || v || "—";
const esc = v => String(v ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const p2 = n => String(n).padStart(2, "0");
const data = iso => new Date(iso);
const fmtH = iso => iso ? `${p2(data(iso).getHours())}:${p2(data(iso).getMinutes())}` : "—";
const fmtDH = iso => iso ? `${p2(data(iso).getDate())}/${p2(data(iso).getMonth() + 1)}/${data(iso).getFullYear()} ${fmtH(iso)}` : "—";

const corDe = p => CORES.find(c => c.prioridade === p);
const tagCor = p => p ? `<span class="cor c${p}">${esc(corDe(p).nome)}</span>` : `<span class="tag">Sem classificação</span>`;
const tag = v => `<span class="tag">${esc(rot(v))}</span>`;
const btnHistorico = (id, txt = "Histórico") => `<button class="btn peq" data-historico="${id}">${txt}</button>`;
const atendimentoPorId = (d, id) => d.atendimentos.find(a => a.id === id);

const cabecalho = (titulo, sub, extra = "") =>
  `<div class="cabecalho"><div><h1>${titulo}</h1>${sub ? `<p>${sub}</p>` : ""}</div>${extra}</div>`;
const kpi = (valor, texto, classe = "", sub = "") =>
  `<div class="kpi ${classe}"><b>${valor}</b><span>${texto}</span>${sub ? `<small>${sub}</small>` : ""}</div>`;
const destaque = (rotulo, valor, sub, ruim = false) =>
  `<div class="destaque ${ruim ? "ruim" : ""}"><span>${rotulo}</span><b>${valor}</b><small>${sub}</small></div>`;
const grafico = (id, titulo, legenda = "", nota = "") =>
  `<section class="painel"><h2>${titulo}${legenda}</h2><div id="${id}" class="grafico"></div>${nota ? `<p class="nota">${nota}</p>` : ""}</section>`;
const legendaMedia = `<span class="legenda"><span><i class="linha tracejada"></i>Média</span></span>`;
const legendaPrazo = `<span class="legenda"><span><i class="linha"></i>Tempo máximo da cor</span></span>`;

function tabela(colunas, linhas, vaziaTexto = "Nenhum registro.") {
  return `<div class="tabela"><table><thead><tr>${colunas.map(c => `<th class="${c.startsWith("#") ? "num" : ""}">${c.replace(/^#/, "")}</th>`).join("")}</tr></thead>
    <tbody>${linhas.length ? linhas.join("") : `<tr><td colspan="${colunas.length}" class="vazio">${vaziaTexto}</td></tr>`}</tbody></table></div>`;
}

// Situação do prazo de quem aguarda consulta (alerta aberto da classificação atual).
function situacaoPrazo(a) {
  if (a.cor === 1) return `<span class="prazo estourado">Atendimento imediato</span>`;
  if (!a.alerta) return `<span class="prazo">Sem alerta aberto</span>`;
  if (a.alerta.status === "DISPARADO") {
    return `<span class="prazo estourado pulsa">⚠ Estourado há ${duracao(a.alerta.restante)}</span>`;
  }
  return `<span class="prazo">Restam ${duracao(a.alerta.restante)} · limite ${fmtH(a.alerta.limite)}</span>`;
}

function painelResumo(titulo, descricao, r) {
  if (!r.quantidade) return `<section class="painel"><h2>${titulo}</h2><p class="vazio">Sem registros.</p></section>`;
  return `<section class="painel"><h2>${titulo}</h2>
    <div class="estat-hero"><span>Média</span><b>${duracao(r.media)}</b><span>${descricao}</span></div>
    <div class="estat">
      <div><span>Mediana</span><b>${duracao(r.mediana)}</b></div>
      <div><span>Desvio padrão</span><b>${duracao(r.desvioPadrao)}</b></div>
      <div><span>Atendimentos</span><b>${r.quantidade}</b></div>
      <div><span>Menor</span><b>${duracao(r.minimo)}</b></div>
      <div><span>Maior</span><b>${duracao(r.maximo)}</b></div>
    </div></section>`;
}

/* ------------------------------------------------------------------ telas */

const TELAS = {

  visao: {
    titulo: "Visão geral", grupo: "Indicadores",
    html(d) {
      const t = d.indicadores.totais, ind = d.indicadores, pr = ind.prazos;
      const disparados = d.alertas.filter(a => a.status === "DISPARADO");
      const porStatus = STATUS.map(s => ({
        rotulo: rot(s), valor: d.atendimentos.filter(a => a.status === s).length, cor: COR.primaria,
        tip: rot(s)
      }));
      return `${cabecalho("Visão geral", `Situação do pronto-socorro em ${fmtDH(d.agora)}`)}
      <div class="cards">
        ${kpi(t.atendimentos, "Atendimentos no dia")}
        ${kpi(t.triagem, "Aguardando triagem", "neutra")}
        ${kpi(t.imediata, "Fila Emergência", "vermelho")}
        ${kpi(t.principal, "Fila Principal", "laranja")}
        ${kpi(t.fastTrack, "Fila Fast Track", "verde")}
        ${kpi(t.emConsulta + t.aguardandoExame, "Em consulta ou exame", "azul")}
        ${kpi(t.observacao, "Em observação", "neutra")}
        ${kpi(t.finalizados, "Finalizados")}
      </div>
      <div class="destaques">
        ${destaque("Média porta–classificação", duracao(ind.portaClassificacao.media), "da chegada até a triagem")}
        ${destaque("Média porta–médico", duracao(ind.portaMedico.media), "da chegada até o início da consulta")}
        ${destaque("Atendidos no prazo", pct(pr.percentualNoPrazo), `${pr.noPrazo} de ${vezes(pr.avaliados, "alerta encerrado", "alertas encerrados")} sem estourar`)}
        ${destaque("Taxa de evasão", pct(ind.taxaEvasao), `${ind.evasoes} de ${t.finalizados} finalizados foram embora sem atendimento`, ind.taxaEvasao > 10)}
      </div>
      <section class="painel"><h2>Alertas disparados (${disparados.length}) <small>o sistema avisa a enfermagem, mas não muda a cor sozinho</small></h2>
        ${disparados.length ? disparados.map(al => `
          <div class="alerta-box pulsa">
            <div><strong><span class="senha">${esc(al.senha)}</span> · ${esc(al.paciente)}</strong> ${tagCor(al.cor)}
              <div class="meta">Limite ${fmtH(al.limite)} · disparado ${fmtH(al.disparo)} · estourado há ${duracao((data(d.agora) - data(al.limite)) / 60000)}</div></div>
            ${btnHistorico(al.atendimento, "Ver histórico")}
          </div>`).join("") : `<p class="vazio">Nenhum alerta disparado.</p>`}
      </section>
      <div class="grade-2">
        ${grafico("g-cores", "Pacientes por cor de risco", "", "Cor atual de cada atendimento classificado (vale a classificação mais recente).")}
        <section class="painel"><h2>Situação dos atendimentos</h2>${barrasH(porStatus)}</section>
      </div>`;
    },
    graficos(d) {
      const ind = d.indicadores;
      colunas(document.getElementById("g-cores"), ind.porCor, {
        valor: c => c.pacientes, cor: c => COR_RISCO[c.prioridade], eixo: c => corDe(c.prioridade).nome,
        rotulo: c => c.pacientes, inteiro: true,
        tip: c => esc(`${corDe(c.prioridade).nome} · fila ${rot(corDe(c.prioridade).fila)}: ${vezes(c.pacientes, "paciente", "pacientes")}`)
      });
    }
  },

  tempos: {
    titulo: "Tempos de espera", grupo: "Indicadores",
    html(d) {
      const ind = d.indicadores;
      return `${cabecalho("Tempos de espera", "Calculados com os horários que o sistema anota em cada etapa: chegada, triagem, consulta e saída.")}
      <div class="grade-3">
        ${painelResumo("Porta–classificação", "da chegada até a 1ª classificação de risco", ind.portaClassificacao)}
        ${painelResumo("Porta–médico", "da chegada até o início da 1ª consulta", ind.portaMedico)}
        ${painelResumo("Permanência", "da chegada até o desfecho", ind.permanencia)}
      </div>
      ${grafico("g-porta-medico", "Porta–médico por atendimento", legendaMedia, "Cada coluna tem a cor da classificação que valia quando o médico começou a consulta.")}
      <div class="grade-2">
        ${grafico("g-class-medico", "Classificação → médico por cor", legendaPrazo, "Espera depois da triagem, comparada ao tempo máximo da cor.")}
        ${grafico("g-evolucao", "Evolução da média porta–médico", "", "Média acumulada, atendimento a atendimento, na ordem de chegada.")}
      </div>`;
    },
    graficos(d) {
      const ind = d.indicadores;
      const atendidos = d.atendimentos.filter(a => a.portaMedico != null);
      colunas(document.getElementById("g-porta-medico"), atendidos, {
        valor: a => a.portaMedico, cor: a => COR_RISCO[a.corNoMedico], eixo: a => a.senha,
        rotulo: a => fmt(a.portaMedico), media: ind.portaMedico.media,
        tip: a => esc(`${a.senha} · ${a.paciente} (${corDe(a.corNoMedico).nome}): ${duracao(a.portaMedico)} até o médico`)
      });
      const porCor = ind.classificacaoMedicoPorCor.filter(c => c.quantidade > 0);
      colunas(document.getElementById("g-class-medico"), porCor, {
        valor: c => c.media, cor: c => COR_RISCO[c.prioridade], eixo: c => corDe(c.prioridade).nome,
        rotulo: c => fmt(c.media), prazo: c => corDe(c.prioridade).tempoMax,
        tip: c => esc(`${corDe(c.prioridade).nome}: média ${duracao(c.media)} em ${vezes(c.quantidade, "atendimento", "atendimentos")} · ` +
          (corDe(c.prioridade).tempoMax ? `prazo ${duracao(corDe(c.prioridade).tempoMax)}` : "atendimento imediato"))
      });
      linha(document.getElementById("g-evolucao"), ind.evolucaoPortaMedico.map((p, i) => ({
        eixo: p.senha, valor: p.media,
        tip: esc(`Após ${vezes(i + 1, "atendimento", "atendimentos")} (${p.senha}: ${duracao(p.valor)}): média ${duracao(p.media)}`)
      })), { rotuloFinal: duracao(ind.portaMedico.media) });
    }
  },

  prazos: {
    titulo: "Prazos e alertas", grupo: "Indicadores",
    html(d) {
      const pr = d.indicadores.prazos;
      const aguardando = ["IMEDIATA", "PRINCIPAL", "FAST_TRACK"].flatMap(f => d.filas[f].map(id => ({ fila: f, a: atendimentoPorId(d, id) })));
      const alertas = d.alertas.filter(a => !ui.filtroAlerta || a.status === ui.filtroAlerta).sort((a, b) => b.id - a.id);
      return `${cabecalho("Prazos e alertas", "Toda classificação, menos a vermelha, gera um alerta com o tempo máximo da cor. Ele dispara quando o prazo vence e o paciente ainda aguarda consulta.")}
      <div class="cards">
        ${kpi(pct(pr.percentualNoPrazo), "Atendidos no prazo", "verde sucesso", `${pr.noPrazo} de ${pr.avaliados} alertas encerrados`)}
        ${kpi(pr.estourados, "Prazos estourados", pr.estourados ? "vermelho alerta" : "verde", "alertas que dispararam")}
        ${kpi(pr.emAndamento, "Prazos em andamento", "neutra", "alertas ainda agendados")}
        ${kpi(d.alertas.length, "Alertas registrados", "", "ficam guardados para avaliar a qualidade")}
      </div>
      <section class="painel"><h2>Aguardando consulta agora <small>ordem da fila: prioridade da cor e, empatando, chegada</small></h2>
        ${tabela(["Fila", "Senha", "Paciente", "Cor", "Queixa", "#Espera", "Prazo", ""], aguardando.map(({ fila, a }) => `
          <tr><td>${rot(fila)}</td><td class="senha">${esc(a.senha)}</td><td>${esc(a.paciente)}</td><td>${tagCor(a.cor)}</td>
          <td class="meta">${esc(a.queixa)}</td><td class="num">${duracao(a.esperaAtual)}</td><td>${situacaoPrazo(a)}</td><td>${btnHistorico(a.id)}</td></tr>`),
          "Ninguém aguardando consulta.")}
      </section>
      <section class="painel"><h2>Histórico de alertas</h2>
        <div class="filtro"><div><label for="filtroAlerta">Filtrar por status</label>
          <select id="filtroAlerta"><option value="">Todos</option>${STATUS_ALERTA.map(s => `<option value="${s}" ${ui.filtroAlerta === s ? "selected" : ""}>${rot(s)}</option>`).join("")}</select></div></div>
        ${tabela(["#Alerta", "Atendimento", "Cor", "Classificação", "Limite", "Disparo", "Resolução", "Status"], alertas.map(al => `
          <tr class="${al.status === "DISPARADO" ? "disparado" : ""}"><td class="num">${al.id}</td>
          <td><button class="btn peq" data-historico="${al.atendimento}">${esc(al.senha)} · ${esc(al.paciente)}</button></td>
          <td>${tagCor(al.cor)}</td><td>#${al.classificacao} ${rot(al.tipo)}</td><td>${fmtH(al.limite)}</td><td>${fmtH(al.disparo)}</td>
          <td>${fmtH(al.resolucao)}${al.reavaliacao ? ` <span class="meta">(reavaliação #${al.reavaliacao})</span>` : ""}</td>
          <td>${al.status === "DISPARADO" ? `<span class="cor c1">Disparado</span>` : tag(al.status)}</td></tr>`), "Nenhum alerta com esse status.")}
      </section>`;
    }
  },

  fluxo: {
    titulo: "Fluxo de chegadas", grupo: "Indicadores",
    html(d) {
      const ind = d.indicadores, f = ind.porFila;
      const total = f.IMEDIATA + f.PRINCIPAL + f.FAST_TRACK;
      const triagem = d.filaTriagem.map(id => atendimentoPorId(d, id));
      const pico = ind.picoChegadas.length ? ind.picoChegadas.map(h => `${p2(h)}h`).join(" e ") : "—";
      return `${cabecalho("Fluxo de chegadas", "Quantas pessoas chegam por hora e para qual fila cada uma vai depois da triagem.")}
      <div class="cards">
        ${kpi(fmt(ind.mediaChegadasHora), "Chegadas por hora (média)")}
        ${kpi(pico, "Horário de pico", "laranja", "hora com mais chegadas")}
        ${kpi(pct(ind.percentualFastTrack), "Casos não urgentes", "verde", "verdes e azuis, no fast track")}
        ${kpi(ind.chamadasSemComparecimento, "Chamadas sem resposta", "neutra", `de ${ind.chamadas} chamadas no painel`)}
      </div>
      <div class="grade-2">
        ${grafico("g-chegadas", "Chegadas por hora", "", "Use para decidir quantos profissionais colocar em cada turno.")}
        <section class="painel"><h2>Pacientes por fila</h2>
          ${barrasH(["IMEDIATA", "PRINCIPAL", "FAST_TRACK"].map(k => ({
            rotulo: `<span class="cor c${COR_FILA[k]}">${rot(k)}</span>`, valor: f[k], cor: COR_RISCO[COR_FILA[k]],
            tip: esc(`${rot(k)}: ${vezes(f[k], "paciente", "pacientes")} (${pct(total ? 100 * f[k] / total : null)})`)
          })))}
          <p class="nota">Vermelho vai para a emergência, laranja e amarelo para a fila principal, verde e azul para o fast track.
          Em hospitais grandes, até 60% dos casos são não urgentes (SOARES).</p>
        </section>
      </div>
      <section class="painel"><h2>Fila de triagem <small>antes da triagem, a fila é por ordem de chegada</small></h2>
        ${tabela(["#", "Senha", "Paciente", "Chegada", "#Espera", "Chamadas", ""], triagem.map((a, i) => `
          <tr><td>${i + 1}º</td><td class="senha">${esc(a.senha)}</td><td>${esc(a.paciente)}</td><td>${fmtH(a.chegada)}</td>
          <td class="num">${duracao(a.esperaAtual)}</td>
          <td>${a.chamadas.length ? a.chamadas.map(c => `${fmtH(c.dt)} ${c.compareceu ? "✓" : "✗"}`).join(", ") : "—"}</td><td>${btnHistorico(a.id)}</td></tr>`),
          "Ninguém aguardando triagem.")}
      </section>`;
    },
    graficos(d) {
      const ind = d.indicadores, freq = ind.chegadasPorHora;
      const horas = [];
      if (freq.length) {
        for (let h = freq[0].valor; h <= freq.at(-1).valor; h++) {
          horas.push({ hora: h, quantidade: freq.find(f => f.valor === h)?.contagem || 0 });
        }
      }
      const pico = new Set(ind.picoChegadas);
      colunas(document.getElementById("g-chegadas"), horas, {
        valor: h => h.quantidade, cor: () => COR.primaria, eixo: h => `${p2(h.hora)}h`, inteiro: true,
        rotulo: h => pico.has(h.hora) ? `${h.quantidade} · pico` : h.quantidade,
        tip: h => esc(`${p2(h.hora)}h às ${p2(h.hora)}h59: ${vezes(h.quantidade, "chegada", "chegadas")}`)
      });
    }
  },

  desfechos: {
    titulo: "Desfechos", grupo: "Indicadores",
    html(d) {
      const ind = d.indicadores, q = t => ind.desfechos.find(x => x.tipo === t).quantidade;
      const finalizados = d.atendimentos.filter(a => a.desfecho).sort((a, b) => data(b.dtDesfecho) - data(a.dtDesfecho));
      return `${cabecalho("Desfechos", "Como cada atendimento terminou. Só o médico decide o destino; a evasão exige uma chamada sem comparecimento.")}
      <div class="cards">
        ${kpi(ind.totais.finalizados, "Atendimentos finalizados")}
        ${kpi(q("ALTA"), "Altas", "verde")}
        ${kpi(q("INTERNACAO") + q("TRANSFERENCIA"), "Internações e transferências", "azul")}
        ${kpi(ind.evasoes, "Evasões", ind.evasoes ? "vermelho alerta" : "verde", `${pct(ind.taxaEvasao)} dos finalizados`)}
      </div>
      <div class="grade-2">
        ${grafico("g-desfechos", "Atendimentos por desfecho")}
        ${painelResumo("Permanência no pronto-socorro", "da chegada até o desfecho", ind.permanencia)}
      </div>
      <section class="painel"><h2>Atendimentos finalizados</h2>
        ${tabela(["Senha", "Paciente", "Cor", "Chegada", "Desfecho", "Saída", "#Permanência", "Encaminhamento", ""], finalizados.map(a => `
          <tr><td class="senha">${esc(a.senha)}</td><td>${esc(a.paciente)}</td><td>${tagCor(a.cor)}</td><td>${fmtH(a.chegada)}</td>
          <td>${a.desfecho === "EVASAO" ? `<span class="cor c1">Evasão</span>` : tag(a.desfecho)}</td><td>${fmtH(a.dtDesfecho)}</td>
          <td class="num">${duracao(a.permanencia)}</td><td>${esc(a.encaminhamento || "—")}</td><td>${btnHistorico(a.id)}</td></tr>`), "Nenhum atendimento finalizado.")}
      </section>`;
    },
    graficos(d) {
      colunas(document.getElementById("g-desfechos"), d.indicadores.desfechos, {
        valor: x => x.quantidade, cor: () => COR.primaria, eixo: x => rot(x.tipo), rotulo: x => x.quantidade, inteiro: true,
        tip: x => esc(`${rot(x.tipo)}: ${vezes(x.quantidade, "atendimento", "atendimentos")}`)
      });
    }
  },

  atendimentos: {
    titulo: "Atendimentos", grupo: "Consulta",
    html(d) {
      const lista = d.atendimentos.filter(a => !ui.filtroStatus || a.status === ui.filtroStatus);
      return `${cabecalho("Atendimentos", "Clique em Histórico para ver a linha do tempo completa de cada paciente.")}
      <section class="painel">
        <div class="filtro"><div><label for="filtroStatus">Filtrar por situação</label>
          <select id="filtroStatus"><option value="">Todas</option>${STATUS.map(s => `<option value="${s}" ${ui.filtroStatus === s ? "selected" : ""}>${rot(s)}</option>`).join("")}</select></div></div>
        ${tabela(["#", "Senha", "Paciente", "#Idade", "Chegada", "Cor atual", "Situação", "#Porta–class.", "#Porta–médico", "Desfecho", ""],
          lista.map(a => `<tr><td>${a.id}</td><td class="senha">${esc(a.senha)}</td><td>${esc(a.paciente)}</td><td class="num">${a.idade ?? "—"}</td>
            <td>${fmtH(a.chegada)}</td><td>${tagCor(a.cor)}</td><td>${tag(a.status)}</td>
            <td class="num">${duracao(a.portaClassificacao)}</td><td class="num">${duracao(a.portaMedico)}</td>
            <td>${a.desfecho ? rot(a.desfecho) : "—"}</td><td>${btnHistorico(a.id)}</td></tr>`), "Nenhum atendimento nessa situação.")}
      </section>`;
    }
  },

  simulador: {
    titulo: "Simulador", grupo: "Ferramentas",
    html() {
      return `${cabecalho("Simulador de espera", "Escolha a cor, digite quanto tempo cada paciente esperou até o médico e veja os indicadores com as mesmas regras de prazo.")}
      <section class="painel">
        <label>Cor da classificação</label>
        <div class="seletor-cores" id="sim-cores">${CORES.map(c => `
          <button class="btn" data-sim-cor="${c.prioridade}" style="--sel:${COR_RISCO[c.prioridade]}">
            <span class="bola" style="background:${COR_RISCO[c.prioridade]}"></span>
            <span><b>${esc(c.nome)}</b><small>${c.tempoMax ? `até ${c.tempoMax} min` : "imediato"} · ${rot(c.fila)}</small></span>
          </button>`).join("")}</div>
        <div class="entrada">
          <div class="campo">
            <label for="sim-espera">Espera até o médico (minutos)</label>
            <input id="sim-espera" inputmode="decimal" autocomplete="off" placeholder="Ex: 45">
            <div class="progresso"><div id="sim-progresso"></div></div>
          </div>
          <div style="align-self:flex-end;display:flex;gap:8px">
            <button class="btn" id="sim-exemplo">Gerar plantão de exemplo</button>
            <button class="btn perigo" id="sim-limpar">Limpar</button>
          </div>
        </div>
        <p class="dica">Enter salva na hora · espaço ou ; separam várias esperas da mesma cor · clique em um paciente para removê-lo</p>
        <label>Pacientes na ordem de chegada (<span id="sim-contagem">0</span>)</label>
        <div class="chips" id="sim-lista"></div>
      </section>
      <div class="cards largos" id="sim-kpis"></div>
      ${grafico("g-sim-esperas", "Espera de cada paciente", `<span class="legenda"><span><i class="linha"></i>Tempo máximo da cor</span><span><i class="linha tracejada"></i>Média</span></span>`)}
      <div class="grade-2">
        ${grafico("g-sim-cor", "Espera média por cor", legendaPrazo)}
        ${grafico("g-sim-media", "Evolução da espera média")}
      </div>`;
    },
    graficos() {
      desenharSimulador();
    }
  }
};

/* ------------------------------------------------------------------ histórico (linha do tempo) */

function htmlHistorico(a, d) {
  const ev = [{ dt: a.chegada, c: "", h: `<strong>Chegada</strong> — senha <span class="senha">${esc(a.senha)}</span>` }];
  a.classificacoes.forEach(c => {
    const sinais = [
      c.paSistolica != null ? `PA ${c.paSistolica}/${c.paDiastolica ?? "?"} mmHg` : null,
      c.freqCardiaca != null ? `FC ${c.freqCardiaca} bpm` : null,
      c.temperatura != null ? `T ${fmt(c.temperatura)} °C` : null,
      c.saturacaoO2 != null ? `SpO₂ ${c.saturacaoO2}%` : null,
      c.escalaDor != null ? `Dor ${c.escalaDor}/10` : null
    ].filter(Boolean).join(" · ");
    ev.push({ dt: c.dt, c: `c${c.cor}`, h: `<strong>Classificação ${rot(c.tipo).toLowerCase()}</strong> ${tagCor(c.cor)} — ${esc(c.enfermeiro)}
      <div>${esc(c.queixa)}</div><div class="meta">${sinais || "Sem sinais vitais"}</div>${c.motivo ? `<div class="meta">Motivo: ${esc(c.motivo)}</div>` : ""}` });
  });
  d.alertas.filter(al => al.atendimento === a.id).forEach(al => {
    ev.push({ dt: al.limite, c: "", h: `Alerta #${al.id}: limite do prazo (${rot(al.status).toLowerCase()})` });
    if (al.disparo) ev.push({ dt: al.disparo, c: "alerta", h: `<strong>Alerta #${al.id} disparado</strong>` });
    if (al.resolucao) ev.push({ dt: al.resolucao, c: "", h: `Alerta #${al.id} resolvido${al.reavaliacao ? ` pela reavaliação #${al.reavaliacao}` : ""}` });
  });
  a.chamadas.forEach(c => ev.push({ dt: c.dt, c: "", h: `<strong>Chamada</strong> — ${esc(c.local)}, ${esc(c.profissional)} (${rot(c.cargo)}) · ${c.compareceu ? "compareceu" : "<strong>não compareceu</strong>"}` }));
  a.prescricoes.forEach(p => {
    ev.push({ dt: p.inicio, c: "", h: `<strong>Consulta</strong> — ${esc(p.medico)}${p.anamnese ? `<div class="meta">${esc(p.anamnese)}</div>` : ""}` });
    if (p.fim) ev.push({ dt: p.fim, c: "", h: "Fim da consulta" });
    p.exames.forEach(e => {
      ev.push({ dt: e.solicitacao, c: "", h: `Exame solicitado: ${esc(e.tipo)}` });
      if (e.dtResultado) ev.push({ dt: e.dtResultado, c: "", h: `Resultado de ${esc(e.tipo)}: ${esc(e.resultado)}` });
      if (e.visualizacao) ev.push({ dt: e.visualizacao, c: "", h: `Resultado de ${esc(e.tipo)} visto pelo médico` });
    });
  });
  if (a.desfecho) ev.push({ dt: a.dtDesfecho, c: "", h: `<strong>Desfecho: ${rot(a.desfecho)}</strong>${a.encaminhamento ? ` — ${esc(a.encaminhamento)}` : ""}` });
  ev.sort((x, y) => data(x.dt) - data(y.dt));

  return `<div class="dl">
      <div><span>Paciente</span>${esc(a.paciente)}</div>
      <div><span>Idade</span>${a.idade != null ? `${a.idade} anos` : "—"}</div>
      <div><span>Situação</span>${tag(a.status)}</div>
      <div><span>Cor atual</span>${tagCor(a.cor)}</div>
      <div><span>Porta–classificação</span>${duracao(a.portaClassificacao)}</div>
      <div><span>Porta–médico</span>${duracao(a.portaMedico)}</div>
      ${a.permanencia != null ? `<div><span>Permanência</span>${duracao(a.permanencia)}</div>` : ""}
      ${a.esperaAtual != null ? `<div><span>Esperando há</span>${duracao(a.esperaAtual)}</div>` : ""}
    </div>
    <ol class="timeline">${ev.map(e => `<li class="${e.c}"><time>${fmtDH(e.dt)}</time>${e.h}</li>`).join("")}</ol>`;
}

/* ------------------------------------------------------------------ simulador */

function textoSituacao(cor, espera, situacao) {
  if (situacao === "IMEDIATO") return "atendimento imediato, sem fila";
  if (situacao === "ESTOURADO") return `prazo de ${duracao(cor.tempoMax)} estourado (+${duracao(espera - cor.tempoMax)})`;
  return `dentro do prazo de ${duracao(cor.tempoMax)}`;
}

function desenharSimulador() {
  const kpis = document.getElementById("sim-kpis");
  if (!kpis) return;
  const r = sim.dados, temDados = sim.pacientes.length > 0 && r.quantidade === sim.pacientes.length;

  marcarChips();
  kpis.innerHTML = !temDados ? "" : `
    ${kpi(duracao(r.resumo.media), "Espera média", "", `de ${vezes(r.quantidade, "paciente", "pacientes")}`)}
    ${kpi(pct(r.percentualNoPrazo), "Atendidos no prazo", "verde sucesso", r.comPrazo ? `${r.noPrazo} de ${r.comPrazo} com prazo` : "só vermelhos")}
    ${kpi(r.estourados, "Prazos estourados", r.estourados ? "vermelho alerta" : "verde", "alertas para a enfermagem")}
    ${kpi(duracao(r.resumo.mediana), "Mediana")}
    ${kpi(duracao(r.resumo.desvioPadrao), "Desvio padrão", "neutra")}
    ${kpi(duracao(r.resumo.maximo), "Maior espera", "neutra")}
    ${kpi(r.corPredominante.length ? r.corPredominante.map(p => corDe(p).nome).join(", ") : "Nenhuma", "Cor predominante", "azul")}
    ${kpi(pct(r.percentualFastTrack), "Fast track", "verde", "verdes e azuis")}`;

  const g = id => document.getElementById(id);
  if (!temDados) {
    ["g-sim-esperas", "g-sim-cor", "g-sim-media"].forEach(id => vazio(g(id), "Adicione pacientes para ver o gráfico"));
    return;
  }
  colunas(g("g-sim-esperas"), sim.pacientes.map((p, i) => ({ ...p, i })), {
    valor: p => p.espera, cor: p => COR_RISCO[p.cor], eixo: p => `${p.i + 1}º`, media: r.resumo.media,
    prazo: p => corDe(p.cor).tempoMax,
    tip: p => esc(`${p.i + 1}º paciente · ${corDe(p.cor).nome}: ${duracao(p.espera)}, ${textoSituacao(corDe(p.cor), p.espera, r.situacoes[p.i])}`)
  });
  colunas(g("g-sim-cor"), r.porCor.filter(c => c.pacientes > 0), {
    valor: c => c.mediaEspera, cor: c => COR_RISCO[c.prioridade], eixo: c => corDe(c.prioridade).nome,
    rotulo: c => fmt(c.mediaEspera), prazo: c => corDe(c.prioridade).tempoMax,
    tip: c => esc(`${corDe(c.prioridade).nome}: média ${duracao(c.mediaEspera)} · ${c.estourados} de ${vezes(c.pacientes, "paciente estourou", "pacientes estouraram")} o prazo`)
  });
  linha(g("g-sim-media"), r.mediaAcumulada.map((v, i) => ({
    eixo: `${i + 1}º`, valor: v, tip: esc(`Após ${vezes(i + 1, "paciente", "pacientes")}: média ${duracao(v)}`)
  })), { rotuloFinal: duracao(r.resumo.media) });
}

function marcarChips() {
  const chips = document.getElementById("sim-lista")?.children;
  if (!chips) return;
  const situacoes = sim.dados.situacoes?.length === sim.pacientes.length ? sim.dados.situacoes : [];
  sim.pacientes.forEach((p, i) => {
    const cor = corDe(p.cor), s = situacoes[i];
    chips[i].classList.toggle("estourado", s === "ESTOURADO");
    chips[i].title = `${i + 1}º paciente · ${cor.nome}: ${duracao(p.espera)}${s ? ", " + textoSituacao(cor, p.espera, s) : ""}. Clique para remover.`;
  });
}
