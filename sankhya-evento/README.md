# Evento Sankhya - Vinculo Pedido x XML (Marketplace)

Classe Java:
- `br.com.bela.sankhya.evento.VinculaPedidoMarketplaceXml`
- (compatibilidade) `br.com.bela.sankhya.evento.VinculaPedidoMarketplaceXmlTGFCAB`

## Objetivo
Vincular automaticamente o faturamento (TGFCAB) gerado a partir de XML importado (TGFIXN) ao pedido de marketplace.

Fluxo da classe:
1. Roda no evento da `TGFCAB` em `After Insert` e `After Update`.
2. Busca em `TGFIXN` o XML (`STATUS in 4,5`) da nota.
3. Extrai `xPed` do XML.
4. Localiza pedido (`TIPMOV='P'`) por campos de marketplace (`AD_PEDIDOMKTPLACE`, `BH_CODEMKT`, etc).
5. Preenche `TGFIXN.AD_NUNOTAORIG`.
6. Cria vinculo item-a-item em `TGFVAR` sem duplicar e remove vinculos concorrentes na mesma NF.
7. Em fallback ambiguo, aplica ranking (pedido sem faturamento previo, pendente, sem NF origem) para decidir automaticamente.
8. Copia `NUMNOTA` da NF no pedido (`TGFCAB.AD_NRONTOAORIGEM` e `TGFCAB.NUMNOTA`).
9. Marca pedido como nao pendente (`TGFCAB.PENDENTE = 'N'`) apos vinculo valido.
10. Em devolucao (`TIPMOV='D'`), preenche `TGFCAB.AD_NUNOTADEV` no pedido.

## Cadastro no Sankhya
No Dicionario de Dados > `TGFCAB` > Eventos:
- Tipo: `Rotina Java`
- Momentos: `After Insert` e `After Update`
- Classe: `br.com.bela.sankhya.evento.VinculaPedidoMarketplaceXml`

Recomendado tambem no Dicionario de Dados > `TGFIXN` > Eventos:
- Tipo: `Rotina Java`
- Momentos: `After Insert` e `After Update`
- Classe: `br.com.bela.sankhya.evento.VinculaPedidoMarketplaceXml`

## Validacao para compra sem numero/chave
Classe Java:
- `br.com.bela.sankhya.evento.BloqueiaLiberacaoCompraSemNumeroEChave`
- (compatibilidade) `br.com.bela.sankhya.evento.BloqueiaLiberacaoCompraSemNumeroEChaveTGFCAB`

Objetivo:
- impedir que a `TGFCAB` seja liberada (`STATUSNOTA = 'L'`) em compra (`TIPMOV = 'C'`) com TOP de entrada (`TGFTOP.ATUALEST = 'E'`) sem `NUMNOTA` e `CHAVENFE`.

Cadastro no Sankhya:
- Dicionario de Dados > `TGFCAB` > Eventos
- Tipo: `Rotina Java`
- Momentos: `Before Insert` e `Before Update`
- Classe: `br.com.bela.sankhya.evento.BloqueiaLiberacaoCompraSemNumeroEChave`

## Sincronismo de Dt. Entrada/Saida na compra
Classe Java:
- `br.com.bela.sankhya.evento.SincronizaDtEntSaiCompra`
- (compatibilidade) `br.com.bela.sankhya.evento.SincronizaDtEntSaiCompraTGFCAB`
- Bio: `br.com.bio.sankhya.evento.SincronizaDtEntSaiCompra`
- Bio (compatibilidade): `br.com.bio.sankhya.evento.SincronizaDtEntSaiCompraTGFCAB`

Objetivo:
- manter `TGFCAB.DTENTSAI` sempre igual a `TGFCAB.DTNEG` em movimentos de compra (`TIPMOV = 'C'`);
- preencher automaticamente no cadastro do lancamento;
- impedir persistencia de edicao manual, porque no `Before Update` o valor volta para `DTNEG`.

Cadastro no Sankhya:
- Dicionario de Dados > `TGFCAB` > Eventos
- Tipo: `Rotina Java`
- Momentos: `Before Insert` e `Before Update`
- Classe: `br.com.bela.sankhya.evento.SincronizaDtEntSaiCompra`

Cadastro recomendado na Biologistica:
- Dicionario de Dados > `TGFCAB` > Eventos
- Tipo: `Rotina Java`
- Momentos: `Before Insert` e `Before Update`
- Classe: `br.com.bio.sankhya.evento.SincronizaDtEntSaiCompraTGFCAB`

## Sincronismo de custo na transferencia para empresa 5
Classe Java:
- `br.com.bela.sankhya.evento.SincronizaCustoTransferenciaEmp5`
- (compatibilidade) `br.com.bela.sankhya.evento.SincronizaCustoTransferenciaEmp5TGFCAB`

Objetivo:
- quando a transferencia entre empresas for efetivada na `TGFCAB` da empresa `5`, copiar o ultimo custo da empresa `1` para a `TGFCUS` da empresa `5`;
- gerar a linha de custo com `DTATUAL = DTNEG` da transferencia, para que a analise de rentabilidade passe a encontrar custo real em vez do fallback `0,01`;
- processar apenas documentos de transferencia (`TIPMOV = 'T'`, `CODTIPOPER = 78`) e somente quando a nota estiver liberada.

Cadastro no Sankhya:
- Dicionario de Dados > `TGFCAB` > Eventos
- Tipo: `Rotina Java`
- Momentos: `After Insert` e `After Update`
- Classe: `br.com.bela.sankhya.evento.SincronizaCustoTransferenciaEmp5TGFCAB`

## Bloqueio de item em requisicao confirmada
Classe Java:
- `br.com.bela.sankhya.evento.BloqueiaEdicaoRequisicaoConfirmada`
- (compatibilidade) `br.com.bela.sankhya.evento.BloqueiaEdicaoRequisicaoConfirmadaTGFITE`

Objetivo:
- em `TGFITE`, bloquear exclusao de item quando o cabecalho da nota estiver como requisicao confirmada;
- bloquear alteracao de campos sensiveis do item quando `TGFCAB.TIPMOV = 'J'` e `TGFCAB.STATUSNOTA = 'L'`;
- manter liberadas as alteracoes operacionais de entrega, porque a regra so barra mudancas em `QTDNEG`, `VLRUNIT`, `VLRTOT`, `VLRDESC`, `PERCDESC`, `CODPROD`, `CODLOCALORIG` e `CONTROLE`.

Cadastro no Sankhya:
- Dicionario de Dados > `TGFITE` > Eventos
- Tipo: `Rotina Java`
- Momentos: `Before Update` e `Before Delete`
- Classe: `br.com.bela.sankhya.evento.BloqueiaEdicaoRequisicaoConfirmada`

## Bloqueio de cabecalho em requisicao confirmada
Classe Java:
- `br.com.bela.sankhya.evento.BloqueiaCabecalhoRequisicaoConfirmada`
- (compatibilidade) `br.com.bela.sankhya.evento.BloqueiaCabecalhoRequisicaoConfirmadaTGFCAB`

Objetivo:
- em `TGFCAB`, bloquear alteracao de `DTNEG`, `CODPARC` e `CODCENCUS` quando a requisicao estiver confirmada (`TIPMOV = 'J'` e `STATUSNOTA = 'L'`);
- preservar os campos de entrega que nao fazem parte desse conjunto.

Cadastro no Sankhya:
- Dicionario de Dados > `TGFCAB` > Eventos
- Tipo: `Rotina Java`
- Momento: `Before Update`
- Classe: `br.com.bela.sankhya.evento.BloqueiaCabecalhoRequisicaoConfirmada`

## Ignorar liberacao fiscal indevida no uso e consumo
Classe Java:
- `br.com.bela.sankhya.evento.IgnoraLiberacaoDivergenciaFiscalUsoConsumo`
- (compatibilidade) `br.com.bela.sankhya.evento.IgnoraLiberacaoDivergenciaFiscalUsoConsumoTSILIB`

Objetivo:
- remover automaticamente a liberacao criada na `TSILIB` pelo importador XML para a `TOP 2101 - NFE COMPRA USO E CONSUMO`;
- atuar apenas quando a observacao contiver divergencia de `NCM` e/ou `CEST`;
- manter a liberacao se tambem houver divergencia de `Origem` ou `FCI`.

Cadastro no Sankhya:
- Dicionario de Dados > `TSILIB` > Eventos
- Tipo: `Rotina Java`
- Momentos: `After Insert` e `After Update`
- Classe: `br.com.bela.sankhya.evento.IgnoraLiberacaoDivergenciaFiscalUsoConsumoTSILIB`

## Promover status da importacao 2101 sem liberacao pendente
Classe Java:
- `br.com.bela.sankhya.evento.PromoveStatusImportacaoUsoConsumo`
- (compatibilidade) `br.com.bela.sankhya.evento.PromoveStatusImportacaoUsoConsumoTGFIXN`

Objetivo:
- promover `TGFIXN.STATUS` de `4` para `5` nas importacoes da `TOP 2101 - NFE COMPRA USO E CONSUMO`;
- atuar somente quando a nota ja existir e nao houver mais pendencias em `TSILIB`;
- evitar que o portal fique preso em “Aguardando liberacao...” mesmo sem liberacao aberta.

Cadastro no Sankhya:
- Dicionario de Dados > `TGFIXN` > Eventos
- Tipo: `Rotina Java`
- Momentos: `After Insert` e `After Update`
- Classe: `br.com.bela.sankhya.evento.PromoveStatusImportacaoUsoConsumoTGFIXN`

## Arquivos versionados
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/AbstractEventoProgramavel.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/BloqueiaCabecalhoRequisicaoConfirmada.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/BloqueiaCabecalhoRequisicaoConfirmadaTGFCAB.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/BloqueiaEdicaoRequisicaoConfirmada.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/BloqueiaEdicaoRequisicaoConfirmadaTGFITE.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/IgnoraLiberacaoDivergenciaFiscalUsoConsumo.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/IgnoraLiberacaoDivergenciaFiscalUsoConsumoTSILIB.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/PromoveStatusImportacaoUsoConsumo.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/PromoveStatusImportacaoUsoConsumoTGFIXN.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/SincronizaDtEntSaiCompra.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/SincronizaDtEntSaiCompraTGFCAB.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/SincronizaCustoTransferenciaEmp5.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/SincronizaCustoTransferenciaEmp5TGFCAB.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/VinculaPedidoMarketplaceXml.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/VinculaPedidoMarketplaceXmlTGFCAB.java`
