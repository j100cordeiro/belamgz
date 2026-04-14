CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 500
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT table_name, column_name
FROM all_tab_columns
WHERE owner='SANKHYA'
  AND column_name IN ('CLASSEJAVA','CLASSE','EVENTO','TIPOEVENTO','NOMETABELA','INSTANCIA','ATIVO','TIPO')
ORDER BY table_name, column_name;
EXIT
