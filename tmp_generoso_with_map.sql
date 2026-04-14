connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 trimspool on
col nomeparc format a55
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where upper(nomeparc) like '%GENEROSO%'
  and ad_codtranspint is not null
order by nomeparc;
exit;
