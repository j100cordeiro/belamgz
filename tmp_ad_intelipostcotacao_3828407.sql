connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 300 trimspool on
col metodo_envio format a30
col nome_transportadora format a25
select id,
       idcotacao,
       nunota,
       cod_transportadora,
       nome_transportadora,
       metodo_envio,
       valor,
       dias_entrega,
       codparc,
       pedidomktplace,
       to_char(datacotacao,'DD/MM/YYYY HH24:MI:SS') datacotacao
from sankhya.ad_intelipostcotacao
where nunota = 3828407
   or idcotacao = '391640555280664'
order by valor;
exit;
