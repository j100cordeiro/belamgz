connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 long 200000 longchunksize 200000 trimspool on
col owner format a20
col name format a40
col type format a20
select distinct owner, name, type
from all_source
where owner='SANKHYA'
  and upper(text) like '%TGFCONT%'
  and (upper(text) like '%AD_CODPARC%' or upper(text) like '%AD_METODO%' or upper(text) like '%MELHORENVIO%')
order by owner, type, name;
exit;
