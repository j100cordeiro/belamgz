CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

SELECT c.nunota, TO_CHAR(c.dtneg,'YYYY-MM-DD') dtneg, c.vlrnota, c.codparc,
       c.ad_pedidomktplace, c.ad_codmkt, c.bh_codemkt
FROM sankhya.tgfcab c
WHERE c.tipmov='P'
  AND c.dtneg >= TRUNC(SYSDATE) - 15
  AND c.codemp = 5
  AND c.codparc = 1061486
  AND ABS(NVL(c.vlrnota,0) - 549.99) <= 0.01
  AND EXISTS (
      SELECT 1
      FROM sankhya.tgfite ped
      WHERE ped.nunota = c.nunota
        AND EXISTS (
            SELECT 1
            FROM sankhya.tgfite nf
            WHERE nf.nunota = 3827676
              AND nf.codprod = ped.codprod
        )
  )
ORDER BY c.dtneg DESC, c.nunota DESC;

EXIT
