connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 260 trimspool on
prompt === DTREF C120 ===
select to_char(dtref,'YYYY-MM-DD') dtref, count(*) qtd
from sankhya.tgfefdfc120
group by to_char(dtref,'YYYY-MM-DD')
order by 1 desc fetch first 20 rows only;

prompt === DTREF D100 ===
select to_char(dtref,'YYYY-MM-DD') dtref, count(*) qtd
from sankhya.tgfefdfd100
group by to_char(dtref,'YYYY-MM-DD')
order by 1 desc fetch first 20 rows only;

prompt === DTREF E110 ===
select to_char(dtref,'YYYY-MM-DD') dtref, count(*) qtd
from sankhya.tgfefdfe110
group by to_char(dtref,'YYYY-MM-DD')
order by 1 desc fetch first 20 rows only;

prompt === C0000 RECENTES ===
select codemp, to_char(dtref,'YYYY-MM-DD') dtref, cnpj, nome
from sankhya.tgfefdc0000
order by dtref desc fetch first 20 rows only;

exit;
