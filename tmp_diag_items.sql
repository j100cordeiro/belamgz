CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nunota, sequencia, codprod, qtdneg, vlrunit, vlrtot
FROM sankhya.tgfite
WHERE nunota IN (3827125,3827126,3827676)
ORDER BY nunota, sequencia;
EXIT
