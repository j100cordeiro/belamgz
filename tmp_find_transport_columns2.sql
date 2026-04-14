connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 300 trimspool on
col table_name format a30
col column_name format a30
select table_name, column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and (column_name like '%CODTRANSP%'
       or column_name like '%TRANSPORTADORA%'
       or column_name like '%METODO_ENVIO%'
       or column_name like '%INTELIPOST%')
order by table_name, column_name;
exit;
