CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nuarquivo, nunota, numnota, status, ad_nunotaorig
FROM sankhya.tgfixn
WHERE nunota IN (3827676,3827675)
ORDER BY nuarquivo DESC;
EXIT
