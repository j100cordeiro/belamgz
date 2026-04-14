connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 240 trimspool on
col nomeparc format a45
col ad_metodo format a35
select nunota, ad_codparc, ad_metodo, ad_custo, ad_diasentrega, ad_melhorenvio
from sankhya.tgfcont
where nunota = 3828407
order by ad_metodo;
exit;
