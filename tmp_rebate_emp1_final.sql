SET PAGESIZE 1000
SET LINESIZE 32767
SET FEEDBACK OFF
SET VERIFY OFF
SET HEADING ON
SET COLSEP ';'

WITH base AS (
    SELECT
        cab.nunota,
        cab.numnota,
        cab.codvend,
        ite.sequencia,
        (
            CASE
                WHEN (
                    CASE
                        WHEN cab.codvend = 34 THEN
                            ((NVL(ite.ad_txcomissaomkt, 0) / 100)
                              * ROUND(NVL(ite.ad_bm_vlrunitpricemkt, 0) * (ite.vlrtot / NULLIF(ite.ad_bm_vlrunitpricemkt, 0)), 2)
                            )
                            - ROUND(NVL(ite.ad_bm_vlrsalefeemkt, 0) * (ite.vlrtot / NULLIF(ite.ad_bm_vlrunitpricemkt, 0)), 2)
                        ELSE
                            CASE
                                WHEN NVL(ite.ad_vlrrebatemkt, 0) <> 0 THEN NVL(ite.ad_vlrrebatemkt, 0)
                                ELSE NVL(cab.ad_vlrrebatemkt, 0)
                            END
                    END
                ) < 1 THEN 0
                ELSE
                    CASE
                        WHEN cab.codvend = 34 THEN
                            ((NVL(ite.ad_txcomissaomkt, 0) / 100)
                              * ROUND(NVL(ite.ad_bm_vlrunitpricemkt, 0) * (ite.vlrtot / NULLIF(ite.ad_bm_vlrunitpricemkt, 0)), 2)
                            )
                            - ROUND(NVL(ite.ad_bm_vlrsalefeemkt, 0) * (ite.vlrtot / NULLIF(ite.ad_bm_vlrunitpricemkt, 0)), 2)
                        ELSE
                            CASE
                                WHEN NVL(ite.ad_vlrrebatemkt, 0) <> 0 THEN NVL(ite.ad_vlrrebatemkt, 0)
                                ELSE NVL(cab.ad_vlrrebatemkt, 0)
                            END
                    END
            END
            + NVL(NVL(com.vlrrebate, comp.vlrrebate), 0)
        ) AS rebate_item
    FROM sankhya.tgfcab cab
    INNER JOIN sankhya.tgfite ite
        ON ite.nunota = cab.nunota
       AND ite.vlrtot <> 0
    INNER JOIN sankhya.tgftop tp
        ON tp.codtipoper = cab.codtipoper
       AND tp.dhalter = cab.dhtipoper
    LEFT JOIN sankhya.ad_bhzcmkt com
        ON com.codprod = ite.codprod
       AND com.codvend = cab.codvend
       AND NVL(com.padrao, 'N') = 'N'
       AND cab.dtneg BETWEEN com.dtini AND com.dtfin
       AND ROUND(ite.vlrunit, 2) = ROUND(com.precovenda, 2)
    LEFT JOIN sankhya.ad_bhzcmkt comp
        ON comp.codprod = ite.codprod
       AND comp.codvend = cab.codvend
       AND comp.padrao = 'S'
    WHERE TRUNC(cab.dtneg) = DATE '2026-03-12'
      AND cab.codemp = 1
      AND tp.tipmov = 'V'
      AND NVL(cab.vlrnota, 0) <> 0
      AND (
            (tp.atualfin IN (1, -1) AND tp.tipatualfin = 'I' AND cab.statusnfe = 'A')
         OR cab.codtipoper IN (3100, 3103, 3105, 3113, 3119, 3225)
      )
), por_nota AS (
    SELECT
        nunota,
        numnota,
        codvend,
        SUM(rebate_item) AS soma_rebate_item,
        COUNT(*) AS qtd_itens,
        CASE
            WHEN codvend = 42 THEN
                CASE WHEN SUM(rebate_item) < 1 THEN 0 ELSE (SUM(rebate_item) / COUNT(*)) * 4 END
            ELSE
                SUM(rebate_item)
        END AS rebate_final_aplicado
    FROM base
    GROUP BY nunota, numnota, codvend
)
SELECT codvend, COUNT(*) qtd_notas, ROUND(SUM(rebate_final_aplicado), 2) total_rebate_final
FROM por_nota
GROUP BY codvend
ORDER BY codvend;

PROMPT TOTAL_GERAL_REBATE_FINAL
SELECT ROUND(SUM(rebate_final_aplicado), 2) total_geral
FROM por_nota;

EXIT
