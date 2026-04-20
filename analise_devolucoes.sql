WITH VENDAS AS (
    SELECT
        CAB.AD_BM_DHINTEGRACAO,
        NVL(ITE.AD_BM_VLRUNITPRICEMKT,0) AD_BM_VLRUNITPRICEMKT,
        CAB.CODEMP,
        CAB.CODPARC,
        CAB.NUNOTA,
        CAB.DTNEG DTNEG,
        CAB.DTFATUR DTFATUR,
        CAB.NUMNOTA NRONOTA,
        NVL(CAB.AD_CODMKT, CAB.BH_CODEMKT) BH_CODEMKT,
        CAB.CHAVENFE,
        CAB.QTDVOL,
        CAB.AD_DHINC,
        NVL(CASE WHEN CAB.CODVEND IN (14,15) THEN ITE.AD_VLRREPASSE ELSE CAB.AD_VLRREPASSE END,0) AS AD_VLRREPASSE,
        PED.NUMNOTA NROPEDIDO,
        PAR.RAZAOSOCIAL,
        PAR.CGC_CPF,
        TP.CODTIPOPER,
        NVL(AVG(CAB.VLRFRETE) OVER (),0) AS FRETE_MEDIO,

        /* TXMKT */
        NVL(CASE
            WHEN ITE.AD_QTDPORTAL IS NOT NULL THEN (5 * ITE.AD_QTDPORTAL) /
                 (SELECT COUNT(CODPROD)
                    FROM TGFITE
                   WHERE NUNOTA = ITE.NUNOTA
                     AND NVL(AD_CODPRODORIG,0) = NVL(ITE.AD_CODPRODORIG,0))
            ELSE (5 * QTDNEG) /
                 (SELECT COUNT(CODPROD)
                    FROM TGFITE
                   WHERE NUNOTA = ITE.NUNOTA
                     AND NVL(AD_CODPRODORIG,0) = NVL(ITE.AD_CODPRODORIG,0))
        END,0) AS TXMKT,

        /* COMISSAO_PADRAO / BASE_COMISSAO */
        NVL((
            (CASE
                WHEN TRANSP.CODPARC = 220344 THEN 0
                ELSE ROUND(
                        ITE.VLRTOT * (
                            CASE
                                WHEN CAB.VLRFRETE > 0 THEN CAB.VLRFRETE /
                                    (CASE WHEN (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA) = 0
                                          THEN 1
                                          ELSE (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA)
                                     END)
                                ELSE 0
                            END
                        ),
                        2
                     )
            END)
            + (CASE
                    WHEN VEN.CODVEND IN (42,29) THEN
                        ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
                    ELSE
                        ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
               END)
        ) * 0.18,0) AS COMISSAO_PADRAO,

        NVL((CASE
            WHEN TRANSP.CODPARC = 220344 THEN 0
            ELSE ROUND(
                    ITE.VLRTOT * (
                        CASE
                            WHEN CAB.VLRFRETE > 0 THEN CAB.VLRFRETE /
                                (CASE WHEN (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA) = 0
                                      THEN 1
                                      ELSE (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA)
                                 END)
                            ELSE 0
                        END
                    ),
                    2
                 )
         END)
         + (CASE
                WHEN VEN.CODVEND IN (42,29) THEN
                    ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
                ELSE
                    ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
            END),0) AS BASE_COMISSAO,

        TP.DESCROPER,
        TP.TIPMOV,
        CID.CODCID,
        CID.NOMECID,
        UFS.CODUF,
        UFS.UF,
        PAR.CEP,
        VEN.CODVEND,
        VEN.APELIDO,
        PRO.BH_MKTMODELO MODELO,
        OPTION_LABEL('TGFPRO','AD_TIME',PRO.AD_TIME) TIME,
        ITE.SEQUENCIA,
        ITE.CODPROD,
        PRO.DESCRPROD,
        GRU.DESCRGRUPOPROD,
        ITE.QTDNEG,
        NVL(CAB.BH_DESCONTO * (VLRTOT / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0)),0) BH_DESCONTO,
        TRUNC(ITE.VLRUNIT,2) VLRUNIT,
        ITE.VLRDESC,

        NVL(CASE
            WHEN VEN.CODVEND IN (42,29) THEN
                ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
            ELSE
                ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
        END,0) VLRTOT,

        NVL((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END,0) VLRTOTBRUTO,
        NVL(TRUNC(NVL(ITE.VLRIPI,0),2),0) VLRIPI,

        /* REBATE */
        NVL(CASE
            WHEN CASE
                     WHEN CAB.CODVEND = 34 THEN
                         ((ITE.AD_TXCOMISSAOMKT/100) * ROUND(ITE.AD_BM_VLRUNITPRICEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2))
                         - ROUND(ITE.AD_BM_VLRSALEFEEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2)
                     ELSE
                         CASE
                             WHEN NVL(ITE.AD_VLRREBATEMKT, 0) != 0 THEN NVL(ITE.AD_VLRREBATEMKT, 0)
                             ELSE NVL(NVL(CAB.AD_VLRREBATEMKT, COM.VLRREBATE), 0)
                         END
                 END < 1
            THEN 0
            ELSE CASE
                     WHEN CAB.CODVEND = 34 THEN
                         ((ITE.AD_TXCOMISSAOMKT/100) * ROUND(ITE.AD_BM_VLRUNITPRICEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2))
                         - ROUND(ITE.AD_BM_VLRSALEFEEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2)
                     ELSE
                         CASE
                             WHEN NVL(ITE.AD_VLRREBATEMKT, 0) != 0 THEN NVL(ITE.AD_VLRREBATEMKT, 0)
                             ELSE NVL(NVL(CAB.AD_VLRREBATEMKT, COM.VLRREBATE), 0)
                         END
                 END
        END,0) AS REBATE,

        PRO.AD_LINHA,
        TRANSP.RAZAOSOCIAL TRANSPORTADOR,
        TRANSP.CODPARC CODPARCTRANSP,

        /* VLRFRETE */
        NVL(CASE
            WHEN TRANSP.CODPARC IN (220344,984687,985588) THEN 0
            ELSE ROUND(
                    ITE.VLRTOT * (
                        CASE
                            WHEN CAB.VLRFRETE > 0 THEN CAB.VLRFRETE /
                                (CASE WHEN (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA) = 0
                                      THEN 1
                                      ELSE (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA)
                                 END)
                            ELSE 0
                        END
                    ),
                    2
                 )
        END,0) VLRFRETE,

        /* CUSTOFRETE */
        NVL(ROUND(
            ITE.VLRTOT *
            (
                CASE
                    WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (285038) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                    WHEN CAB.CODEMP = 5 THEN 0
                    ELSE NVL(NCT.VLRCTE,CONT.VLRCOTACAO)
                END
                +
                CASE
                    WHEN TRANSP.NOMEPARC LIKE '%AMAZON PRIME%'
                         OR (CAB.CODEMP IN (5,6) AND CAB.CODVEND = 32)
                    THEN (CASE
                              WHEN NVL(PRE.ADKG, 0) > 0 THEN
                                  PRE.VALOR + (
                                      (CASE
                                           WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                           THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                           ELSE PRO.PESOBRUTO
                                       END - PRE.PESOMIN) * PRE.ADKG
                                  )
                              ELSE PRE.VALOR
                          END * ITE.QTDNEG)
                    ELSE
                        CASE
                            WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                            WHEN CAB.CODPARCTRANSP IN (220344,224984,285038) THEN NVL(CAB.BH_CUSTOFRETE,0) - CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END
                            ELSE 0
                        END
                END
            ) / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0),
            2
        ),0) AS CUSTOFRETE,

        /* PERCCUSTOFRETE */
        NVL(ROUND(
            ITE.VLRTOT *
            (
                CASE
                    WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (285038) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                    WHEN CAB.CODEMP = 5 THEN 0
                    ELSE NVL(NCT.VLRCTE,CONT.VLRCOTACAO)
                END
                +
                CASE
                    WHEN TRANSP.NOMEPARC LIKE '%AMAZON PRIME%'
                         OR (CAB.CODEMP IN (5,6) AND CAB.CODVEND = 32)
                    THEN (CASE
                              WHEN NVL(PRE.ADKG, 0) > 0 THEN
                                  PRE.VALOR + (
                                      (CASE
                                           WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                           THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                           ELSE PRO.PESOBRUTO
                                       END - PRE.PESOMIN) * PRE.ADKG
                                  )
                              ELSE PRE.VALOR
                          END * ITE.QTDNEG)
                    ELSE
                        CASE
                            WHEN CAB.CODPARCTRANSP IN (220344,224984,285038,984687,985588) THEN NVL(CAB.BH_CUSTOFRETE,0) - CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END
                            ELSE 0
                        END
                END
            ) / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0),
            2
        ) /
        NULLIF(((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END),0) * 100,0) AS PERCCUSTOFRETE,

        /* CUSTOFRETEPREV */
        NVL(ROUND(
            ITE.VLRTOT *
            (
                CASE
                    WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (285038) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                    WHEN CAB.CODEMP = 5 THEN 0
                    ELSE NVL(CONT.VLRCOTACAO,0)
                END
                +
                CASE
                    WHEN TRANSP.NOMEPARC LIKE '%AMAZON PRIME%'
                         OR (CAB.CODEMP IN (5,6) AND CAB.CODVEND = 32)
                    THEN (CASE
                              WHEN NVL(PRE.ADKG, 0) > 0 THEN
                                  PRE.VALOR + (
                                      (CASE
                                           WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                           THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                           ELSE PRO.PESOBRUTO
                                       END - PRE.PESOMIN) * PRE.ADKG
                                  )
                              ELSE PRE.VALOR
                          END * ITE.QTDNEG)
                    ELSE
                        CASE
                            WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                            WHEN CAB.CODPARCTRANSP IN (220344,224984,285038) THEN NVL(CAB.BH_CUSTOFRETE,0) - CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END
                            ELSE 0
                        END
                END
            ) / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0),
            2
        ),0) AS CUSTOFRETEPREV,

        /* CUSTO / PERCCUSTOFIXO */
        NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0) CUSTO,
        (SELECT MAX(NUMDEC) FROM TSIPAR WHERE CHAVE = 'PERCCUSFIXO' AND CODUSU = 0) PERCCUSTOFIXO,

        /* =========================================================
           ALTERACAO: PERCCOM PARA CODVEND=32
           - Se existir comissao ativa na AD_BHZCMKT (COM/COMP), usa ela
           - Senao, segue regra original (BHZ_OBTEMPERCCOMMKT etc)
           ========================================================= */
        CASE
            /* 14/15: regra existente */
            WHEN CAB.CODVEND IN (14, 15) AND ITE.AD_TXCOMISSAOMKT IS NOT NULL THEN
                ITE.AD_TXCOMISSAOMKT / NULLIF(
                    (SELECT COUNT(*)
                       FROM TGFITE I2
                      WHERE I2.NUNOTA = ITE.NUNOTA
                        AND NVL(I2.AD_CODPRODORIG,0) = NVL(ITE.AD_CODPRODORIG,0)),
                    0
                )

            /* CODVEND=32: se promo/comissao ativa na AD_BHZCMKT, usa percentual da tabela */
            WHEN CAB.CODVEND = 32 AND NVL(COM.COMISSAO, COMP.COMISSAO) IS NOT NULL THEN
                NVL(COM.COMISSAO, COMP.COMISSAO)

            /* Demais: regra original */
            ELSE
                CASE
                    WHEN CAB.CODVEND IN (42,29,32) THEN BHZ_OBTEMPERCCOMMKT(CAB.NUNOTA,ITE.SEQUENCIA,CAB.CODVEND,0)
                    WHEN COMFAIXA.PERCCOM IS NOT NULL THEN COMFAIXA.PERCCOM
                    WHEN CAB.CODVEND != 34
                         AND ITE.VLRUNIT = NVL(COM.PRECOVENDA, COMP.PRECOVENDA)
                         AND NVL(NVL(COM.COMISSAO, COMP.COMISSAO), 0) > 0
                    THEN NVL(COM.COMISSAO, COMP.COMISSAO)
                    ELSE NVL(
                             CASE
                                 WHEN NVL(ITE.AD_TXCOMISSAOMKT, 0) = 0 THEN CAB.AD_TXCOMISSAOMKT
                                 ELSE NVL(ITE.AD_TXCOMISSAOMKT, 0)
                             END,
                             (CASE
                                  WHEN CAB.AD_COMISSAOMKT > 0 THEN CAB.AD_COMISSAOMKT
                                  ELSE
                                      (CASE
                                           WHEN CAB.CODVEND IN (5,34) THEN
                                               (CAB.VLRNOTA - NVL(CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END,0))
                                               * (NVL(CAB.AD_TXCOMISSAOMKT,VEN.COMGER)/100)
                                           ELSE CAB.VLRNOTA * (NVL(CAB.AD_TXCOMISSAOMKT,VEN.COMGER)/100)
                                       END)
                              END / NULLIF(CAB.VLRNOTA,0)
                             ) * 100
                         )
                END
        END PERCCOM,

        /* Flag/percentual da tabela para uso no GROUP BY final (CODVEND=32) */
        CASE
            WHEN CAB.CODVEND = 32 THEN NVL(COM.COMISSAO, COMP.COMISSAO)
            ELSE NULL
        END AS PERCCOM_TABELA_32,

        ITE.AD_TXCOMISSAOMKT,
        CAB.AD_TXCOMISSAOMKT COMIS,
        CAB.VLRNOTA,

        /* PIS/COFINS */
        NVL(((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.0165,0) VLRPIS,
        NVL(((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.076,0) VLRCOFINS,
        NVL((((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.0165)
        + (((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.076),0) VLRPISCOFINS,

        PRO.AD_PRECOMINIMO PRECOMINIMO,
        NVL((SELECT SUM(VLRCUPOM) FROM AD_BHZCVRI WHERE NUNOTA = ITE.NUNOTA AND SEQUENCIA = ITE.SEQUENCIA),0) REBATEBELA,
        PRO.FABRICANTE,
        NVL(ITE.AD_CUSTOFIXOMELI,0) CUSTOFIXOMELI

    FROM SANKHYA.TGFCAB CAB
    INNER JOIN SANKHYA.TGFITE ITE ON ITE.NUNOTA = CAB.NUNOTA AND ITE.VLRTOT != 0
    INNER JOIN SANKHYA.TGFPRO PRO ON PRO.CODPROD = ITE.CODPROD
    LEFT JOIN (SELECT NUNOTA, NUNOTAORIG FROM SANKHYA.TGFVAR GROUP BY NUNOTA, NUNOTAORIG) VAR ON VAR.NUNOTA = CAB.NUNOTA
    LEFT JOIN SANKHYA.TGFCAB PED ON PED.NUNOTA = VAR.NUNOTAORIG
    INNER JOIN SANKHYA.TGFPAR PAR ON PAR.CODPARC = CAB.CODPARC
    INNER JOIN SANKHYA.TSICID CID ON CID.CODCID = PAR.CODCID
    INNER JOIN SANKHYA.TSIUFS UFS ON UFS.CODUF = CID.UF
    INNER JOIN SANKHYA.TGFVEN VEN ON VEN.CODVEND = CAB.CODVEND
    INNER JOIN SANKHYA.TGFGRU GRU ON GRU.CODGRUPOPROD = PRO.CODGRUPOPROD
    LEFT JOIN SANKHYA.TGFPAR TRANSP ON TRANSP.CODPARC = CAB.CODPARCTRANSP
    LEFT JOIN (
        SELECT C2.NUNOTA, SUM(ROUND(C.VLRNOTA / NULLIF((SELECT SUM(VLRNOTA) FROM TGFNCT WHERE NUNOTA = NCT.NUNOTA),0) * C2.VLRNOTA,4)) VLRCTE
        FROM TGFNCT NCT
        INNER JOIN TGFCAB C2 ON NCT.CHAVENFE = C2.CHAVENFE
        INNER JOIN TGFCAB C ON C.NUNOTA = NCT.NUNOTA
        GROUP BY C2.NUNOTA
    ) NCT ON NCT.NUNOTA = CAB.NUNOTA
    LEFT JOIN (
        SELECT NUNOTA, MAX(AD_CUSTO) VLRCOTACAO
        FROM TGFCONT
        WHERE AD_MELHORENVIO = 'S'
        GROUP BY NUNOTA
    ) CONT ON CONT.NUNOTA = PED.NUNOTA
    INNER JOIN SANKHYA.TGFTOP TP ON TP.CODTIPOPER = CAB.CODTIPOPER AND TP.DHALTER = CAB.DHTIPOPER

    /* AD_BHZCMKT: comissao por produto/vendedor com vigencia */
    LEFT JOIN AD_BHZCMKT COM ON COM.CODPROD = ITE.CODPROD
                             AND COM.CODVEND = CAB.CODVEND
                             AND NVL(COM.PADRAO, 'N') = 'N'
                             AND CAB.DTNEG BETWEEN COM.DTINI AND COM.DTFIN
                             AND ROUND(ITE.VLRUNIT,2) = ROUND(COM.PRECOVENDA,2)
    LEFT JOIN AD_BHZCMKT COMP ON COMP.CODPROD = ITE.CODPROD
                             AND COMP.CODVEND = CAB.CODVEND
                             AND COMP.PADRAO = 'S'

    LEFT JOIN AD_BHZTAB TAB ON TAB.CODPARC = (
                                        CASE
                                            WHEN CAB.CODVEND = 34 THEN 220344
                                            WHEN CAB.CODVEND = 32 THEN 285038
                                            ELSE CAB.CODPARCTRANSP
                                        END)
                           AND ITE.VLRUNIT BETWEEN TAB.VLRMIN AND TAB.VLRMAX
                           AND TAB.VLRMIN IS NOT NULL
    LEFT JOIN AD_BHZPRE PRE ON PRE.NUTAB = TAB.NUTAB
                           AND PRE.CODPARC = (
                                        CASE
                                            WHEN CAB.CODVEND = 34 THEN 220344
                                            WHEN CAB.CODVEND = 32 THEN 285038
                                            ELSE CAB.CODPARCTRANSP
                                        END)
                           AND CASE
                                   WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                   THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                   ELSE PRO.PESOBRUTO
                               END BETWEEN PRE.PESOMIN AND PRE.PESOMAX

    LEFT JOIN (
        SELECT
            ITE.NUNOTA,
            ITE.SEQUENCIA,
            ROUND(
                (SUM(
                    CASE
                        WHEN ITE.VLRUNIT >= COMFAIXA.VLRFINAL THEN COMFAIXA.VLRFINAL - VLRINI
                        WHEN ITE.VLRUNIT < COMFAIXA.VLRFINAL THEN VLRUNIT - COMFAIXA.VLRINI + 0.01
                    END * (COMFAIXA.PERCCOM/100)
                ) * 100) / NULLIF(ITE.VLRTOT,0),
                2
            ) PERCCOM
        FROM TGFITE ITE
        INNER JOIN TGFCAB CAB ON CAB.NUNOTA = ITE.NUNOTA
        LEFT JOIN AD_BHZCOMFAIXA COMFAIXA ON COMFAIXA.CODPROD = ITE.CODPROD
                                         AND COMFAIXA.CODVEND = CAB.CODVEND
                                         AND COMFAIXA.DTREF = (
                                                SELECT MAX(DTREF)
                                                FROM AD_BHZCOMFAIXA
                                                WHERE CODPROD = ITE.CODPROD
                                                  AND CODVEND = CAB.CODVEND
                                                  AND DTREF <= CAB.DTNEG
                                            )
        WHERE CAB.TIPMOV IN ('P','V')
        GROUP BY ITE.NUNOTA, ITE.SEQUENCIA, ITE.VLRTOT
    ) COMFAIXA ON COMFAIXA.SEQUENCIA = ITE.SEQUENCIA
             AND COMFAIXA.NUNOTA = CAB.NUNOTA

    WHERE TRUNC(CAB.DTNEG) BETWEEN :PERIODO.INI AND :PERIODO.FIN
      AND NVL(CAB.VLRNOTA,0) != 0
      AND (:CODPROD IS NULL OR EXISTS (
                SELECT 1
                FROM TGFITE
                WHERE CODPROD = :CODPROD AND NUNOTA = CAB.NUNOTA
          ))
      AND PRO.AD_TIME IN :TIME
      AND (PRO.BH_MKTMODELO = :MODELO OR :MODELO IS NULL)
      AND (CAB.CODTIPOPER IN :CODTIPOPER)
      AND (CAB.NUMNOTA = :NUMNOTA OR :NUMNOTA IS NULL)
      AND (CAB.CODPARC = :CODPARC OR :CODPARC IS NULL)
      AND (CAB.CODEMP = :CODEMP OR :CODEMP IS NULL)
      AND CAB.CODVEND IN :CODVEND
      AND (CAB.NUNOTA = :NUNOTA OR :NUNOTA IS NULL)
      AND (PRO.AD_LINHA = :LINHA OR :LINHA IS NULL)
      AND PRO.FABRICANTE IN :FABRICANTE
      AND CASE
              WHEN CAB.CODEMP IN (5,6) AND CAB.CODVEND = 29 THEN 'MA'
              WHEN CAB.CODEMP IN (5,6) AND CAB.CODVEND IN (5,34) THEN 'M'
              WHEN CAB.CODEMP IN (5,6) AND CAB.CODVEND IN (32) THEN 'A'
              WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 'M'
              WHEN CAB.CODPARCTRANSP IN (285038) THEN 'A'
              ELSE 'P'
          END IN :TIPOENV
      AND (TP.TIPMOV = 'V' AND ((TP.ATUALFIN IN (1, -1) AND TP.TIPATUALFIN = 'I' AND CAB.STATUSNFE = 'A')
           OR CAB.CODTIPOPER IN (3100, 3103, 3105, 3113, 3119, 3225)))
      AND CAB.CODEMP != 5
UNION ALL
SELECT
        CAB.AD_BM_DHINTEGRACAO,
        ITE.AD_BM_VLRUNITPRICEMKT,
        CAB.CODEMP,
        CAB.CODPARC,
        CAB.NUNOTA,
        CAB.DTNEG DTNEG,
        CAB.DTFATUR DTFATUR,
        CAB.NUMNOTA NRONOTA,
        NVL(CAB.AD_CODMKT, CAB.BH_CODEMKT) BH_CODEMKT,
        CAB.CHAVENFE,
        CAB.QTDVOL,
        CAB.AD_DHINC,
        CASE WHEN CAB.CODVEND IN (14,15) THEN ITE.AD_VLRREPASSE ELSE CAB.AD_VLRREPASSE END AS AD_VLRREPASSE,
        CAB.NUMNOTA NROPEDIDO,
        PAR.RAZAOSOCIAL,
        PAR.CGC_CPF,
        TP.CODTIPOPER,
        AVG(CAB.VLRFRETE) OVER () AS FRETE_MEDIO,
        /* TXMKT */
        CASE
            WHEN ITE.AD_QTDPORTAL IS NOT NULL THEN (5 * ITE.AD_QTDPORTAL) /
                 (SELECT COUNT(CODPROD)
                    FROM TGFITE
                   WHERE NUNOTA = ITE.NUNOTA
                     AND NVL(AD_CODPRODORIG,0) = NVL(ITE.AD_CODPRODORIG,0))
            ELSE (5 * ITE.QTDNEG) /
                 (SELECT COUNT(CODPROD)
                    FROM TGFITE
                   WHERE NUNOTA = ITE.NUNOTA
                     AND NVL(AD_CODPRODORIG,0) = NVL(ITE.AD_CODPRODORIG,0))
        END AS TXMKT,
        /* COMISSAO_PADRAO / BASE_COMISSAO */
        (
            (CASE
                WHEN TRANSP.CODPARC = 220344 THEN 0
                ELSE ROUND(
                        ITE.VLRTOT * (
                            CASE
                                WHEN CAB.VLRFRETE > 0 THEN CAB.VLRFRETE /
                                    (CASE WHEN (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA) = 0
                                          THEN 1
                                          ELSE (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA)
                                     END)
                                ELSE 0
                            END
                        ),
                        2
                     )
            END)
            + (CASE
                    WHEN VEN.CODVEND IN (42,29) THEN
                        ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
                    ELSE
                        ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
               END)
        ) * 0.18 AS COMISSAO_PADRAO,

        (CASE
            WHEN TRANSP.CODPARC = 220344 THEN 0
            ELSE ROUND(
                    ITE.VLRTOT * (
                        CASE
                            WHEN CAB.VLRFRETE > 0 THEN CAB.VLRFRETE /
                                (CASE WHEN (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA) = 0
                                      THEN 1
                                      ELSE (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA)
                                 END)
                            ELSE 0
                        END
                    ),
                    2
                 )
         END)
         + (CASE
                WHEN VEN.CODVEND IN (42,29) THEN
                    ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
                ELSE
                    ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
            END) AS BASE_COMISSAO,

        TP.DESCROPER,
        TP.TIPMOV,
        CID.CODCID,
        CID.NOMECID,
        UFS.CODUF,
        UFS.UF,
        PAR.CEP,
        VEN.CODVEND,
        VEN.APELIDO,
        PRO.BH_MKTMODELO MODELO,
        OPTION_LABEL('TGFPRO','AD_TIME',PRO.AD_TIME) TIME,
        ITE.SEQUENCIA,
        ITE.CODPROD,
        PRO.DESCRPROD,
        GRU.DESCRGRUPOPROD,
        ITE.QTDNEG,
        CAB.BH_DESCONTO * (ITE.VLRTOT / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0)) BH_DESCONTO,
        TRUNC(ITE.VLRUNIT,2) VLRUNIT,
        ITE.VLRDESC,

        CASE
            WHEN VEN.CODVEND IN (42,29) THEN
                ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
            ELSE
                ((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END)
        END VLRTOT,

        (ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END VLRTOTBRUTO,
        TRUNC(NVL(ITE.VLRIPI,0),2) VLRIPI,

        /* REBATE */
        CASE
            WHEN CASE
                     WHEN CAB.CODVEND = 34 THEN
                         ((ITE.AD_TXCOMISSAOMKT/100) * ROUND(ITE.AD_BM_VLRUNITPRICEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2))
                         - ROUND(ITE.AD_BM_VLRSALEFEEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2)
                     ELSE
                         CASE
                             WHEN NVL(ITE.AD_VLRREBATEMKT, 0) != 0 THEN NVL(ITE.AD_VLRREBATEMKT, 0)
                             ELSE NVL(NVL(CAB.AD_VLRREBATEMKT, COM.VLRREBATE), 0)
                         END
                 END < 1
            THEN 0
            ELSE CASE
                     WHEN CAB.CODVEND = 34 THEN
                         ((ITE.AD_TXCOMISSAOMKT/100) * ROUND(ITE.AD_BM_VLRUNITPRICEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2))
                         - ROUND(ITE.AD_BM_VLRSALEFEEMKT*(ITE.VLRTOT/ITE.AD_BM_VLRUNITPRICEMKT),2)
                     ELSE
                         CASE
                             WHEN NVL(ITE.AD_VLRREBATEMKT, 0) != 0 THEN NVL(ITE.AD_VLRREBATEMKT, 0)
                             ELSE NVL(NVL(CAB.AD_VLRREBATEMKT, COM.VLRREBATE), 0)
                         END
                 END
        END AS REBATE,

        PRO.AD_LINHA,
        TRANSP.RAZAOSOCIAL TRANSPORTADOR,
        TRANSP.CODPARC CODPARCTRANSP,

        /* VLRFRETE */
        CASE
            WHEN TRANSP.CODPARC IN (220344,984687,985588) THEN 0
            ELSE ROUND(
                    ITE.VLRTOT * (
                        CASE
                            WHEN CAB.VLRFRETE > 0 THEN CAB.VLRFRETE /
                                (CASE WHEN (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA) = 0
                                      THEN 1
                                      ELSE (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA)
                                 END)
                            ELSE 0
                        END
                    ),
                    2
                 )
        END VLRFRETE,


         /* CUSTOFRETE */
        ROUND(
            ITE.VLRTOT *
            (
                CASE
                    WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (285038) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                    WHEN CAB.CODEMP = 5 THEN 0
                    ELSE NVL(NCT.VLRCTE,CONT.VLRCOTACAO)
                END
                +
                CASE
                    WHEN TRANSP.NOMEPARC LIKE '%AMAZON PRIME%'
                         OR (CAB.CODEMP IN (5,6) AND CAB.CODVEND = 32)
                    THEN (CASE
                              WHEN NVL(PRE.ADKG, 0) > 0 THEN
                                  PRE.VALOR + (
                                      (CASE
                                           WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                           THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                           ELSE PRO.PESOBRUTO
                                       END - PRE.PESOMIN) * PRE.ADKG
                                  )
                              ELSE PRE.VALOR
                          END * ITE.QTDNEG)
                    ELSE
                        CASE
                            WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                            WHEN CAB.CODPARCTRANSP IN (220344,224984,285038) THEN NVL(CAB.BH_CUSTOFRETE,0) - CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END
                            ELSE 0
                        END
                END
            ) / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0),
            2
        ) AS CUSTOFRETE,

        /* PERCCUSTOFRETE */
        ROUND(
            ITE.VLRTOT *
            (
                CASE
                    WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (285038) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                    WHEN CAB.CODEMP = 5 THEN 0
                    ELSE NVL(NCT.VLRCTE,CONT.VLRCOTACAO)
                END
                +
                CASE
                    WHEN TRANSP.NOMEPARC LIKE '%AMAZON PRIME%'
                         OR (CAB.CODEMP IN (5,6) AND CAB.CODVEND = 32)
                    THEN (CASE
                              WHEN NVL(PRE.ADKG, 0) > 0 THEN
                                  PRE.VALOR + (
                                      (CASE
                                           WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                           THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                           ELSE PRO.PESOBRUTO
                                       END - PRE.PESOMIN) * PRE.ADKG
                                  )
                              ELSE PRE.VALOR
                          END * ITE.QTDNEG)
                    ELSE
                        CASE
                            WHEN CAB.CODPARCTRANSP IN (220344,224984,285038,984687,985588) THEN NVL(CAB.BH_CUSTOFRETE,0) - CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END
                            ELSE 0
                        END
                END
            ) / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0),
            2
        ) /
        NULLIF(((ITE.VLRTOT + ITE.VLRIPI + ITE.VLRSUBST - ITE.VLRDESC) * CASE WHEN CAB.TIPMOV = 'D' THEN -1 ELSE 1 END),0) * 100 AS PERCCUSTOFRETE,

        /* CUSTOFRETEPREV */
        ROUND(
            ITE.VLRTOT *
            (
                CASE
                    WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (285038) THEN 0
                    WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                    WHEN CAB.CODEMP = 5 THEN 0
                    ELSE NVL(CONT.VLRCOTACAO,0)
                END
                +
                CASE
                    WHEN TRANSP.NOMEPARC LIKE '%AMAZON PRIME%'
                         OR (CAB.CODEMP IN (5,6) AND CAB.CODVEND = 32)
                    THEN (CASE
                              WHEN NVL(PRE.ADKG, 0) > 0 THEN
                                  PRE.VALOR + (
                                      (CASE
                                           WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                           THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                           ELSE PRO.PESOBRUTO
                                       END - PRE.PESOMIN) * PRE.ADKG
                                  )
                              ELSE PRE.VALOR
                          END * ITE.QTDNEG)
                    ELSE
                        CASE
                            WHEN CAB.CODPARCTRANSP IN (984687,985588) THEN 0
                            WHEN CAB.CODPARCTRANSP IN (220344,224984,285038) THEN NVL(CAB.BH_CUSTOFRETE,0) - CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END
                            ELSE 0
                        END
                END
            ) / NULLIF((SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = CAB.NUNOTA),0),
            2
        ) AS CUSTOFRETEPREV,

        /* CUSTO / PERCCUSTOFIXO */
        NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0) CUSTO,
        (SELECT MAX(NUMDEC) FROM TSIPAR WHERE CHAVE = 'PERCCUSFIXO' AND CODUSU = 0) PERCCUSTOFIXO,

        /* =========================================================
           ALTERACAO: PERCCOM PARA CODVEND=32
           - Se existir comissao ativa na AD_BHZCMKT (COM/COMP), usa ela
           - Senao, segue regra original (BHZ_OBTEMPERCCOMMKT etc)
           ========================================================= */
        CASE
            /* 14/15: regra existente */
            WHEN CAB.CODVEND IN (14, 15) AND ITE.AD_TXCOMISSAOMKT IS NOT NULL THEN
                ITE.AD_TXCOMISSAOMKT / NULLIF(
                    (SELECT COUNT(*)
                       FROM TGFITE I2
                      WHERE I2.NUNOTA = ITE.NUNOTA
                        AND NVL(I2.AD_CODPRODORIG,0) = NVL(ITE.AD_CODPRODORIG,0)),
                    0
                )

            /* CODVEND=32: se promo/comissao ativa na AD_BHZCMKT, usa percentual da tabela */
            WHEN CAB.CODVEND = 32 AND NVL(COM.COMISSAO, COMP.COMISSAO) IS NOT NULL THEN
                NVL(COM.COMISSAO, COMP.COMISSAO)

            /* Demais: regra original */
            ELSE
                CASE
                    WHEN CAB.CODVEND IN (42,29,32) THEN BHZ_OBTEMPERCCOMMKT(CAB.NUNOTA,ITE.SEQUENCIA,CAB.CODVEND,0)
                    WHEN COMFAIXA.PERCCOM IS NOT NULL THEN COMFAIXA.PERCCOM
                    WHEN CAB.CODVEND != 34
                         AND ITE.VLRUNIT = NVL(COM.PRECOVENDA, COMP.PRECOVENDA)
                         AND NVL(NVL(COM.COMISSAO, COMP.COMISSAO), 0) > 0
                    THEN NVL(COM.COMISSAO, COMP.COMISSAO)
                    ELSE NVL(
                             CASE
                                 WHEN NVL(ITE.AD_TXCOMISSAOMKT, 0) = 0 THEN CAB.AD_TXCOMISSAOMKT
                                 ELSE NVL(ITE.AD_TXCOMISSAOMKT, 0)
                             END,
                             (CASE
                                  WHEN CAB.AD_COMISSAOMKT > 0 THEN CAB.AD_COMISSAOMKT
                                  ELSE
                                      (CASE
                                           WHEN CAB.CODVEND IN (5,34) THEN
                                               (CAB.VLRNOTA - NVL(CASE WHEN CAB.CIF_FOB = 'T' THEN 0 ELSE CAB.VLRFRETE END,0))
                                               * (NVL(CAB.AD_TXCOMISSAOMKT,VEN.COMGER)/100)
                                           ELSE CAB.VLRNOTA * (NVL(CAB.AD_TXCOMISSAOMKT,VEN.COMGER)/100)
                                       END)
                              END / NULLIF(CAB.VLRNOTA,0)
                             ) * 100
                         )
                END
        END PERCCOM,

        /* Flag/percentual da tabela para uso no GROUP BY final (CODVEND=32) */
        CASE
            WHEN CAB.CODVEND = 32 THEN NVL(COM.COMISSAO, COMP.COMISSAO)
            ELSE NULL
        END AS PERCCOM_TABELA_32,

        ITE.AD_TXCOMISSAOMKT,
        CAB.AD_TXCOMISSAOMKT COMIS,
        CAB.VLRNOTA,

        /* PIS/COFINS */
        ((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.0165 VLRPIS,
        ((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.076 VLRCOFINS,
        (((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.0165)
        + (((ITE.VLRTOT - ITE.VLRDESC) - NVL(BHZ_OBTEMCUSTO(ITE.CODPROD,'S',CAB.CODEMP,'N',0,'N',' ',CAB.DTNEG,4) * ITE.QTDNEG,0)) * 0.076) VLRPISCOFINS,

        PRO.AD_PRECOMINIMO PRECOMINIMO,
        NVL((SELECT SUM(VLRCUPOM) FROM AD_BHZCVRI WHERE NUNOTA = ITE.NUNOTA AND SEQUENCIA = ITE.SEQUENCIA),0) REBATEBELA,
        PRO.FABRICANTE,

        NVL(ITE.AD_CUSTOFIXOMELI,0) CUSTOFIXOMELI

    FROM SANKHYA.TGFCAB NCAB
    INNER JOIN SANKHYA.TGFITE NITE ON NITE.NUNOTA = NCAB.NUNOTA AND NITE.VLRTOT != 0
    INNER JOIN SANKHYA.TGFPRO PRO ON PRO.CODPROD = NITE.CODPROD
    LEFT JOIN (SELECT NUNOTA, NUNOTAORIG, SEQUENCIA, SEQUENCIAORIG FROM SANKHYA.TGFVAR GROUP BY NUNOTA, NUNOTAORIG, SEQUENCIA, SEQUENCIAORIG) VAR ON VAR.NUNOTA = NCAB.NUNOTA AND VAR.SEQUENCIA = NITE.SEQUENCIA
    LEFT JOIN SANKHYA.TGFCAB CAB ON CAB.NUNOTA = VAR.NUNOTAORIG
    LEFT JOIN SANKHYA.TGFITE ITE ON ITE.NUNOTA = VAR.NUNOTAORIG AND ITE.SEQUENCIA = VAR.SEQUENCIAORIG
    INNER JOIN SANKHYA.TGFPAR PAR ON PAR.CODPARC = CAB.CODPARC
    INNER JOIN SANKHYA.TSICID CID ON CID.CODCID = PAR.CODCID
    INNER JOIN SANKHYA.TSIUFS UFS ON UFS.CODUF = CID.UF
    INNER JOIN SANKHYA.TGFVEN VEN ON VEN.CODVEND = CAB.CODVEND
    INNER JOIN SANKHYA.TGFGRU GRU ON GRU.CODGRUPOPROD = PRO.CODGRUPOPROD
    LEFT JOIN SANKHYA.TGFPAR TRANSP ON TRANSP.CODPARC = CAB.CODPARCTRANSP
    LEFT JOIN (
        SELECT C2.NUNOTA, SUM(ROUND(C.VLRNOTA / NULLIF((SELECT SUM(VLRNOTA) FROM TGFNCT WHERE NUNOTA = NCT.NUNOTA),0) * C2.VLRNOTA,4)) VLRCTE
        FROM TGFNCT NCT
        INNER JOIN TGFCAB C2 ON NCT.CHAVENFE = C2.CHAVENFE
        INNER JOIN TGFCAB C ON C.NUNOTA = NCT.NUNOTA
        GROUP BY C2.NUNOTA
    ) NCT ON NCT.NUNOTA = CAB.NUNOTA
    LEFT JOIN (
        SELECT NUNOTA, MAX(AD_CUSTO) VLRCOTACAO
        FROM TGFCONT
        WHERE AD_MELHORENVIO = 'S'
        GROUP BY NUNOTA
    ) CONT ON CONT.NUNOTA = CAB.NUNOTA
    INNER JOIN SANKHYA.TGFTOP TP ON TP.CODTIPOPER = NCAB.CODTIPOPER AND TP.DHALTER = NCAB.DHTIPOPER

    /* AD_BHZCMKT: comissao por produto/vendedor com vigencia */
    LEFT JOIN AD_BHZCMKT COM ON COM.CODPROD = ITE.CODPROD
                             AND COM.CODVEND = CAB.CODVEND
                             AND NVL(COM.PADRAO, 'N') = 'N'
                             AND CAB.DTNEG BETWEEN COM.DTINI AND COM.DTFIN
                             AND ROUND(ITE.VLRUNIT,2) = ROUND(COM.PRECOVENDA,2)
    LEFT JOIN AD_BHZCMKT COMP ON COMP.CODPROD = ITE.CODPROD
                             AND COMP.CODVEND = CAB.CODVEND
                             AND COMP.PADRAO = 'S'

    LEFT JOIN AD_BHZTAB TAB ON TAB.CODPARC = (
                                        CASE
                                            WHEN CAB.CODVEND = 34 THEN 220344
                                            WHEN CAB.CODVEND = 32 THEN 285038
                                            ELSE CAB.CODPARCTRANSP
                                        END)
                           AND ITE.VLRUNIT BETWEEN TAB.VLRMIN AND TAB.VLRMAX
                           AND TAB.VLRMIN IS NOT NULL
    LEFT JOIN AD_BHZPRE PRE ON PRE.NUTAB = TAB.NUTAB
                           AND PRE.CODPARC = (
                                        CASE
                                            WHEN CAB.CODVEND = 34 THEN 220344
                                            WHEN CAB.CODVEND = 32 THEN 285038
                                            ELSE CAB.CODPARCTRANSP
                                        END)
                           AND CASE
                                   WHEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000) > PRO.PESOBRUTO
                                   THEN CEIL((PRO.ALTURA * PRO.LARGURA * PRO.ESPESSURA) / 6000)
                                   ELSE PRO.PESOBRUTO
                               END BETWEEN PRE.PESOMIN AND PRE.PESOMAX

    LEFT JOIN (
        SELECT
            ITE.NUNOTA,
            ITE.SEQUENCIA,
            ROUND(
                (SUM(
                    CASE
                        WHEN ITE.VLRUNIT >= COMFAIXA.VLRFINAL THEN COMFAIXA.VLRFINAL - VLRINI
                        WHEN ITE.VLRUNIT < COMFAIXA.VLRFINAL THEN VLRUNIT - COMFAIXA.VLRINI + 0.01
                    END * (COMFAIXA.PERCCOM/100)
                ) * 100) / NULLIF(ITE.VLRTOT,0),
                2
            ) PERCCOM
        FROM TGFITE ITE
        INNER JOIN TGFCAB CAB ON CAB.NUNOTA = ITE.NUNOTA
        LEFT JOIN AD_BHZCOMFAIXA COMFAIXA ON COMFAIXA.CODPROD = ITE.CODPROD
                                         AND COMFAIXA.CODVEND = CAB.CODVEND
                                         AND COMFAIXA.DTREF = (
                                                SELECT MAX(DTREF)
                                                FROM AD_BHZCOMFAIXA
                                                WHERE CODPROD = ITE.CODPROD
                                                  AND CODVEND = CAB.CODVEND
                                                  AND DTREF <= CAB.DTNEG
                                            )
        WHERE CAB.TIPMOV IN ('P','V')
        GROUP BY ITE.NUNOTA, ITE.SEQUENCIA, ITE.VLRTOT
    ) COMFAIXA ON COMFAIXA.SEQUENCIA = ITE.SEQUENCIA
             AND COMFAIXA.NUNOTA = CAB.NUNOTA

    WHERE TRUNC(CAB.DTNEG) BETWEEN :PERIODO.INI AND :PERIODO.FIN
      AND NVL(CAB.VLRNOTA,0) != 0
      AND (:CODPROD IS NULL OR EXISTS (
                SELECT 1
                FROM TGFITE
                WHERE CODPROD = :CODPROD AND NUNOTA = CAB.NUNOTA
          ))
      AND PRO.AD_TIME IN :TIME
      AND (PRO.BH_MKTMODELO = :MODELO OR :MODELO IS NULL)
      AND (CAB.CODTIPOPER IN :CODTIPOPER)
      AND (CAB.NUMNOTA = :NUMNOTA OR :NUMNOTA IS NULL)
      AND (CAB.CODPARC = :CODPARC OR :CODPARC IS NULL)
      AND (CAB.CODEMP = :CODEMP OR :CODEMP IS NULL)
      AND CAB.CODVEND IN :CODVEND
      AND (CAB.NUNOTA = :NUNOTA OR :NUNOTA IS NULL)
      AND (PRO.AD_LINHA = :LINHA OR :LINHA IS NULL)
      AND PRO.FABRICANTE IN :FABRICANTE
      AND CASE
              WHEN CAB.CODEMP IN (5,6) AND CAB.CODVEND = 29 THEN 'MA'
              WHEN CAB.CODEMP IN (5,6) AND CAB.CODVEND IN (5,34) THEN 'M'
              WHEN CAB.CODEMP IN (5,6) AND CAB.CODVEND IN (32) THEN 'A'
              WHEN CAB.CODPARCTRANSP IN (220344,224984) THEN 'M'
              WHEN CAB.CODPARCTRANSP IN (285038) THEN 'A'
              ELSE 'P'
          END IN :TIPOENV
      AND (TP.TIPMOV = 'V' AND ((TP.ATUALFIN IN (1, -1) AND TP.TIPATUALFIN = 'I' AND NCAB.STATUSNFE = 'A')
           OR NCAB.CODTIPOPER IN (3100, 3103, 3105, 3113, 3119, 3225)))
      AND CAB.CODEMP = 5

),
T1 AS (
    SELECT
        VD.*,

        /* =========================================================
           ALTERACAO: COMISSAO (VALOR) PARA CODVEND=32
           - Se existir PERCCOM_TABELA_32, calcula valor = VLRTOT * %/100
           - Senao, segue regra anterior (BHZ_OBTEMPERCCOMMKT)
           - Mantem + CUSTOFIXOMELI como no seu padrao atual
           ========================================================= */
        CASE
            WHEN VD.CODVEND IN (14, 15) AND VD.AD_TXCOMISSAOMKT IS NOT NULL THEN
                (VD.VLRNOTA) * (NVL(VD.PERCCOM,0) / 100)

            WHEN VD.CODVEND = 32 AND VD.PERCCOM_TABELA_32 IS NOT NULL THEN
                (VD.VLRNOTA) * (VD.PERCCOM_TABELA_32 / 100) + VD.CUSTOFIXOMELI

            ELSE
                (CASE
                     WHEN VD.CODVEND IN (42,29,32) THEN BHZ_OBTEMPERCCOMMKT(VD.NUNOTA,VD.SEQUENCIA,VD.CODVEND,1)
                     ELSE (VD.VLRTOT) * (NVL(VD.PERCCOM,0) / 100)
                 END + CUSTOFIXOMELI)
        END COMISSAO,

        CASE
            WHEN VD.UF in ('SP', 'MG', 'RJ', 'ES') THEN 'SUDESTE'
            WHEN VD.UF in ('RS', 'SC', 'PR') THEN 'SUL'
            WHEN VD.UF in ('DF','GO','MT','MS') THEN 'CENTRO OESTE'
            WHEN VD.UF in ('AL', 'BA', 'CE', 'MA','PB', 'PE','PI', 'SE', 'RN') THEN 'NORDESTE'
            WHEN VD.UF in ('AC', 'AP', 'AM', 'PA', 'RO', 'RR', 'TO') THEN 'NORTE'
        END AS REGIOES,

        (VD.VLRTOT) * 0.0113 VLRESPECIAL,
        VD.VLRFRETE - VD.CUSTOFRETE RESULTFRETE,

        CASE
            WHEN (VD.VLRTOT) = 0 THEN 0
            ELSE ROUND((((VD.VLRTOT) / NULLIF(VD.CUSTO,0)) - 1) * 100,2)
        END PERCMARKUP,

        ROUND(((VD.VLRTOT) - VD.CUSTO),2) VLRMARKUP,

        NVL(DIN.VLRFCP,0) VLRFCP,
        NVL(CASE
            WHEN VD.UF = 'ES' THEN DIN.VLRICMS
            ELSE DIN.VLRDIFALDEST
        END,0) VLRDIFALDEST,

        NVL(CASE
            WHEN VD.UF = 'ES' THEN 17
            ELSE ROUND((DIN.VLRDIFALDEST / NULLIF((VD.VLRTOT - VD.VLRFRETE),0)) * 100,2)
        END,0) PERCDIFAL,

        NVL(DIN.VLRFCP,0) + NVL(CASE
                         WHEN VD.UF = 'ES' THEN DIN.VLRICMS
                         ELSE DIN.VLRDIFALDEST
                     END,0) + NVL(VD.VLRIPI,0) + (VD.VLRTOT * 0.0113) + NVL(VD.VLRPIS,0) + NVL(VD.VLRCOFINS,0) IMPOSTOS,

        VD.VLRTOT * (VD.PERCCUSTOFIXO / 100) CUSTOFIXO,

        ((NVL(VD.VLRTOT,0) + NVL(VD.VLRFRETE,0) + (CASE WHEN CODVEND = 29 THEN 0 ELSE NVL(VD.REBATE,0) END))
        - NVL((CASE WHEN VD.UF = 'ES' THEN NVL(DIN.VLRICMS,0) ELSE NVL(DIN.VLRDIFALDEST,0) END
           + (VD.VLRTOT * 0.0113) + NVL(DIN.VLRFCP,0) + NVL(VD.VLRIPI,0) + NVL(VD.VLRPIS,0) + NVL(VD.VLRCOFINS,0) + NVL(VD.CUSTO,0)
           + NVL(CASE
                    WHEN VD.CODVEND = 32 AND VD.PERCCOM_TABELA_32 IS NOT NULL THEN (VD.VLRNOTA) * (VD.PERCCOM_TABELA_32 / 100) + VD.CUSTOFIXOMELI
                 WHEN VD.CODVEND IN (42,29) THEN BHZ_OBTEMPERCCOMMKT(VD.NUNOTA,VD.SEQUENCIA,VD.CODVEND,1)
                 ELSE (VD.VLRTOT) * (NVL(VD.PERCCOM,0) / 100)
             END,0)
           + NVL(VD.CUSTOFRETE,0) + VD.VLRTOT * (VD.PERCCUSTOFIXO / 100)),0))
        - NVL(CASE WHEN VD.CODPARCTRANSP = 220344 THEN VD.VLRFRETE ELSE 0 END,0)
        + NVL(REBATEBELA,0) VLRRESULTADO

    FROM VENDAS VD
    LEFT JOIN (
        SELECT NUNOTA, SEQUENCIA, SUM(NVL(VLRFCP,0)) VLRFCP, SUM(NVL(VLRDIFALDEST,0)) VLRDIFALDEST, SUM(NVL(VALOR,0)) VLRICMS
        FROM TGFDIN
        WHERE CODIMP = 1
        GROUP BY NUNOTA, SEQUENCIA
    ) DIN ON DIN.NUNOTA = VD.NUNOTA AND DIN.SEQUENCIA = VD.SEQUENCIA
),
REBATE_CALC AS (
    SELECT
        NUNOTA,
        SEQUENCIA,
        CODPARC,
        CODVEND,
        REBATE,
        CASE
            WHEN ROUND(BASE_COMISSAO * ((COMISSAO - TXMKT - COMISSAO_PADRAO) / NULLIF(COMISSAO_PADRAO,0)),2) < 0 THEN 0
            ELSE ROUND(BASE_COMISSAO * ((COMISSAO - TXMKT - COMISSAO_PADRAO) / NULLIF(COMISSAO_PADRAO,0)),2)
        END AS VLRREBATEMKT
    FROM T1
)
SELECT
    SUM(T1.CUSTO) AS CUSTO,
    SUM(T1.VLRIPI) AS VLRIPI,
    SUM(T1.VLRDIFALDEST) AS VLRICMSDIFALDEST,
    SUM(T1.VLRFCP) AS VLRICMSFCP,
    SUM(T1.VLRPISCOFINS) AS VLRPISCOFINS,
    SUM(T1.VLRTOT) AS VLRTOT,
    T1.CODPARC,
    T1.CODPARCTRANSP,
    T1.TRANSPORTADOR,
    T1.NRONOTA AS NUMNOTA,
    T1.CODEMP,
    T1.DTFATUR,
    T1.CODTIPOPER,
    T1.DESCROPER,
    T1.NUNOTA,
    T1.VLRNOTA,
    T1.CODVEND,
    T1.APELIDO AS VENDEDOR,
    T1.BH_CODEMKT,
    T1.CODCID,
    T1.NOMECID,
    T1.CODUF,
    AD_VLRREPASSE,
    T1.UF,
    T1.CEP,

    CASE
        WHEN T1.CODVEND = 29 THEN SUM(NVL(R.VLRREBATEMKT,0))
        WHEN T1.CODVEND = 42 THEN CASE WHEN  SUM(T1.REBATE) < 1 THEN 0 ELSE  (SUM(NVL(T1.REBATE,0))/COUNT(T1.REBATE)) END
        ELSE SUM(T1.REBATE)
    END REBATE,

    T1.TIPMOV,
    T1.DTNEG,
    SUM(NVL(T1.BH_DESCONTO,0)) BH_DESCONTO,
    T1.CHAVENFE,
    SUM(NVL(REBATEBELA,0)) REBATEBELA,
    PERCCUSTOFIXO,
    SUM(NVL(CUSTOFIXO,0)) VLRCUSTOFIXO,
    REGIOES,

    CASE
        WHEN T1.CODVEND = 29 THEN
            SUM(NVL(VLRRESULTADO,0)) + SUM(NVL(R.VLRREBATEMKT,0))
        ELSE SUM(NVL(VLRRESULTADO,0))
    END RESULTADO,

    CASE
        WHEN T1.CODVEND = 29 THEN
            ROUND(
                (
                    SUM(NVL(VLRRESULTADO,0)) + SUM(NVL(R.VLRREBATEMKT,0))
                ) / NULLIF(SUM(NVL(VLRTOT,0)),0) * 100,
                2
            )
        ELSE ROUND(SUM(NVL(VLRRESULTADO,0)) / NULLIF(SUM(VLRTOT),0) * 100,2)
    END PERCRESULTADO,

    QTDVOL,
    AD_DHINC,
    SUM(NVL(VLRESPECIAL,0)) VLRESPECIAL,
    ROUND(SUM(NVL(VLRMARKUP,0)) / NULLIF(SUM(CUSTO),0) * 100,2) MARKUP,
    SUM(NVL(RESULTFRETE,0)) RESULTFRETE,
    SUM(NVL(IMPOSTOS,0)) VLRIMPOSTOS,
    SUM(NVL(VLRFRETE,0)) FRETE,
    SUM(NVL(CUSTOFRETE,0)) CUSTOFRETE,
    SUM(NVL(CUSTOFRETEPREV,0)) CUSTOFRETEPREV,
    SUM(NVL(CUSTOFRETEPREV,0)) - SUM(CUSTOFRETE) DIFFRETEPREV,
    SUM(NVL(VLRMARKUP,0)) VLRMARKUP,
    AD_BM_DHINTEGRACAO,
    NVL(AD_VLRREPASSE,0) AD_VLRREPASSE,

    SUM(NVL(T1.COMISSAO,0)) VLRCOMMKT,

    /* =========================================================
       ALTERACAO: PERCCOM NO SELECT FINAL PARA CODVEND=32
       - Se existir comissao ativa (PERCCOM_TABELA_32), retorna percentual ponderado por VLRTOT
       - Senao, cai no calculo antigo (valor/total * 100)
       ========================================================= */
    CASE
        WHEN T1.CODVEND = 29 THEN
            ROUND(SUM(T1.COMISSAO) / NULLIF((T1.VLRNOTA + SUM(NVL(R.VLRREBATEMKT,0))),0) * 100,2)

        WHEN T1.CODVEND = 42 THEN
            ROUND(SUM(T1.COMISSAO) / NULLIF((T1.VLRNOTA + NVL(SUM(T1.REBATE),0)),0) * 100,2)

        WHEN T1.CODVEND = 32 AND SUM(CASE WHEN T1.PERCCOM_TABELA_32 IS NOT NULL THEN 1 ELSE 0 END) > 0 THEN
            ROUND(
                SUM(T1.VLRTOT * T1.PERCCOM_TABELA_32) / NULLIF(SUM(T1.VLRTOT),0),
                2
            )

        WHEN T1.CODVEND = 32 THEN
            ROUND(
                SUM(T1.COMISSAO)
                / NULLIF(
                    (SELECT SUM(T2.VLRTOT)
                       FROM T1 T2
                      WHERE T2.NUNOTA = T1.NUNOTA),
                    0
                ) * 100,
                2
            )

        ELSE
            ROUND(SUM(T1.COMISSAO) / NULLIF(T1.VLRNOTA,0) * 100,2)
    END PERCCOM,

    CASE
        WHEN (CASE
                  WHEN T1.CODVEND = 29 THEN
                      SUM(VLRRESULTADO) + SUM(NVL(R.VLRREBATEMKT,0))
                  ELSE SUM(VLRRESULTADO)
              END) < 0
             AND T1.CODVEND IN (34,42,29,32)
        THEN '#ffbfaa'
        ELSE ''
    END AS BKCOLOR,

    T1.FABRICANTE

FROM T1
JOIN REBATE_CALC R ON R.NUNOTA = T1.NUNOTA AND R.SEQUENCIA = T1.SEQUENCIA
GROUP BY
    T1.CODPARC,
    T1.CODPARCTRANSP,
    T1.TRANSPORTADOR,
    T1.NRONOTA,
    T1.CODEMP,
    T1.DTFATUR,
    T1.CODTIPOPER,
    T1.DESCROPER,
    T1.NUNOTA,
    T1.VLRNOTA,
    T1.CODVEND,
    T1.APELIDO,
    PERCCUSTOFIXO,
    T1.BH_CODEMKT,
    T1.CODCID,
    T1.NOMECID,
    T1.CODUF,
    AD_VLRREPASSE,
    T1.UF,
    T1.CEP,
    T1.TIPMOV,
    T1.DTNEG,
    T1.CHAVENFE,
    QTDVOL,
    AD_DHINC,
    AD_BM_DHINTEGRACAO,
    T1.CODVEND,
    T1.FABRICANTE,
    REGIOES
HAVING ((:RESULTNEG = 'S' AND SUM(VLRRESULTADO) < 0) OR NVL(:RESULTNEG,'N') = 'N')
