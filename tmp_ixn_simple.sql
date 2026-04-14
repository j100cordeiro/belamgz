CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET COLSEP ';'
SELECT i.nuarquivo, i.nunota, i.numnota, i.codtipoper, i.status, i.ad_nunotaorig
FROM sankhya.tgfixn i
WHERE i.nunota IN (3827675, 3827676, 3827125, 3827126)
ORDER BY i.nuarquivo DESC;
EXIT
