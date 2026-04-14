CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT c.nunota, c.ad_pedidomktplace, c.ad_codmkt, c.bh_codemkt
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
EXIT
