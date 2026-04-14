connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 120 lines 220 trimspool on
select count(*) qtd
from sankhya.tgfpar
where ad_codtranspint is not null;
exit;
