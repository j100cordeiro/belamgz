CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

SELECT nuarquivo, nunota, status,
       TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess,
       DBMS_LOB.SUBSTR(detalhesimportacao, 250, 1) AS detalhes
FROM sankhya.tgfixn
WHERE detalhesimportacao LIKE '%[VINCXML %'
  AND dhprocess >= TRUNC(SYSDATE)
ORDER BY dhprocess DESC
FETCH FIRST 20 ROWS ONLY;

EXIT
