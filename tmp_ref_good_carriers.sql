connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 240 trimspool on
col nomeparc format a50
select ad_codtranspint, count(*) qtd
from sankhya.tgfpar
where ad_codtranspint in (79264,536527,386550,1058304,511941)
group by ad_codtranspint
order by ad_codtranspint;
exit;
