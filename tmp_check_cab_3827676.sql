CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nunota, numnota, tipmov, statusnfe, codtipoper, codemp, vlrnota
FROM sankhya.tgfcab
WHERE nunota = 3827676;
EXIT
