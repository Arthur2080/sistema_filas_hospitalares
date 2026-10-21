/* =========================================================================
   Estado, navegação, relógio simulado e eventos.
   ========================================================================= */

const $ = id => document.getElementById(id);
const RELOGIO_INICIAL = "2026-10-06T10:46";
const ESPERA_DIGITACAO = 900;

let dados = null;
let agora = RELOGIO_INICIAL;
// A tela atual fica no endereço (ex.: #tempos), para abrir uma tela direto pelo link.
let tela = Object.hasOwn(TELAS, location.hash.slice(1)) ? location.hash.slice(1) : "visao";
const sim = { cor: 3, pacientes: [], dados: {}, timer: null, pedido: 0 };

const ICONES = {
  visao: "M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z",
  tempos: "M12 7v5l3 2M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0",
  prazos: "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10 21h4",
  fluxo: "M3 20h18M6 16v-5M11 16V6M16 16v-8",
  desfechos: "M10 17l5-5-5-5M15 12H3M21 3v18",
  atendimentos: "M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01",
  simulador: "M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"
};

// Data local no formato do input datetime-local: 2026-10-06T10:46
function isoLocal(d) {
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}T${p2(d.getHours())}:${p2(d.getMinutes())}`;
}

const disparados = () => dados ? dados.alertas.filter(a => a.status === "DISPARADO").length : 0;

async function carregar(novoAgora, avisar = false) {
  const antes = disparados();
  try {
    const resp = await fetch(`/api/painel?agora=${encodeURIComponent(novoAgora)}`);
    const json = await resp.json();
    if (!resp.ok) return toast(json.erro, "erro");
    dados = json;
    agora = json.agora;
    CORES = json.cores;
  } catch {
    return toast("Não foi possível falar com o servidor. Ele está rodando?", "erro");
  }
  render();
  const novos = disparados() - antes;
  if (avisar && novos > 0) toast(`${vezes(novos, "alerta de prazo disparado", "alertas de prazo disparados")}!`, "erro");
}

function render() {
  $("hora").textContent = fmtDH(agora);
  $("definirHora").value = agora;

  let grupo = "";
  $("menu").innerHTML = Object.entries(TELAS).map(([k, t]) => {
    const cabeca = t.grupo !== grupo ? `<div class="grupo-menu">${(grupo = t.grupo)}</div>` : "";
    const contador = k === "prazos" && disparados() ? `<span class="contador" aria-label="alertas disparados">${disparados()}</span>` : "";
    return `${cabeca}<button data-tela="${k}" ${tela === k ? 'aria-current="page"' : ""}>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${ICONES[k]}"/></svg>
      ${t.titulo}${contador}</button>`;
  }).join("");

  $("legenda").innerHTML = "<strong>Cores de risco</strong>" + CORES.map(c =>
    `<div><span class="cor c${c.prioridade}">${esc(c.nome)}</span><small>${c.tempoMax ? `até ${c.tempoMax} min` : "imediato"}</small></div>`).join("");

  $("conteudo").innerHTML = TELAS[tela].html(dados);
  if (tela === "simulador") {
    marcarCorSimulador();
    desenharListaSim();
  }
  desenharGraficos();
}

function desenharGraficos() {
  TELAS[tela].graficos?.(dados);
}

function irPara(novaTela) {
  tela = novaTela;
  history.replaceState(null, "", "#" + novaTela);
  render();
  $("conteudo").focus();
  scrollTo(0, 0);
}

function mudarRelogio(novo) {
  carregar(novo, true);
}

/* ------------------------------------------------------------------ modal e avisos */

function toast(msg, tipo = "ok") {
  const t = document.createElement("div");
  t.className = "toast " + tipo;
  t.textContent = msg;
  $("toasts").appendChild(t);
  setTimeout(() => t.remove(), 4500);
}

function abrirHistorico(id) {
  const a = atendimentoPorId(dados, id);
  const dlg = $("modal");
  dlg.innerHTML = `<header><h2 id="mtitulo">Histórico — <span class="senha">${esc(a.senha)}</span> · ${esc(a.paciente)}</h2>
    <button type="button" class="x" data-fechar aria-label="Fechar">×</button></header>
    <div class="mcorpo">${htmlHistorico(a, dados)}</div>`;
  dlg.showModal();
}

/* ------------------------------------------------------------------ simulador */

function lerEspera(texto) {
  texto = texto.trim().replace(",", ".");
  if (texto === "") return null;
  const n = Number(texto);
  return Number.isFinite(n) && n >= 0 ? n : NaN;
}

function marcarCorSimulador() {
  document.querySelectorAll("[data-sim-cor]").forEach(b =>
    b.setAttribute("aria-pressed", Number(b.dataset.simCor) === sim.cor));
}

function desenharListaSim(destacarUltimo = false) {
  const lista = $("sim-lista");
  if (!lista) return;
  $("sim-contagem").textContent = sim.pacientes.length;
  lista.innerHTML = sim.pacientes.map((p, i) => `
    <button class="btn chip ${destacarUltimo && i === sim.pacientes.length - 1 ? "novo" : ""}" data-sim-remover="${i}">
      <span class="bola" style="background:${COR_RISCO[p.cor]}"></span>${duracao(p.espera)}</button>`).join("");
  marcarChips();
}

async function atualizarSimulador(destacarUltimo = false) {
  desenharListaSim(destacarUltimo);
  const pedido = ++sim.pedido;
  if (!sim.pacientes.length) {
    sim.dados = {};
    return desenharSimulador();
  }
  const resp = await fetch("/calcular", { method: "POST", body: sim.pacientes.map(p => `${p.cor}:${p.espera}`).join(",") });
  const json = await resp.json();
  if (pedido !== sim.pedido) return;
  if (!resp.ok) toast(json.erro, "erro");
  sim.dados = resp.ok ? json : {};
  desenharSimulador();
}

function agendarSim() {
  const input = $("sim-espera"), barra = $("sim-progresso");
  clearTimeout(sim.timer);
  barra.classList.remove("ativa");
  void barra.offsetWidth;

  const n = lerEspera(input.value);
  input.classList.toggle("invalido", Number.isNaN(n));
  if (!Number.isFinite(n)) return;
  barra.classList.add("ativa");
  sim.timer = setTimeout(salvarSim, ESPERA_DIGITACAO);
}

function salvarSim() {
  const input = $("sim-espera");
  clearTimeout(sim.timer);
  $("sim-progresso").classList.remove("ativa");
  const n = lerEspera(input.value);
  if (!Number.isFinite(n)) return;
  sim.pacientes.push({ cor: sim.cor, espera: n });
  input.value = "";
  atualizarSimulador(true);
}

// Simula um plantão: poucos vermelhos, maioria amarelo e verde, e parte das esperas passando do prazo.
function plantaoExemplo() {
  const sorteio = [1, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5];
  sim.pacientes = Array.from({ length: 20 }, () => {
    const cor = corDe(sorteio[Math.floor(Math.random() * sorteio.length)]);
    const espera = cor.tempoMax === 0 ? Math.floor(Math.random() * 3) : Math.round(cor.tempoMax * (0.2 + Math.random() * 1.1));
    return { cor: cor.prioridade, espera };
  });
  atualizarSimulador();
}

/* ------------------------------------------------------------------ eventos */

document.addEventListener("click", e => {
  const alvo = e.target.closest("[data-tela],[data-historico],[data-fechar],[data-avancar],[data-sim-cor],[data-sim-remover],#sim-exemplo,#sim-limpar");
  if (!alvo) {
    if (e.target === $("modal")) $("modal").close();
    return;
  }
  const ds = alvo.dataset;
  if (ds.tela) irPara(ds.tela);
  else if (ds.historico) abrirHistorico(Number(ds.historico));
  else if ("fechar" in ds) $("modal").close();
  else if (ds.avancar) mudarRelogio(isoLocal(new Date(data(agora).getTime() + Number(ds.avancar) * 60000)));
  else if (ds.simCor) {
    sim.cor = Number(ds.simCor);
    marcarCorSimulador();
    $("sim-espera").focus();
  } else if (ds.simRemover) {
    sim.pacientes.splice(Number(ds.simRemover), 1);
    atualizarSimulador();
  } else if (alvo.id === "sim-exemplo") plantaoExemplo();
  else if (alvo.id === "sim-limpar") {
    sim.pacientes = [];
    $("sim-espera").value = "";
    agendarSim();
    atualizarSimulador();
  }
});

document.addEventListener("input", e => {
  if (e.target.id !== "sim-espera") return;
  const input = e.target, partes = input.value.split(/[\s;]+/);
  if (partes.length > 1) {
    const ultimo = partes.pop();
    partes.map(lerEspera).filter(Number.isFinite).forEach(n => sim.pacientes.push({ cor: sim.cor, espera: n }));
    input.value = ultimo;
    atualizarSimulador();
  }
  agendarSim();
});

document.addEventListener("keydown", e => {
  if (e.target.id === "sim-espera" && e.key === "Enter") salvarSim();
});

document.addEventListener("change", e => {
  if (e.target.id === "filtroAlerta") ui.filtroAlerta = e.target.value;
  else if (e.target.id === "filtroStatus") ui.filtroStatus = e.target.value;
  else return;
  render();
});

$("btnDefinir").onclick = () => {
  const v = $("definirHora").value;
  if (!v) return toast("Informe data e hora.", "erro");
  mudarRelogio(v);
};
$("btnReiniciar").onclick = () => carregar(RELOGIO_INICIAL);

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
  quadro = requestAnimationFrame(() => dados && desenharGraficos());
});

carregar(RELOGIO_INICIAL);
