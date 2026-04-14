CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET COLSEP ';'
SELECT c.nunota, c.numnota, c.tipmov, c.codemp, c.codtipoper, c.codparc, c.vlrnota, c.statusnfe, c.pendente
FROM sankhya.tgfcab c
WHERE c.nunota IN (3827125, 3827126, 3827675, 3827676)
ORDER BY c.nunota;
EXIT
