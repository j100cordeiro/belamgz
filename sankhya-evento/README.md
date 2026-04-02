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

## Arquivos versionados
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/AbstractEventoProgramavel.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/VinculaPedidoMarketplaceXml.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/VinculaPedidoMarketplaceXmlTGFCAB.java`
