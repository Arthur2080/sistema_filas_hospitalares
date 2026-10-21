# Painel do Pronto-Socorro — Indicadores

Dashboard de análise estatística do Sistema de Gestão de Filas Hospitalares. Ele usa os dados de teste do banco
(`filas_hospitalares_banco_2_dados.sql`, 06/10/2026), aplica as regras do projeto Java e mostra os indicadores
que o documento do sistema pede: tempo porta–classificação e porta–médico, atendidos no prazo, chegadas por hora
e quem foi embora sem atendimento.

O visual segue o site de filas feito no Lovable: menu lateral, relógio simulado no topo, cartões e cores de risco.

## Como rodar

Precisa do Java 22 ou mais novo (o backend tem mais de um arquivo). Na pasta do projeto:

```
java backend/Dashboard.java
```

Depois abra http://localhost:8081. No VS Code, também dá para rodar o `Dashboard.java` direto pelo botão Run.

## Pastas

```
backend/
  Dashboard.java     servidor HTTP e o simulador (/calcular)
  Dados.java         dados de teste, iguais aos do SQL
  Indicadores.java   regras de negócio e cálculo dos indicadores (/api/painel)
  Estatistica.java   média, mediana, desvio padrão, frequências, moda e média acumulada
frontend/
  index.html         estrutura: menu, relógio e área das telas
  css/style.css
  js/graficos.js     gráficos em SVG
  js/telas.js        conteúdo de cada tela e o histórico do atendimento
  js/app.js          navegação, relógio simulado e simulador
```

## Telas

| Tela | O que mostra |
|---|---|
| Visão geral | totais por fila e situação, médias porta–classificação e porta–médico, % no prazo, evasão e alertas disparados |
| Tempos de espera | média, mediana, desvio, menor e maior tempo de porta–classificação, porta–médico e permanência |
| Prazos e alertas | quem aguarda consulta, com o tempo restante ou estourado, e o histórico de todos os alertas |
| Fluxo de chegadas | chegadas por hora, horário de pico, pacientes por fila e a fila de triagem |
| Desfechos | altas, internações, transferências e evasões, com o tempo de permanência |
| Atendimentos | lista completa; o botão Histórico abre a linha do tempo do paciente |
| Simulador | digite esperas por cor e veja os mesmos indicadores com as regras de prazo |

O **relógio simulado** começa em 06/10/2026 10:46. Ao avançar o relógio, os prazos são verificados de novo:
com +4 h (14:46) o alerta da Beatriz dispara, como no cenário de teste do sistema.

## Regras de negócio aplicadas

| Cor      | Prioridade | Tempo máximo | Fila       |
|----------|------------|--------------|------------|
| Vermelho | 1          | imediato     | Emergência |
| Laranja  | 2          | 10 min       | Principal  |
| Amarelo  | 3          | 60 min       | Principal  |
| Verde    | 4          | 120 min      | Fast track |
| Azul     | 5          | 240 min      | Fast track |

- Vale a classificação mais recente; nenhuma é apagada.
- Toda classificação, menos a vermelha, gera um alerta com o tempo máximo da cor.
- O alerta dispara quando o relógio atinge o limite e o paciente ainda aguarda consulta (`AlertaPrazo.verificar`).
- Filas médicas: pela prioridade da cor e, empatando, por quem chegou primeiro. Triagem: por ordem de chegada.
- Porta–classificação: da chegada até a primeira classificação. Porta–médico: da chegada até a primeira consulta.
- Atendidos no prazo: alertas encerrados que nunca dispararam, sobre o total de alertas encerrados.
- Evasão: atendimentos finalizados como evasão, sobre o total de finalizados.
