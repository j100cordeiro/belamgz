connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 100 lines 220 trimspool on
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where codparc = 1058304;
exit;
