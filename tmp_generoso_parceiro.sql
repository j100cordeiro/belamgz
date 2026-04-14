connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 220 trimspool on
col nomeparc format a60
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where upper(nomeparc) like '%GENEROSO%'
   or codparc = 0
order by nomeparc;
exit;
