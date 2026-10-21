/* =========================================================================
   Simulador: o usuário escolhe a cor, digita a quantidade de pacientes e a
   espera; o Java (/calcular) faz as contas e a tela mostra o resultado.
   ========================================================================= */

const $ = id => document.getElementById(id);
const NOME_COR = { 1: "Vermelho", 2: "Laranja", 3: "Amarelo", 4: "Verde", 5: "Azul" };

// grupos: [{ cor, quantidade, espera }] · dados: resposta do Java · pedido: descarta respostas antigas
const sim = { cor: 3, grupos: [], dados: null, pedido: 0 };

const esc = v => String(v ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const kpi = (valor, texto, classe = "", sub = "") =>
  `<div class="kpi ${classe}"><b>${valor}</b><span>${texto}</span>${sub ? `<small>${sub}</small>` : ""}</div>`;
const totalPacientes = () => sim.grupos.reduce((soma, g) => soma + g.quantidade, 0);

function toast(msg) {
  const t = document.createElement("div");
  t.className = "toast erro";
  t.textContent = msg;
  $("toasts").appendChild(t);
  setTimeout(() => t.remove(), 4500);
}

/* ------------------------------------------------------------------ entrada */

function marcarCor() {
  document.querySelectorAll("[data-cor]").forEach(b => b.setAttribute("aria-pressed", Number(b.dataset.cor) === sim.cor));
}

function lerNumero(input, minimo, inteiro) {
  const n = Number(input.value.replace(",", "."));
  const ok = input.value.trim() !== "" && Number.isFinite(n) && n >= minimo && (!inteiro || Number.isInteger(n));
  input.classList.toggle("invalido", !ok);
  return ok ? n : null;
}

function adicionarGrupo() {
  const quantidade = lerNumero($("quantidade"), 1, true);
  const espera = lerNumero($("espera"), 0, false);
  if (quantidade === null) return toast("Digite a quantidade de pacientes: um número inteiro a partir de 1.");
  if (espera === null) return toast("Digite a espera em minutos: um número a partir de 0.");

  sim.grupos.push({ cor: sim.cor, quantidade, espera });
  $("quantidade").value = "";
  $("espera").value = "";
  $("quantidade").focus();
  calcular();
}

// Exemplo fixo de um plantão (17 pacientes), para conferir os resultados à mão.
function carregarExemplo() {
  sim.grupos = [
    { cor: 1, quantidade: 1, espera: 0 },
    { cor: 2, quantidade: 2, espera: 8 },
    { cor: 3, quantidade: 5, espera: 30 },
    { cor: 3, quantidade: 2, espera: 75 },
    { cor: 4, quantidade: 4, espera: 90 },
    { cor: 4, quantidade: 1, espera: 150 },
    { cor: 5, quantidade: 2, espera: 100 }
  ];
  calcular();
}

/* ------------------------------------------------------------------ cálculo no Java */

async function calcular() {
  const pedido = ++sim.pedido;
  sim.dados = null;
  if (sim.grupos.length) {
    try {
      // Cada grupo vai como "prioridade:quantidade:espera", separados por vírgula.
      const corpo = sim.grupos.map(g => `${g.cor}:${g.quantidade}:${g.espera}`).join(",");
      const resp = await fetch("/calcular", { method: "POST", body: corpo });
      const json = await resp.json();
      if (pedido !== sim.pedido) return;
      if (resp.ok) sim.dados = json;
      else toast(json.erro);
    } catch {
      if (pedido !== sim.pedido) return;
      toast("Não foi possível falar com o servidor. Ele está rodando?");
    }
  }
  desenhar();
}

/* ------------------------------------------------------------------ resultado */

function textoSituacao(g, situacao, tempoMax) {
  if (situacao === "IMEDIATO") return "atendimento imediato, sem fila";
  if (situacao === "ESTOURADO") return `prazo de ${duracao(tempoMax)} estourado (+${duracao(g.espera - tempoMax)})`;
  return `dentro do prazo de ${duracao(tempoMax)}`;
}

function desenhar() {
  const r = sim.dados;
  const tempoMax = p => r.porCor.find(c => c.prioridade === p).tempoMax;

  $("contagem").textContent = vezes(totalPacientes(), "paciente", "pacientes");
  $("grupos").innerHTML = sim.grupos.map((g, i) => {
    const s = r ? r.situacoes[i] : "";
    const titulo = `${NOME_COR[g.cor]}: ${vezes(g.quantidade, "paciente", "pacientes")} com ${duracao(g.espera)}` +
                   (s ? `, ${textoSituacao(g, s, tempoMax(g.cor))}` : "") + ". Clique para remover.";
    return `<li><button type="button" class="btn chip ${s === "ESTOURADO" ? "estourado" : ""}" data-remover="${i}" title="${esc(titulo)}">
      <span class="bola" style="background:${COR_RISCO[g.cor]}"></span>${NOME_COR[g.cor]} · ${vezes(g.quantidade, "paciente", "pacientes")} · ${duracao(g.espera)}</button></li>`;
  }).join("");

  const graficos = ["g-grupos", "g-pacientes", "g-media-cor"].map($);
  if (!r) {
    $("kpis").innerHTML = "";
    graficos.forEach(el => vazio(el, "Adicione pacientes para ver o gráfico"));
    return;
  }

  $("kpis").innerHTML = `
    ${kpi(r.pacientes, "Pacientes", "", vezes(sim.grupos.length, "grupo", "grupos"))}
    ${kpi(duracao(r.media), "Espera média")}
    ${kpi(duracao(r.mediana), "Mediana")}
    ${kpi(duracao(r.desvioPadrao), "Desvio padrão", "neutra")}
    ${kpi(duracao(r.minimo), "Menor espera", "neutra")}
    ${kpi(duracao(r.maximo), "Maior espera", "neutra")}
    ${kpi(pct(r.percentualNoPrazo), "Atendidos no prazo", "verde sucesso", r.comPrazo ? `${r.noPrazo} de ${r.comPrazo} com prazo` : "só vermelhos")}
    ${kpi(r.estourados, "Prazos estourados", r.estourados ? "vermelho alerta" : "verde", "pacientes além do tempo máximo")}
    ${kpi(r.corPredominante.length ? r.corPredominante.map(p => NOME_COR[p]).join(", ") : "Nenhuma", "Cor predominante", "azul", "moda das cores")}
    ${kpi(pct(r.percentualFastTrack), "Fast track", "verde", "verdes e azuis")}`;

  colunas(graficos[0], sim.grupos.map((g, i) => ({ ...g, i })), {
    valor: g => g.espera, cor: g => COR_RISCO[g.cor], eixo: g => `${g.i + 1}º`, media: r.media,
    rotulo: g => `${g.quantidade}×`, prazo: g => tempoMax(g.cor),
    tip: g => esc(`${g.i + 1}º grupo · ${NOME_COR[g.cor]}: ${vezes(g.quantidade, "paciente", "pacientes")} com ${duracao(g.espera)}, ${textoSituacao(g, r.situacoes[g.i], tempoMax(g.cor))}`)
  });
  colunas(graficos[1], r.porCor, {
    valor: c => c.pacientes, cor: c => COR_RISCO[c.prioridade], eixo: c => c.nome, inteiro: true,
    rotulo: c => c.pacientes,
    tip: c => esc(`${c.nome}: ${vezes(c.pacientes, "paciente", "pacientes")}`)
  });
  colunas(graficos[2], r.porCor.filter(c => c.pacientes > 0), {
    valor: c => c.mediaEspera, cor: c => COR_RISCO[c.prioridade], eixo: c => c.nome,
    rotulo: c => fmt(c.mediaEspera), prazo: c => c.tempoMax,
    tip: c => esc(`${c.nome}: média ${duracao(c.mediaEspera)} · ${c.estourados} de ${vezes(c.pacientes, "paciente estourou", "pacientes estouraram")} o prazo`)
  });
}

/* ------------------------------------------------------------------ eventos */

$("form-sim").addEventListener("submit", e => {
  e.preventDefault();
  adicionarGrupo();
});

document.addEventListener("click", e => {
  const alvo = e.target.closest("[data-cor],[data-remover],#exemplo,#limpar");
  if (!alvo) return;
  if (alvo.dataset.cor) {
    sim.cor = Number(alvo.dataset.cor);
    marcarCor();
    $("quantidade").focus();
  } else if (alvo.dataset.remover) {
    sim.grupos.splice(Number(alvo.dataset.remover), 1);
    calcular();
  } else if (alvo.id === "exemplo") carregarExemplo();
  else if (alvo.id === "limpar") {
    sim.grupos = [];
    calcular();
  }
});

const tip = $("tip");
document.addEventListener("mousemove", e => {
  const alvo = e.target.closest("[data-tip]");
  if (!alvo || !alvo.dataset.tip) {
    tip.hidden = true;
    return;
  }
  tip.textContent = alvo.dataset.tip;
  tip.hidden = false;
  tip.style.left = Math.min(e.clientX + 14, innerWidth - tip.offsetWidth - 8) + "px";
  tip.style.top = e.clientY + 16 + "px";
});

let quadro;
addEventListener("resize", () => {
  cancelAnimationFrame(quadro);
  quadro = requestAnimationFrame(desenhar);
});

marcarCor();
desenhar();
