CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nunota, sequencia, nunotaorig, sequenciaorig, statusnota
FROM sankhya.tgfvar
WHERE nunota = 3827676
ORDER BY sequencia, nunotaorig;
EXIT
