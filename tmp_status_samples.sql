CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 30
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT * FROM (
    SELECT status, nunota, numnota, codtipoper,
           DBMS_LOB.SUBSTR(detalhesimportacao, 160, 1) detalhes,
           TO_CHAR(dhprocess,'YYYY-MM-DD HH24:MI') dh
    FROM sankhya.tgfixn
    WHERE status IN (0,2,4,5)
    ORDER BY dhprocess DESC NULLS LAST
)
WHERE ROWNUM <= 20;
EXIT
