connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 100 lines 220 trimspool on
col idcotacao format a30
select distinct idcotacao, nunota, to_char(datacotacao,'DD/MM/YYYY HH24:MI:SS') datacotacao
from sankhya.ad_intelipostcotacao
where nunota = 3829564;
exit;
