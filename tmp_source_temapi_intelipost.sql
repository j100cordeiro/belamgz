connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 trimspool on
col owner format a20
col name format a40
col type format a20
select distinct owner, name, type
from all_source
where owner='SANKHYA'
  and (upper(text) like '%INTELIPOST%' or upper(text) like '%TEMAPI%' or upper(text) like '%DELIVERYMETHOD%' or upper(text) like '%LOGISTICPROVIDER%')
order by owner, type, name;
exit;
