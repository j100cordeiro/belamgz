CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

PROMPT CAB
SELECT nunota, numnota, tipmov, codemp, codtipoper, codparc, vlrnota, pendente, statusnfe,
       ad_nrontoaorigem, ad_nunotadev, ad_pedidomktplace, ad_codmkt, bh_codemkt
FROM sankhya.tgfcab
WHERE nunota IN (3827564,3827858)
ORDER BY nunota;

PROMPT VAR_REL
SELECT nunota, sequencia, nunotaorig, sequenciaorig, statusnota
FROM sankhya.tgfvar
WHERE nunota IN (3827858)
   OR nunotaorig IN (3827564)
ORDER BY nunota, sequencia, nunotaorig;

PROMPT IXN
SELECT nuarquivo, nunota, numnota, status, ad_nunotaorig,
       TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess,
       DBMS_LOB.SUBSTR(detalhesimportacao, 700, 1) detalhes
FROM sankhya.tgfixn
WHERE nunota IN (3827858)
ORDER BY nuarquivo DESC;

EXIT
