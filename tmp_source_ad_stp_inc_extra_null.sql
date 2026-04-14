connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 400 lines 300 long 200000 longchunksize 200000 trimspool on
col text format a220
select line, text
from all_source
where owner='SANKHYA'
  and name='AD_STP_INC_EXTRA_NULL'
  and type='PROCEDURE'
order by line;
exit;
