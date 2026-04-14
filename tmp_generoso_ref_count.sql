connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 120 lines 220 trimspool on
col nomeparc format a50
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where ad_codtranspint in (79918,106709,111066)
order by codparc;
exit;
