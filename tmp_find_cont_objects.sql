connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 260 trimspool on
col owner format a20
col object_name format a40
col object_type format a20
select owner, object_name, object_type
from all_objects
where owner='SANKHYA'
  and object_type in ('PROCEDURE','FUNCTION','PACKAGE','PACKAGE BODY','TRIGGER','VIEW')
  and upper(object_name) like '%CONT%'
order by object_type, object_name;
exit;
