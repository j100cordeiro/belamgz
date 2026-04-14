connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 220 trimspool on
select table_name, count(*) qtd
from (
  select 'TGFEFDCC120' table_name from sankhya.tgfefdcc120
  union all
  select 'TGFEFDFC120' from sankhya.tgfefdfc120
  union all
  select 'TGFEFDCD600' from sankhya.tgfefdcd600
  union all
  select 'TGFEFDFD100' from sankhya.tgfefdfd100
  union all
  select 'TGFEFDFE110' from sankhya.tgfefdfe110
  union all
  select 'TGFEFDFE116' from sankhya.tgfefdfe116
)
group by table_name
order by table_name;

prompt === DTREF TGFEFDCC120 ===
select to_char(dtref,'YYYY-MM-DD') dtref, count(*) qtd
from sankhya.tgfefdcc120
group by to_char(dtref,'YYYY-MM-DD')
order by 1 desc fetch first 20 rows only;
exit;
