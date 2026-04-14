CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nuarquivo, nunota, numnota, codtipoper, status, codparc, TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI:SS') dhprocess
FROM sankhya.tgfixn
WHERE numnota IN (25583,25572,25574)
ORDER BY nuarquivo DESC;
EXIT
