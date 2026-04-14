CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nunota, numnota, tipmov, codtipoper, codparc, vlrnota, statusnfe, TO_CHAR(dtneg,'YYYY-MM-DD HH24:MI') dtneg
FROM sankhya.tgfcab
WHERE codemp=5
  AND codparc=1061535
  AND tipmov='V'
  AND codtipoper=3225
  AND dtneg >= TRUNC(SYSDATE)-2
ORDER BY nunota DESC
FETCH FIRST 20 ROWS ONLY;
EXIT
