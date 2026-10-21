// Cores do Protocolo de Manchester, pela prioridade (1 = mais grave). Sempre acompanhadas do nome da cor.
const COR_RISCO = { 1: "#b71c1c", 2: "#e8650a", 3: "#d4a200", 4: "#2e7d32", 5: "#1565c0" };

const COR = {
  primaria: "#0a5c78",
  tinta: "#102131",
  grade: "#e6ebf0",
  eixo: "#b9c5d0",
  superficie: "#ffffff",
  neutra: "#6b7785"
};

const fmt = n => n == null ? "—" : n.toLocaleString("pt-BR", { maximumFractionDigits: 1 });
const pct = n => n == null ? "—" : `${fmt(n)}%`;
const vezes = (n, um, varios) => `${n} ${n === 1 ? um : varios}`;

// Minutos em texto: "45 min" ou "1 h 26 min".
function duracao(n) {
  if (n == null) return "—";
  const m = Math.round(Math.abs(n));
  return m >= 60 ? `${Math.floor(m / 60)} h ${String(m % 60).padStart(2, "0")} min` : `${m} min`;
}

function escala(d0, d1, r0, r1) {
  return v => r0 + ((v - d0) / (d1 - d0 || 1)) * (r1 - r0);
}

function ticksBonitos(min, max, qtd = 5) {
  if (min === max) { min -= 1; max += 1; }
  const bruto = (max - min) / qtd;
  const mag = 10 ** Math.floor(Math.log10(bruto));
  const passo = [1, 2, 2.5, 5, 10].map(m => m * mag).find(p => p >= bruto);
  const inicio = Math.floor(min / passo) * passo;
  const ticks = [];
  for (let i = 0; ; i++) {
    const v = +(inicio + i * passo).toFixed(10);
    ticks.push(v);
    if (v >= max) break;
  }
  return ticks;
}

function ticksInteiros(max) {
  const passo = Math.max(1, Math.ceil(max / 4));
  const ticks = [];
  for (let t = 0; t < max + passo; t += passo) ticks.push(t);
  return ticks;
}

function margemEsquerda(ticks) {
  return 14 + 7 * Math.max(...ticks.map(t => fmt(t).length));
}

function passoRotulos(qtd, largura, espaco = 44) {
  return Math.max(1, Math.ceil(qtd / Math.max(1, Math.floor(largura / espaco))));
}

function desenharSvg(el, altura, conteudo) {
  const largura = el.clientWidth;
  el.innerHTML = `<svg width="${largura}" height="${altura}" viewBox="0 0 ${largura} ${altura}" role="img">${conteudo}</svg>`;
}

function vazio(el, texto = "Sem dados para exibir") {
  el.innerHTML = `<p class="vazio">${texto}</p>`;
}

function gradeY(ticks, y, x0, x1) {
  return ticks.map(t => `
    <line x1="${x0}" x2="${x1}" y1="${y(t)}" y2="${y(t)}" stroke="${t === 0 ? COR.eixo : COR.grade}"/>
    <text x="${x0 - 8}" y="${y(t)}" text-anchor="end" dominant-baseline="middle">${fmt(t)}</text>`).join("");
}

function coluna(x, w, yBase, yFim) {
  if (yFim === yBase) return "";
  const r = Math.min(4, w / 2, Math.abs(yBase - yFim));
  const d = yFim < yBase ? r : -r;
  return `M${x},${yBase} V${yFim + d} Q${x},${yFim} ${x + r},${yFim} H${x + w - r} ` +
         `Q${x + w},${yFim} ${x + w},${yFim + d} V${yBase} Z`;
}

function linhaRef(y, x0, x1, cor, tracejada = false) {
  return `<line x1="${x0}" x2="${x1}" y1="${y}" y2="${y}" stroke="${cor}" stroke-width="2"
          stroke-linecap="round" ${tracejada ? 'stroke-dasharray="6 5"' : ""} pointer-events="none"/>`;
}

// Marca curta sobre a coluna mostrando o tempo máximo da cor (com contorno para aparecer sobre a barra).
function marcaPrazo(x, w, y) {
  return `<line x1="${x - 5}" x2="${x + w + 5}" y1="${y}" y2="${y}" stroke="${COR.superficie}" stroke-width="6"
          stroke-linecap="round" pointer-events="none"/>` +
         `<line x1="${x - 5}" x2="${x + w + 5}" y1="${y}" y2="${y}" stroke="${COR.tinta}" stroke-width="2.5"
          stroke-linecap="round" pointer-events="none"/>`;
}

/**
 * Colunas verticais, uma por item.
 * opcoes: valor, cor, eixo (rótulo do eixo X), tip, rotulo (texto acima da coluna),
 *         prazo (marca do tempo máximo), media (linha tracejada), inteiro (eixo só com inteiros).
 */
function colunas(el, itens, opcoes) {
  if (!itens.length) return vazio(el);
  const { valor, cor, eixo, tip, rotulo, prazo, media, inteiro, altura = 240 } = opcoes;

  const topo = Math.max(1, ...itens.map(valor), ...itens.map(i => (prazo && prazo(i)) || 0), media || 0);
  const ticks = inteiro ? ticksInteiros(topo) : ticksBonitos(0, topo, 4);
  const W = el.clientWidth, H = altura;
  const m = { t: 24, r: 8, b: 30, l: margemEsquerda(ticks) };
  const y = escala(0, ticks.at(-1), H - m.b, m.t);
  const banda = (W - m.l - m.r) / itens.length;
  const larg = Math.max(2, Math.min(44, banda * 0.6));
  const passo = passoRotulos(itens.length, W - m.l - m.r, 56);

  let s = gradeY(ticks, y, m.l, W - m.r);
  itens.forEach((item, i) => {
    const v = valor(item), p = prazo ? prazo(item) : 0;
    const x = m.l + i * banda, xc = x + (banda - larg) / 2;
    s += `<g class="banda" data-tip="${tip(item)}">
      <rect class="alvo" x="${x}" y="${m.t}" width="${banda}" height="${H - m.t - m.b}"/>
      <path d="${coluna(xc, larg, y(0), y(v))}" fill="${cor(item)}"/>
      ${p > 0 ? marcaPrazo(xc, larg, y(p)) : ""}
    </g>`;
    if (rotulo && banda >= 28) {
      // Rótulo logo acima da coluna; só sobe para cima da marca de prazo quando encostaria nela.
      const yRotulo = p > 0 && Math.abs(y(v) - y(p)) < 18 ? Math.min(y(v), y(p)) : y(v);
      s += `<text class="rotulo" x="${x + banda / 2}" y="${yRotulo - 8}" text-anchor="middle">${rotulo(item)}</text>`;
    }
    if (i % passo === 0) s += `<text x="${x + banda / 2}" y="${H - 9}" text-anchor="middle">${eixo(item)}</text>`;
  });
  if (media != null) s += linhaRef(y(media), m.l, W - m.r, COR.primaria, true);
  desenharSvg(el, H, s);
}

/** Linha com área, para séries ao longo dos atendimentos (ex.: média acumulada). pontos: [{eixo, valor, tip}] */
function linha(el, pontos, { altura = 220, rotuloFinal } = {}) {
  if (!pontos.length) return vazio(el);

  const serie = pontos.map(p => p.valor), n = serie.length;
  const W = el.clientWidth, H = altura;
  const ticks = ticksBonitos(0, Math.max(1, ...serie), 4);
  const m = { t: 14, r: 76, b: 30, l: margemEsquerda(ticks) };
  const y = escala(0, ticks.at(-1), H - m.b, m.t);
  const banda = (W - m.l - m.r) / n;
  const xc = i => m.l + banda * (i + 0.5);
  const passo = passoRotulos(n, W - m.l - m.r, 56);

  let s = gradeY(ticks, y, m.l, W - m.r);
  const caminho = serie.map((v, i) => `${i ? "L" : "M"}${xc(i)},${y(v)}`).join(" ");
  s += `<path d="${caminho} L${xc(n - 1)},${y(0)} L${xc(0)},${y(0)} Z" fill="${COR.primaria}" fill-opacity="0.08"/>`;
  s += `<path d="${caminho}" fill="none" stroke="${COR.primaria}" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"/>`;

  pontos.forEach((p, i) => {
    if (i % passo === 0) s += `<text x="${xc(i)}" y="${H - 9}" text-anchor="middle">${p.eixo}</text>`;
    s += `<g class="banda" data-tip="${p.tip}">
      <rect class="alvo" x="${m.l + i * banda}" y="${m.t}" width="${banda}" height="${H - m.t - m.b}"/>
      <line class="mira" x1="${xc(i)}" x2="${xc(i)}" y1="${m.t}" y2="${H - m.b}"/>
      <circle cx="${xc(i)}" cy="${y(p.valor)}" r="4.5" fill="${COR.primaria}" stroke="${COR.superficie}" stroke-width="2"
              opacity="${n <= 40 || i === n - 1 ? 1 : 0}"/>
    </g>`;
  });
  if (rotuloFinal) {
    s += `<text class="rotulo" x="${xc(n - 1) + 10}" y="${y(serie[n - 1])}" dominant-baseline="middle">${rotuloFinal}</text>`;
  }
  desenharSvg(el, H, s);
}

/** Barras horizontais em HTML (rótulo · barra · valor). itens: [{rotulo, valor, cor, tip}] */
function barrasH(itens, formatar = String) {
  const max = Math.max(1, ...itens.map(i => i.valor));
  return `<div class="barras-h">${itens.map(i => `
    <div class="barra-h banda" data-tip="${i.tip || ""}">
      <span>${i.rotulo}</span>
      <div class="trilho"><i style="width:${(i.valor / max) * 100}%;background:${i.cor}"></i></div>
      <b>${formatar(i.valor)}</b>
    </div>`).join("")}</div>`;
}
