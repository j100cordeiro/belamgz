connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 trimspool on
col what format a120
select job, what, broken
from user_jobs
where upper(what) like '%AD_STP_INC_EXTRA_NULL%';
exit;
