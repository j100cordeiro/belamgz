CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 100
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nunota, numnota, tipmov, codemp, codtipoper, codparc, vlrnota, TO_CHAR(dtneg,'YYYY-MM-DD HH24:MI') dtneg
FROM sankhya.tgfcab
WHERE codemp=5
  AND numnota IN (25583,25572,25574)
ORDER BY numnota, nunota;
EXIT
