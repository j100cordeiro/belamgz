connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 240 trimspool on
col nomeparc format a45
col ad_codtranspint format a30
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where upper(ad_codtranspint) like '%JAMEF%'
   or upper(ad_codtranspint) like '%BRASIL WEB%'
   or upper(ad_codtranspint) like '%BRASPRESS%'
   or upper(ad_codtranspint) like '%TRILOG%'
   or upper(ad_codtranspint) like '%TOTAL%'
   or upper(ad_codtranspint) like '%GENEROSO%'
order by ad_codtranspint, codparc;
exit;
