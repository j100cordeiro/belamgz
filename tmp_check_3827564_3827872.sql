CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

PROMPT CAB_PED_NF
SELECT nunota, numnota, tipmov, codemp, codtipoper, codparc, vlrnota, pendente, statusnfe,
       ad_nrontoaorigem, ad_nunotadev, ad_pedidomktplace, ad_codmkt, bh_codemkt
FROM sankhya.tgfcab
WHERE nunota IN (3827564,3827872)
ORDER BY nunota;

PROMPT IXN_NF
SELECT nuarquivo, nunota, numnota, status, ad_nunotaorig,
       TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess,
       REGEXP_SUBSTR(DBMS_LOB.SUBSTR(xml, 4000, 1), '<xPed>([^<]+)</xPed>', 1, 1, NULL, 1) xped,
       DBMS_LOB.SUBSTR(detalhesimportacao, 1200, 1) detalhes
FROM sankhya.tgfixn
WHERE nunota = 3827872
ORDER BY nuarquivo DESC;

PROMPT VAR_NF
SELECT nunota, sequencia, nunotaorig, sequenciaorig, statusnota
FROM sankhya.tgfvar
WHERE nunota = 3827872
ORDER BY sequencia, nunotaorig;

EXIT
