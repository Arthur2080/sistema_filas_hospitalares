// Simulador de espera do pronto-socorro.
// Fluxo: o usuário escolhe a cor -> digita a quantidade e a espera -> o Java calcula -> a tela mostra o resultado.

const CORES = [
  { prioridade: 1, nome: "Vermelho", cor: "#b71c1c" },
  { prioridade: 2, nome: "Laranja", cor: "#e8650a" },
  { prioridade: 3, nome: "Amarelo", cor: "#d4a200" },
  { prioridade: 4, nome: "Verde", cor: "#2e7d32" },
  { prioridade: 5, nome: "Azul", cor: "#1565c0" }
];

let corSelecionada = 3;
let grupos = [];       // cada grupo: { prioridade, quantidade, espera }
let resultado = null;  // resposta do Java

// ------------------------------------------------------------------ funções de apoio

function buscarCor(prioridade) {
  for (const cor of CORES) {
    if (cor.prioridade === prioridade) {
      return cor;
    }
  }
}

// 45 -> "45 min" | 90 -> "1 h 30 min"
function formatarMinutos(minutos) {
  const total = Math.round(minutos);
  if (total < 60) {
    return total + " min";
  }
  const horas = Math.floor(total / 60);
  const resto = total % 60;
  return horas + " h " + String(resto).padStart(2, "0") + " min";
}

// 81.25 -> "81,3%"
function formatarPercentual(valor) {
  if (valor === null) {
    return "—";
  }
  return valor.toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + "%";
}

function textoPacientes(quantidade) {
  if (quantidade === 1) {
    return "1 paciente";
  }
  return quantidade + " pacientes";
}

function mostrarErro(mensagem) {
  const erro = document.getElementById("mensagem-erro");
  erro.textContent = mensagem;
  erro.hidden = false;
}

// ------------------------------------------------------------------ ações do usuário

function selecionarCor(prioridade) {
  corSelecionada = prioridade;
  const botoes = document.querySelectorAll(".seletor-cores button");
  for (const botao of botoes) {
    botao.classList.toggle("selecionado", Number(botao.dataset.cor) === prioridade);
  }
  document.getElementById("quantidade").focus();
}

function adicionarGrupo(evento) {
  evento.preventDefault(); // não recarrega a página
  const campoQuantidade = document.getElementById("quantidade");
  const campoEspera = document.getElementById("espera");
  const quantidade = Number(campoQuantidade.value);
  const espera = Number(campoEspera.value);

  if (campoQuantidade.value === "" || !Number.isInteger(quantidade) || quantidade < 1) {
    mostrarErro("Digite a quantidade de pacientes: um número inteiro a partir de 1.");
    return;
  }
  if (campoEspera.value === "" || espera < 0) {
    mostrarErro("Digite a espera em minutos: um número a partir de 0.");
    return;
  }

  document.getElementById("mensagem-erro").hidden = true;
  grupos.push({ prioridade: corSelecionada, quantidade: quantidade, espera: espera });
  campoQuantidade.value = "";
  campoEspera.value = "";
  campoQuantidade.focus();
  calcular();
}

function removerGrupo(indice) {
  grupos.splice(indice, 1);
  calcular();
}

function limpar() {
  grupos = [];
  calcular();
}

// Exemplo fixo de um plantão com 17 pacientes, para conferir os resultados à mão.
function carregarExemplo() {
  grupos = [
    { prioridade: 1, quantidade: 1, espera: 0 },
    { prioridade: 2, quantidade: 2, espera: 8 },
    { prioridade: 3, quantidade: 5, espera: 30 },
    { prioridade: 3, quantidade: 2, espera: 75 },
    { prioridade: 4, quantidade: 4, espera: 90 },
    { prioridade: 4, quantidade: 1, espera: 150 },
    { prioridade: 5, quantidade: 2, espera: 100 }
  ];
  calcular();
}

// ------------------------------------------------------------------ chamada ao Java

async function calcular() {
  if (grupos.length === 0) {
    resultado = null;
    mostrarResultado();
    return;
  }

  // Cada grupo vai como "prioridade:quantidade:espera", separados por vírgula. Ex.: "3:5:30,4:3:150"
  const partes = [];
  for (const grupo of grupos) {
    partes.push(grupo.prioridade + ":" + grupo.quantidade + ":" + grupo.espera);
  }

  try {
    const resposta = await fetch("/calcular", { method: "POST", body: partes.join(",") });
    const dados = await resposta.json();
    if (resposta.ok) {
      resultado = dados;
    } else {
      resultado = null;
      mostrarErro(dados.erro);
    }
  } catch (erro) {
    resultado = null;
    mostrarErro("Não foi possível falar com o servidor. Ele está rodando?");
  }
  mostrarResultado();
}

// ------------------------------------------------------------------ resultado na tela

function mostrarResultado() {
  mostrarGrupos();
  mostrarCards();
  mostrarGraficos();
}

function mostrarGrupos() {
  let html = "";
  let total = 0;
  for (let i = 0; i < grupos.length; i++) {
    const grupo = grupos[i];
    const cor = buscarCor(grupo.prioridade);
    let classe = "btn chip";
    if (resultado !== null && resultado.situacoes[i] === "ESTOURADO") {
      classe += " estourado";
    }
    html += `<li><button type="button" class="${classe}" onclick="removerGrupo(${i})" title="Clique para remover">
      <span class="bola" style="background:${cor.cor}"></span>${cor.nome} · ${textoPacientes(grupo.quantidade)} · ${formatarMinutos(grupo.espera)}
    </button></li>`;
    total += grupo.quantidade;
  }
  document.getElementById("grupos").innerHTML = html;
  document.getElementById("contagem").textContent = textoPacientes(total);
}

function card(valor, titulo, classe, detalhe) {
  return `<div class="card ${classe}"><b>${valor}</b><span>${titulo}</span><small>${detalhe}</small></div>`;
}

function mostrarCards() {
  const cards = document.getElementById("cards");
  if (resultado === null) {
    cards.innerHTML = "";
    return;
  }

  let detalhePrazo = "só vermelhos";
  if (resultado.comPrazo > 0) {
    detalhePrazo = resultado.noPrazo + " de " + resultado.comPrazo + " com prazo";
  }
  let corPredominante = "Nenhuma";
  if (resultado.corPredominante.length > 0) {
    corPredominante = resultado.corPredominante.join(", ");
  }

  cards.innerHTML =
    card(resultado.pacientes, "Pacientes", "", grupos.length + " grupo(s)") +
    card(formatarMinutos(resultado.media), "Espera média", "", "média de todos os pacientes") +
    card(formatarMinutos(resultado.mediana), "Mediana", "", "valor do meio") +
    card(formatarMinutos(resultado.desvioPadrao), "Desvio padrão", "cinza", "distância média até a média") +
    card(formatarMinutos(resultado.minimo), "Menor espera", "cinza", "") +
    card(formatarMinutos(resultado.maximo), "Maior espera", "cinza", "") +
    card(formatarPercentual(resultado.percentualNoPrazo), "Atendidos no prazo", "verde", detalhePrazo) +
    card(resultado.estourados, "Prazos estourados", "vermelho", "pacientes além do tempo máximo") +
    card(corPredominante, "Cor predominante", "azul", "moda das cores") +
    card(formatarPercentual(resultado.percentualFastTrack), "Fast track", "verde", "verdes e azuis");
}

// Uma coluna do gráfico. altura e limite são percentuais da área do gráfico.
function coluna(nome, texto, altura, cor, limite) {
  let linhaLimite = "";
  if (limite !== null) {
    linhaLimite = `<div class="limite" style="bottom:${limite}%"></div>`;
  }
  return `<div class="coluna">
    <div class="area">
      ${linhaLimite}
      <div class="barra" style="height:${altura}%; background:${cor}"></div>
    </div>
    <small>${nome}</small>
    <b>${texto}</b>
  </div>`;
}

function mostrarGraficos() {
  const graficoPacientes = document.getElementById("grafico-pacientes");
  const graficoEspera = document.getElementById("grafico-espera");
  if (resultado === null) {
    graficoPacientes.innerHTML = `<p class="vazio">Adicione pacientes para ver o gráfico</p>`;
    graficoEspera.innerHTML = `<p class="vazio">Adicione pacientes para ver o gráfico</p>`;
    return;
  }

  // Pacientes por cor: a coluna mais alta é a cor com mais pacientes.
  let maiorQuantidade = 1;
  for (const c of resultado.porCor) {
    maiorQuantidade = Math.max(maiorQuantidade, c.pacientes);
  }
  let html = "";
  for (const c of resultado.porCor) {
    html += coluna(c.nome, c.pacientes, c.pacientes / maiorQuantidade * 100, buscarCor(c.prioridade).cor, null);
  }
  graficoPacientes.innerHTML = html;

  // Espera média por cor, só das cores com pacientes. A linha preta é o tempo máximo da cor.
  let maiorEspera = 1;
  for (const c of resultado.porCor) {
    if (c.pacientes > 0) {
      maiorEspera = Math.max(maiorEspera, c.mediaEspera, c.tempoMaximo);
    }
  }
  html = "";
  for (const c of resultado.porCor) {
    if (c.pacientes > 0) {
      let limite = null;
      if (c.tempoMaximo > 0) {
        limite = c.tempoMaximo / maiorEspera * 100;
      }
      html += coluna(c.nome, formatarMinutos(c.mediaEspera), c.mediaEspera / maiorEspera * 100, buscarCor(c.prioridade).cor, limite);
    }
  }
  graficoEspera.innerHTML = html;
}

// Ao abrir a página
selecionarCor(3);
mostrarResultado();
