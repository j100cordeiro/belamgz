CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET COLSEP ';'
SELECT
  i.nuarquivo,
  i.nunota,
  REGEXP_SUBSTR(DBMS_LOB.SUBSTR(i.xml, 4000, 1), '<xPed>([^<]+)</xPed>', 1, 1, NULL, 1) AS xped
FROM sankhya.tgfixn i
WHERE i.nuarquivo IN (1132981,1132982)
ORDER BY i.nuarquivo;
EXIT
