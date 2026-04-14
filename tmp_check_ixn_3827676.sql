CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nuarquivo, nunota, status, ad_nunotaorig, TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess
FROM sankhya.tgfixn
WHERE nunota = 3827676
ORDER BY nuarquivo;
EXIT
