# Pronto-Atendimento — Simulador de espera

Dashboard que simula a espera dos pacientes de um pronto-socorro. O usuário escolhe a cor de risco, digita
quantos pacientes estão nessa cor e quanto tempo esperaram até o médico. O Java calcula as estatísticas e
o dashboard mostra o resultado em cartões e gráficos.

## Como rodar

Precisa do Java 22 ou mais novo (o backend tem mais de um arquivo). Na pasta do projeto:

```
java backend/Dashboard.java
```

Depois abra http://localhost:8081. No VS Code, também dá para rodar o `Dashboard.java` direto pelo botão Run.

Para conferir os cálculos com resultados feitos à mão:

```
java backend/TesteEstatistica.java
```

## Como usar

1. Clique na **cor de risco**.
2. Digite a **quantidade de pacientes** nessa cor.
3. Digite a **espera até o médico**, em minutos, e clique em **Adicionar** (ou tecle Enter).
4. O dashboard envia os grupos para o Java e mostra o resultado. Repita para outras cores, ou clique em
   **Carregar exemplo** para ver um plantão de 17 pacientes.

Cada grupo vale "quantidade" pacientes com a mesma espera: 5 amarelos com 30 min viram cinco esperas de 30 min
nas contas.

## Pastas

```
backend/
  Dashboard.java         servidor HTTP: entrega a página e recebe os grupos em POST /calcular
  Simulacao.java         cores de risco, regra de prazo e montagem do resultado
  Estatistica.java       média, mediana, desvio padrão, mínimo, máximo, frequências e moda
  TesteEstatistica.java  confere os cálculos com resultados feitos à mão
frontend/
  index.html             estrutura da página: formulário, cartões e áreas dos gráficos
  css/style.css
  js/graficos.js         gráficos de colunas em SVG
  js/app.js              formulário, chamada ao Java e desenho do resultado
```

## O que o Java calcula

| Indicador | Cálculo |
|---|---|
| Pacientes | soma das quantidades |
| Espera média, mediana, desvio padrão, menor e maior | sobre a espera de cada paciente |
| Atendidos no prazo | pacientes com espera abaixo do tempo máximo da cor ÷ pacientes com prazo (o vermelho não tem prazo) |
| Prazos estourados | pacientes com espera igual ou acima do tempo máximo da cor |
| Cor predominante | moda das cores dos pacientes |
| Fast track | % de pacientes verdes e azuis |
| Por cor | pacientes, espera média e estourados de cada cor |

## Regras de negócio

| Cor      | Prioridade | Tempo máximo | Fila       |
|----------|------------|--------------|------------|
| Vermelho | 1          | imediato     | Emergência |
| Laranja  | 2          | 10 min       | Principal  |
| Amarelo  | 3          | 60 min       | Principal  |
| Verde    | 4          | 120 min      | Fast track |
| Azul     | 5          | 240 min      | Fast track |

O desvio padrão é populacional (divide por n): os pacientes simulados são o grupo inteiro, não uma amostra.
