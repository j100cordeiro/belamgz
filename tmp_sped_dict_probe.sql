connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 220 trimspool on
select table_name, column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and (
    table_name like 'TGF%DI%' or
    table_name like 'TGF%E%' or
    table_name like 'TSI%CID%' or
    table_name like 'TSI%UFS%'
  )
  and (
    upper(column_name) like '%DOCIMP%' or
    upper(column_name) like '%ACDRAW%' or
    upper(column_name) like '%MUN%' or
    upper(column_name) like '%CID%' or
    upper(column_name) like '%UF%' or
    upper(column_name) like '%RECOL%' or
    upper(column_name) like '%OBRIG%' or
    upper(column_name) like '%CODREC%' or
    upper(column_name) like '%VL_OR%'
  )
order by table_name, column_name;
exit;
