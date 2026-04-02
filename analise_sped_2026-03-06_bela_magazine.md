# Analise SPED - Bela Magazine

## Fonte analisada
- Arquivo: `C:\Users\jacks\Downloads\SPED 06-03.pdf`
- Contribuinte no relatorio: `BELA MAGAZINE COMER VAREJ MOVEIS LTDA`
- CNPJ: `30.048.726/0001-65`
- IE: `083424407`
- Periodo da escrituracao: `01/02/2026 a 28/02/2026`
- Data/hora da avaliacao no relatorio: `06/03/2026 13:07`
- Total de erros: `71`

## Resumo por tipo de erro
| Tipo de erro (mensagem do validador) | Qtde |
|---|---:|
| Campo nao pode conter espacos em branco no inicio nem no final | 1 |
| Registros C101/D101 so podem ser informados quando UF do COD_PART for diferente da UF do registro 0000 | 1 |
| Duplicidade de ocorrencia de chave | 16 |
| Campo com valor diferente dos valores validos | 1 |
| Para documentos de entrada (IND_OPER=0), campo obrigatorio para COD_MOD 57/63/67 | 2 |
| Somatorio de E116 diferente de E110 | 1 |
| Soma de VL_RECOL_DIFAL/DEB_ESP_DIFAL/VL_RECOL_FCP/DEB_ESP_FCP diferente de VL_OR (E316) | 26 |
| VL_TOT_DEBITOS_DIFAL diferente da soma esperada por C101/D101 | 8 |
| VL_TOT_DEB_FCP diferente da soma esperada por C101/D101 | 3 |
| Campo obrigatorio para EFD ICMS/IPI perfil A ou B | 12 |

## Principais achados
1. Erros de apuracao DIFAL/FCP no bloco E3xx (37 ocorrencias: 26 + 8 + 3).
2. Duplicidades de documentos/chaves (16 ocorrencias), principalmente em registros de transporte/entrada.
3. Falta de conta contabil (COD_CTA) no inventario (H010) para perfil A/B (12 ocorrencias).
4. Erros pontuais de qualidade cadastral/regra (espaco em endereco, C101 indevido, valor invalido).

## Evidencias observadas nas paginas de detalhe
- Duplicidades em `D100` com mensagem de chave duplicada (`NUM_DOC, COD_MOD, COD_SIT, SER, SUB, CHV_CTE, COD_PAR`).
- Duplicidade em `C120` (`NUM_DOC_IMP, NUM_ACDRAW`).
- Em documentos de entrada com modelo de transporte, faltas de campos obrigatorios (ex.: `COD_MUN_ORIG`/`COD_MUN_DEST`).
- Divergencias recorrentes em `E310` para:
  - `10 - VL_RECOL`
  - `4 - VL_TOT_DEBITOS_DIFAL`
  - `14 - VL_TOT_DEB_FCP`
- Falta de `10 - COD_CTA` em varios registros `H010`.

## Prioridade de correcao sugerida
1. **Bloco E3xx (DIFAL/FCP)**: corrigir calculo e formacao de `E310/E316`.
2. **Duplicidades**: eliminar duplicacao de notas/documentos no processo de geracao do SPED.
3. **H010 COD_CTA**: completar vinculacao contabil de inventario para perfil A/B.
4. **Ajustes pontuais**: limpar espacos, validar C101/D101 por UF, corrigir valor de campo invalido.

## Checklist rapido para reprocesso
1. Regerar apuracao e conferir se `E310` bate com `E316`.
2. Conferir regras DIFAL/FCP para UF igual/diferente do informante.
3. Validar deduplicacao antes de montar registros `C120/D100`.
4. Preencher `COD_CTA` no inventario (`H010`).
5. Revalidar no PVA e comparar novo total de erros.

