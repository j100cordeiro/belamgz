CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT column_name
FROM all_tab_columns
WHERE owner='SANKHYA'
  AND table_name='TGFIXN'
  AND (column_name LIKE '%PED%' OR column_name LIKE '%MKT%' OR column_name LIKE '%ORDER%' OR column_name LIKE '%ORIG%')
ORDER BY column_name;
EXIT
