CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

PROMPT IXN
SELECT nuarquivo, nunota, status, ad_nunotaorig,
       TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess,
       DBMS_LOB.SUBSTR(detalhesimportacao, 800, 1) detalhes
FROM sankhya.tgfixn
WHERE nunota IN (3827676)
ORDER BY nuarquivo;

PROMPT VAR
SELECT nunota, sequencia, nunotaorig, sequenciaorig, statusnota
FROM sankhya.tgfvar
WHERE nunota IN (3827676)
ORDER BY nunota, sequencia, nunotaorig;

PROMPT CAB_PEDIDOS
SELECT nunota, numnota, tipmov, pendente, ad_nrontoaorigem, ad_nunotadev,
       ad_pedidomktplace, ad_codmkt, bh_codemkt
FROM sankhya.tgfcab
WHERE nunota IN (3827125,3827126)
ORDER BY nunota;

PROMPT CAB_NF
SELECT nunota, numnota, tipmov, codemp, codtipoper, codparc, vlrnota, statusnfe
FROM sankhya.tgfcab
WHERE nunota IN (3827676)
ORDER BY nunota;

EXIT
