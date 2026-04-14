CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

SELECT nunota, numnota, tipmov, codemp, codparc, vlrnota, pendente, ad_nrontoaorigem, ad_nunotadev,
       ad_pedidomktplace, ad_codmkt, bh_codemkt
FROM sankhya.tgfcab
WHERE nunota IN (3827676,3827675,3827125,3827126)
ORDER BY nunota;

EXIT
