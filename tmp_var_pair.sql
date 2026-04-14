CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET COLSEP ';'
SELECT nunota, nunotaorig, COUNT(*) qtd
FROM sankhya.tgfvar
WHERE nunota IN (3827675, 3827676)
GROUP BY nunota, nunotaorig
ORDER BY nunota, nunotaorig;
EXIT
