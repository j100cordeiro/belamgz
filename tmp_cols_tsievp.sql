CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 500
SET LINESIZE 32767
SET FEEDBACK OFF
SET COLSEP ';'
SELECT column_id, column_name, data_type
FROM all_tab_columns
WHERE owner='SANKHYA'
  AND table_name='TSIEVP'
ORDER BY column_id;
EXIT
