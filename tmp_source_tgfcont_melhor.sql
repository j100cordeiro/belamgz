connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 300 trimspool on
col owner format a20
col name format a40
col type format a20
select distinct owner, name, type
from all_source
where owner='SANKHYA'
  and upper(text) like '%TGFCONT%'
  and upper(text) like '%AD_MELHORENVIO%'
order by owner, type, name;
exit;
