set pages 200 lines 260 trimspool on
col owner format a20
col name format a40
col type format a20
select distinct owner, name, type
from all_source
where owner='SANKHYA'
  and upper(text) like '%AD_CODPARC%'
order by owner, type, name;
exit;
