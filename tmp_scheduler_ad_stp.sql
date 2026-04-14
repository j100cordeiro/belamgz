connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 trimspool on
col job_name format a40
col program_name format a40
col job_action format a120
select owner, job_name, program_name, job_action, enabled, state
from all_scheduler_jobs
where upper(nvl(job_action,' ')) like '%AD_STP_INC_EXTRA_NULL%'
   or upper(nvl(program_name,' ')) like '%AD_STP_INC_EXTRA_NULL%'
order by owner, job_name;
exit;
