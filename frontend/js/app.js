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
  const idsGraficos = ["grafico-pacientes", "grafico-espera", "grafico-filas", "grafico-situacao", "grafico-faixas", "grafico-resumo"];
  if (resultado === null) {
    for (const id of idsGraficos) {
      document.getElementById(id).innerHTML = `<p class="vazio">Adicione pacientes para ver o gráfico</p>`;
    }
    return;
  }
  mostrarGraficoColunas();
  mostrarGraficoFilas();
  mostrarGraficoSituacao();
  mostrarGraficoFaixas();
  mostrarGraficoResumo();
}

// Dois gráficos de colunas: pacientes por cor e espera média por cor.
function mostrarGraficoColunas() {
  const graficoPacientes = document.getElementById("grafico-pacientes");
  const graficoEspera = document.getElementById("grafico-espera");

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

// Uma linha de legenda: bolinha colorida, nome e valor.
function itemLegenda(cor, nome, valor) {
  return `<li><span class="bola" style="background:${cor}"></span>${nome}<b>${valor}</b></li>`;
}

// Gráfico de rosca: cada fila ocupa uma fatia proporcional aos seus pacientes.
// O CSS conic-gradient pinta as fatias: "cor início% fim%" para cada uma.
function mostrarGraficoFilas() {
  const COR_FILA = { "Emergência": "#b71c1c", "Principal": "#e8650a", "Fast Track": "#2e7d32" };
  const fatias = [];
  let legenda = "";
  let inicio = 0;
  for (const f of resultado.porFila) {
    const percentual = f.pacientes / resultado.pacientes * 100;
    const fim = inicio + percentual;
    fatias.push(COR_FILA[f.fila] + " " + inicio + "% " + fim + "%");
    legenda += itemLegenda(COR_FILA[f.fila], f.fila, textoPacientes(f.pacientes) + " · " + formatarPercentual(percentual));
    inicio = fim;
  }
  document.getElementById("grafico-filas").innerHTML = `
    <div class="rosca-area">
      <div class="rosca" style="background: conic-gradient(${fatias.join(", ")})">
        <span><b>${resultado.pacientes}</b>pacientes</span>
      </div>
      <ul class="legenda-lista">${legenda}</ul>
    </div>`;
}

// Barra empilhada: uma barra só, dividida entre no prazo, estourados e imediatos (vermelhos, sem prazo).
function mostrarGraficoSituacao() {
  const partes = [
    { nome: "No prazo", valor: resultado.noPrazo, cor: "#2e7d32" },
    { nome: "Prazo estourado", valor: resultado.estourados, cor: "#b42318" },
    { nome: "Imediato (vermelho, sem prazo)", valor: resultado.pacientes - resultado.comPrazo, cor: "#6b7785" }
  ];
  let barra = "";
  let legenda = "";
  for (const parte of partes) {
    const percentual = parte.valor / resultado.pacientes * 100;
    if (parte.valor > 0) {
      let texto = "";
      if (percentual >= 10) {
        texto = formatarPercentual(percentual); // só escreve dentro do pedaço se couber
      }
      barra += `<div style="width:${percentual}%; background:${parte.cor}">${texto}</div>`;
    }
    legenda += itemLegenda(parte.cor, parte.nome, textoPacientes(parte.valor) + " · " + formatarPercentual(percentual));
  }
  document.getElementById("grafico-situacao").innerHTML = `
    <div class="empilhada">${barra}</div>
    <ul class="legenda-lista">${legenda}</ul>`;
}

// Barras horizontais: quantos pacientes em cada faixa de espera (distribuição de frequências).
function mostrarGraficoFaixas() {
  let maior = 1;
  for (const f of resultado.faixasEspera) {
    maior = Math.max(maior, f.pacientes);
  }
  let html = "";
  for (const f of resultado.faixasEspera) {
    html += `<div class="linha-barra">
      <span>${f.faixa}</span>
      <div class="trilho"><div style="width:${f.pacientes / maior * 100}%"></div></div>
      <b>${f.pacientes}</b>
    </div>`;
  }
  document.getElementById("grafico-faixas").innerHTML = html;
}

// Régua de 0 até a maior espera: a faixa cinza vai da menor à maior espera,
// e as marcas mostram onde ficam a mediana e a média.
function mostrarGraficoResumo() {
  let escala = resultado.maximo;
  if (escala === 0) {
    escala = 1;
  }
  function posicao(valor) {
    return valor / escala * 100;
  }

  let observacao = "A média e a mediana são iguais: as esperas estão equilibradas.";
  if (resultado.media > resultado.mediana) {
    observacao = "A média está acima da mediana: algumas esperas longas puxam a média para cima.";
  } else if (resultado.media < resultado.mediana) {
    observacao = "A média está abaixo da mediana: algumas esperas curtas puxam a média para baixo.";
  }

  document.getElementById("grafico-resumo").innerHTML = `
    <div class="regua">
      <div class="regua-faixa" style="left:${posicao(resultado.minimo)}%; width:${posicao(resultado.maximo - resultado.minimo)}%"></div>
      <div class="marcador mediana" style="left:${posicao(resultado.mediana)}%"></div>
      <div class="marcador media" style="left:${posicao(resultado.media)}%"></div>
    </div>
    <div class="regua-eixo"><span>0 min</span><span>${formatarMinutos(resultado.maximo)}</span></div>
    <ul class="legenda-lista">
      ${itemLegenda("#b9c5d0", "Menor espera", formatarMinutos(resultado.minimo))}
      ${itemLegenda("#6a1b9a", "Mediana", formatarMinutos(resultado.mediana))}
      ${itemLegenda("#0a5c78", "Média", formatarMinutos(resultado.media))}
      ${itemLegenda("#b9c5d0", "Maior espera", formatarMinutos(resultado.maximo))}
    </ul>
    <p class="dica">${observacao}</p>`;
}

// Ao abrir a página
selecionarCor(3);
mostrarResultado();
