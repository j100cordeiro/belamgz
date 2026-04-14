connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 220 trimspool on
col column_name format a30
col data_type format a15
select column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and table_name='TGFCONT'
  and (column_name like '%CODPARC%' or column_name like '%NOME%' or column_name like '%MELHOR%' or column_name like '%ENVIO%' or column_name like '%CUSTO%' or column_name like '%NUNOTA%' or column_name like '%PARC%')
order by column_name;
exit;
