# Acao Sankhya - Altera Local de Devolucao

Este pacote gera um `jar` com tres classes de botao de acao (`Rotina Java`) para uso no Sankhya:

- `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucao30100`
- `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucaoSelecionavel`
- `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucao10100`
- `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucao20100`
- `br.com.bela.sankhya.acao.AcaoMarcarPedidoNaoPendente`
- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucaoRma20100`
- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucaoSelecionavel`
- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucao10100`
- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucao20100`
- `br.com.bela.sankhya.acao.AcaoCorrigirRejeicaoIbsCbs`

Importante:

- Nao selecione classes de suporte/internas no cadastro.
- Para um unico botao com escolha de local, use `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucaoSelecionavel`.

## Regra aplicada

A rotina replica a alteracao observada no monitoramento:

```sql
UPDATE TGFITE
   SET CODLOCALORIG = <local>
 WHERE NUNOTA = <nunota selecionada>
```

Ou seja, o botao atua nos itens da nota selecionada no contexto `TGFCAB/CabecalhoNota`.

## Build local

No PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File ".\belamgz\sankhya-acao-local\build-jar.ps1"
```

## Cadastro sugerido no Sankhya

## Fluxo alinhado ao BHZ

Etapa 1:

- Classe: `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucao30100`
- Efeito: move a devolucao para a triagem `30100`

Etapa 2:

- Classe: `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucao10100`
- Efeito: gera a transferencia WMS a partir da triagem `30100` com destino `10100`

Tabela/instancia:

- Tabela: `TGFCAB`
- Instancia: `CabecalhoNota`
- Tipo: `Rotina Java`

Classe com escolha por parametro:

- Classe: `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucaoSelecionavel`
- Parametro:
  - Descricao: `Local destino`
  - Nome: `CODLOCALDESTINO`
  - Tipo: `Inteiro`
  - Valor esperado: `10100` ou `20100`

Classes fixas sem parametro:

- `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucao10100`
- `br.com.bela.sankhya.acao.AcaoAlteraLocalDevolucao20100`

## Botao para marcar pedido como nao pendente

Tabela/instancia:

- Tabela: `TGFCAB`
- Instancia: `CabecalhoNota`
- Tipo: `Rotina Java`

Classe:

- `br.com.bela.sankhya.acao.AcaoMarcarPedidoNaoPendente`

Regra:

- aceita uma ou mais linhas selecionadas
- valida se `TIPMOV = 'P'`
- atualiza `TGFCAB.PENDENTE = 'N'`

## Botao para recalcular IBS/CBS e corrigir rejeicoes 232/1076

Tabela/instancia:

- Tabela: `TGFCAB`
- Instancia: `CabecalhoNota`
- Tipo: `Rotina Java`

Classe:

- `br.com.bela.sankhya.acao.AcaoCorrigirRejeicaoIbsCbs`

Regra:

- aceita uma ou mais notas selecionadas
- chama a procedure existente `STP_CORRIGIR_NFE_REJEICAO`
- a procedure corrige `TGFPAR.CLASSIFICMS` quando estiver `X`
- depois executa `STP_CALCULAVITEM_IBS_CBS(P_NUNOTA, 0)`

Uso sugerido:

- aplicar em notas com retorno de autorizacao contendo rejeicao `232` ou `1076`
- apos executar o botao, reenviar a nota para autorizacao

## Botao de geracao aceitando RMA 20100

Classe:

- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucaoRma20100`

Regra:

- se a nota ja tiver item positivo em `30100`, delega para `br.com.sankhya.bhz.wms.AcaoGeraEntradaDevolucao`
- se nao tiver item em `30100`, mas tiver item positivo em `20100`, move para `30100` e depois delega para a rotina BHZ
- se nao tiver item em `30100` nem `20100`, bloqueia

## Botao unico de entrada com escolha de destino

Classe:

- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucaoSelecionavel`

Parametro:

- Descricao: `Local destino`
- Nome: `CODLOCALDESTINO`
- Tipo: `Opcoes`
- Valores permitidos: `10100` e `20100`

Regra:

- aceita nota com item positivo em `30100`
- se a nota estiver em `20100`, move primeiro para `30100` para satisfazer a validacao da BHZ
- chama a rotina original `br.com.sankhya.bhz.wms.AcaoGeraEntradaDevolucao`
- se o destino escolhido for `20100`, ajusta a nota gerada para que os itens de destino fiquem em `20100`

## Entrada sem formulario, igual ao estilo do BHZ

Classes:

- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucao10100`
- `br.com.bela.sankhya.acao.AcaoGeraEntradaDevolucao20100`

Regra:

- nao usam parametro
- aparecem e executam como um botao direto
- aceitam origem em `30100`
- se a origem estiver em `20100`, movem para `30100` antes de chamar a rotina BHZ
- geram a entrada com destino fixo no local da propria classe
