connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 320 trimspool on
col nome_transportadora format a22
col metodo_envio format a18
col ad_metodo format a25
select a.cod_transportadora,
       a.nome_transportadora,
       a.metodo_envio,
       a.codparc as codparc_ad_intelipost,
       c.ad_metodo,
       c.ad_codparc as codparc_tgfcont,
       c.ad_custo,
       c.ad_diasentrega,
       c.ad_melhorenvio
from sankhya.ad_intelipostcotacao a
left join sankhya.tgfcont c
  on c.nunota = a.nunota
 and (
      upper(c.ad_metodo) like '%' || upper(a.nome_transportadora) || '%'
      or upper(c.ad_metodo) like '%' || upper(a.nome_transportadora || ' ' || a.metodo_envio) || '%'
      or upper(c.ad_metodo) like '%' || upper(a.nome_transportadora || ' INTERIOR') || '%'
      or upper(c.ad_metodo) like '%' || upper(a.nome_transportadora || ' CAPITAL') || '%'
     )
where a.nunota = 3828407
order by a.valor;
exit;
