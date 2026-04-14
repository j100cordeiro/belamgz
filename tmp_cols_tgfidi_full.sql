connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 260 trimspool on
select column_id, column_name, data_type
from all_tab_columns
where owner='SANKHYA' and table_name='TGFIDI'
order by column_id;
exit;
