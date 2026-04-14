CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nuarquivo, nunota, numnota, status,
       TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess,
       DBMS_LOB.SUBSTR(detalhesimportacao, 200, 1) detalhes
FROM sankhya.tgfixn
WHERE detalhesimportacao LIKE '%[VINCXML %'
  AND dhprocess >= TO_DATE('2026-03-16 13:50:00','YYYY-MM-DD HH24:MI:SS')
ORDER BY dhprocess DESC;
EXIT
