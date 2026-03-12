# Evento Sankhya - Vinculo Pedido x XML (Marketplace)

Classe Java:
- `br.com.bela.sankhya.evento.VinculaPedidoMarketplaceXmlTGFCAB`

## Objetivo
Vincular automaticamente o faturamento (TGFCAB) gerado a partir de XML importado (TGFIXN) ao pedido de marketplace.

Fluxo da classe:
1. Roda no evento da `TGFCAB` em `After Insert` e `After Update`.
2. Busca em `TGFIXN` o XML processado (`STATUS=5`) da nota.
3. Extrai `xPed` do XML.
4. Localiza pedido (`TIPMOV='P'`) por campos de marketplace (`AD_PEDIDOMKTPLACE`, `BH_CODEMKT`, etc).
5. Preenche `TGFIXN.AD_NUNOTAORIG`.
6. Cria vinculo item-a-item em `TGFVAR` sem duplicar.

## Cadastro no Sankhya
No Dicionario de Dados > `TGFCAB` > Eventos:
- Tipo: `Rotina Java`
- Momentos: `After Insert` e `After Update`
- Classe: `br.com.bela.sankhya.evento.VinculaPedidoMarketplaceXmlTGFCAB`

## Arquivos versionados
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/AbstractEventoProgramavel.java`
- `sankhya-evento/src/main/java/br/com/bela/sankhya/evento/VinculaPedidoMarketplaceXmlTGFCAB.java`
