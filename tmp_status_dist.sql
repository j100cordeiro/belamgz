CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT status, COUNT(*) qtd,
       SUM(CASE WHEN nunota IS NOT NULL THEN 1 ELSE 0 END) com_nunota,
       SUM(CASE WHEN nunota IS NULL THEN 1 ELSE 0 END) sem_nunota
FROM sankhya.tgfixn
WHERE dhprocess >= TRUNC(SYSDATE)-7
GROUP BY status
ORDER BY status;
EXIT
