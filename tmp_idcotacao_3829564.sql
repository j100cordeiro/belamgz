connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 260 trimspool on
col idcotacao format a30
col metodo_envio format a25
col nome_transportadora format a25
select distinct idcotacao, nunota, pedidomktplace, to_char(datacotacao,'DD/MM/YYYY HH24:MI:SS') datacotacao
from sankhya.ad_intelipostcotacao
where nunota = 3829564
order by datacotacao desc;
exit;
