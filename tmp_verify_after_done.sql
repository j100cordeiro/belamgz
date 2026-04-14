CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET LONG 5000
SET COLSEP ';'

PROMPT IXN
SELECT nuarquivo, nunota, numnota, status, ad_nunotaorig,
       TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess,
       DBMS_LOB.SUBSTR(detalhesimportacao, 1500, 1) detalhes
FROM sankhya.tgfixn
WHERE nunota = 3827872
ORDER BY nuarquivo DESC;

PROMPT VAR
SELECT nunota, sequencia, nunotaorig, sequenciaorig, statusnota
FROM sankhya.tgfvar
WHERE nunota = 3827872
ORDER BY sequencia, nunotaorig;

PROMPT CAB_PED
SELECT nunota, numnota, tipmov, pendente, ad_nrontoaorigem, ad_nunotadev,
       ad_pedidomktplace, ad_codmkt, bh_codemkt
FROM sankhya.tgfcab
WHERE nunota = 3827564;

PROMPT CAB_NF
SELECT nunota, numnota, tipmov, statusnfe, codtipoper, codemp, vlrnota
FROM sankhya.tgfcab
WHERE nunota = 3827872;

EXIT
