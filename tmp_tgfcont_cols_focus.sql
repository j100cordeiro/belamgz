connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 trimspool on
col owner format a20
col table_name format a20
col column_name format a30
select owner, table_name, column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and table_name in ('TGFCONT')
  and column_name in ('AD_CODPARC','AD_METODO','AD_CUSTO','AD_DIASENTREGA','AD_MELHORENVIO');
exit;
