CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

SELECT nuarquivo, nunota, status, ad_nunotaorig,
       DBMS_LOB.SUBSTR(detalhesimportacao, 1000, 1) AS detalhes
FROM sankhya.tgfixn
WHERE nunota IN (3827675,3827676)
ORDER BY nuarquivo;

SELECT nunota, sequencia, nunotaorig, sequenciaorig, statusnota
FROM sankhya.tgfvar
WHERE nunota IN (3827675,3827676)
ORDER BY nunota, sequencia, nunotaorig;

SELECT nunota, numnota, tipmov, pendente, ad_nrontoaorigem, ad_pedidomktplace, ad_codmkt, bh_codemkt
FROM sankhya.tgfcab
WHERE nunota IN (3827125,3827126,3827675,3827676)
ORDER BY nunota;

EXIT
