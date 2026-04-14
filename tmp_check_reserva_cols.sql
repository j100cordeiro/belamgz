SET PAGESIZE 200
SET LINESIZE 320

PROMPT === COLUNAS TGFTOP (EST/RES) ===
SELECT column_name
  FROM all_tab_columns
 WHERE owner='SANKHYA'
   AND table_name='TGFTOP'
   AND (column_name LIKE '%EST%' OR column_name LIKE '%RES%')
 ORDER BY column_name;

PROMPT === TOPS DE PEDIDO EMPRESA 5 (ULTIMOS 30 DIAS) ===
SELECT c.codtipoper,
       c.dhtipoper,
       COUNT(*) qtd
  FROM sankhya.tgfcab c
 WHERE c.codemp = 5
   AND c.tipmov = 'P'
   AND c.dtneg >= TRUNC(SYSDATE)-30
 GROUP BY c.codtipoper, c.dhtipoper
 ORDER BY qtd DESC;

EXIT
