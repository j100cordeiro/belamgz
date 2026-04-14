connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 500 lines 220 trimspool on
select table_name, column_id, column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and table_name in ('TGFEFDFC120','TGFEFDFD100','TGFEFDFE110','TGFEFDFE116','TGFEFDFE310','TGFEFDFE316','TGFEFDC0000')
order by table_name, column_id;
exit;
