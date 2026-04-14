CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

SELECT nuarquivo, nunota, numnota, codtipoper, status, ad_nunotaorig,
       TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess,
       SUBSTR(detalhesimportacao,1,500) detalhes
FROM sankhya.tgfixn
WHERE nunota IN (3827676,3827675)
ORDER BY nuarquivo DESC;

EXIT
