CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT owner, table_name, column_name
FROM all_tab_columns
WHERE owner='SANKHYA'
  AND (
    column_name LIKE '%PEDIDOMKT%'
    OR column_name LIKE '%CODEMKT%'
    OR column_name LIKE '%CODMKT%'
    OR column_name LIKE '%MKTPLACE%'
    OR column_name LIKE '%ORDERID%'
  )
ORDER BY table_name, column_name;
EXIT
