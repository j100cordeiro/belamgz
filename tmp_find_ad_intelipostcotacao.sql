connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 280 trimspool on
col owner format a20
col table_name format a25
select owner, table_name
from all_tables
where table_name='AD_INTELIPOSTCOTACAO'
order by owner;
exit;
