connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 trimspool on
col trigger_name format a40
col status format a10
col triggering_event format a20
select owner, trigger_name, status, triggering_event
from all_triggers
where owner='SANKHYA'
  and table_name='TGFCONT'
order by trigger_name;
exit;
