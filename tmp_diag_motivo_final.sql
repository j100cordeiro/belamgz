CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

PROMPT ELEGIBILIDADE_3827872
SELECT i.nuarquivo, i.nunota, i.status,
       REGEXP_SUBSTR(DBMS_LOB.SUBSTR(i.xml, 4000, 1), '<xPed>([^<]+)</xPed>', 1, 1, NULL, 1) xped,
       i.ad_nunotaorig,
       TO_CHAR(i.dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess
FROM sankhya.tgfixn i
WHERE i.nuarquivo = 1133410;

SELECT c.nunota, c.tipmov, c.codemp, c.codparc, c.vlrnota,
       c.ad_pedidomktplace, c.ad_codmkt, c.bh_codemkt
FROM sankhya.tgfcab c
WHERE c.nunota = 3827564;

SELECT c.nunota
FROM sankhya.tgfcab c
WHERE c.tipmov = 'P'
  AND c.codemp = 5
  AND c.codparc = 1061535
  AND (
      REGEXP_REPLACE(TO_CHAR(c.ad_pedidomktplace), '[^0-9]', '') = '200001557090954'
   OR (LENGTH(REGEXP_REPLACE(TO_CHAR(c.ad_pedidomktplace), '[^0-9]', '')) = LENGTH('200001557090954') + 1
       AND SUBSTR(REGEXP_REPLACE(TO_CHAR(c.ad_pedidomktplace), '[^0-9]', ''), 1, LENGTH(REGEXP_REPLACE(TO_CHAR(c.ad_pedidomktplace), '[^0-9]', '')) - 1) = '200001557090954')
   OR REGEXP_REPLACE(TO_CHAR(c.ad_codmkt), '[^0-9]', '') = '200001557090954'
   OR (LENGTH(REGEXP_REPLACE(TO_CHAR(c.ad_codmkt), '[^0-9]', '')) = LENGTH('200001557090954') + 1
       AND SUBSTR(REGEXP_REPLACE(TO_CHAR(c.ad_codmkt), '[^0-9]', ''), 1, LENGTH(REGEXP_REPLACE(TO_CHAR(c.ad_codmkt), '[^0-9]', '')) - 1) = '200001557090954')
   OR REGEXP_REPLACE(TO_CHAR(c.bh_codemkt), '[^0-9]', '') = '200001557090954'
   OR (LENGTH(REGEXP_REPLACE(TO_CHAR(c.bh_codemkt), '[^0-9]', '')) = LENGTH('200001557090954') + 1
       AND SUBSTR(REGEXP_REPLACE(TO_CHAR(c.bh_codemkt), '[^0-9]', ''), 1, LENGTH(REGEXP_REPLACE(TO_CHAR(c.bh_codemkt), '[^0-9]', '')) - 1) = '200001557090954')
  )
ORDER BY c.nunota;

PROMPT LOGS_VINCXML
SELECT COUNT(*) qtd_logs
FROM sankhya.tgfixn
WHERE detalhesimportacao LIKE '%[VINCXML %';

SELECT MAX(dhprocess) AS ultimo_dhprocess_com_log
FROM sankhya.tgfixn
WHERE detalhesimportacao LIKE '%[VINCXML %';

SELECT COUNT(*) qtd_logs_novos
FROM sankhya.tgfixn
WHERE detalhesimportacao LIKE '%criterio=%';

SELECT COUNT(*) qtd_logs_pos_14h
FROM sankhya.tgfixn
WHERE detalhesimportacao LIKE '%[VINCXML %'
  AND dhprocess >= TO_DATE('2026-03-16 14:00:00','YYYY-MM-DD HH24:MI:SS');

EXIT
