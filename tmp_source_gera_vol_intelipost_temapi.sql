connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 400 lines 300 trimspool on
col text format a220
select line, text
from all_source
where owner='SANKHYA'
  and name='GERA_VOL_INTELIPOST_TEMAPI'
  and type='FUNCTION'
order by line;
exit;
