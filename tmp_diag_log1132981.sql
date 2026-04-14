CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nuarquivo, DBMS_LOB.SUBSTR(detalhesimportacao, 500, 1) AS detalhes
FROM sankhya.tgfixn
WHERE nuarquivo = 1132981;
EXIT
