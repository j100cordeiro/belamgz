connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 100 lines 220 trimspool on
select column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and table_name='TGFPAR'
  and column_name='AD_CODTRANSPINT';
exit;
