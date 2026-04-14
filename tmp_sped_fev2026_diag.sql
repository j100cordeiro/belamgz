connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 500 lines 260 trimspool on
prompt === C0000 ===
select codemp, dtref, cnpj, nome, uf, cod_mun
from sankhya.tgfefdc0000
where trunc(dtref) between date '2026-02-01' and date '2026-02-28'
order by codemp, dtref;

prompt === C120 DUPLICIDADE ===
select codemp, dtref, cod_doc_imp, num_doc_imp, nvl(trim(num_acdraw),'<null>') num_acdraw, count(*) qtd
from sankhya.tgfefdfc120
where trunc(dtref) between date '2026-02-01' and date '2026-02-28'
group by codemp, dtref, cod_doc_imp, num_doc_imp, nvl(trim(num_acdraw),'<null>')
having count(*) > 1
order by dtref, codemp, num_doc_imp;

prompt === C120 VALORES ===
select codemp, dtref, cod_doc_imp, num_doc_imp, nvl(trim(num_acdraw),'<null>') num_acdraw
from sankhya.tgfefdfc120
where trunc(dtref) between date '2026-02-01' and date '2026-02-28'
order by dtref, codemp, num_doc_imp;

prompt === D100 MUNICIPIO INVALIDO ===
select d.codemp, d.dtref, d.chave, d.cod_mod, d.num_doc, d.cod_mun_orig, d.cod_mun_dest,
       case when o.codmunfis is null then 'ORIG_INVALIDO' end as erro_orig,
       case when de.codmunfis is null then 'DEST_INVALIDO' end as erro_dest
from sankhya.tgfefdfd100 d
left join sankhya.tsicid o on o.codmunfis = d.cod_mun_orig
left join sankhya.tsicid de on de.codmunfis = d.cod_mun_dest
where trunc(d.dtref) between date '2026-02-01' and date '2026-02-28'
  and (d.cod_mun_orig is not null or d.cod_mun_dest is not null)
  and (o.codmunfis is null or de.codmunfis is null)
order by d.dtref, d.codemp, d.num_doc;

prompt === E110 X E116 ===
select e110.codemp, e110.dtref, e110.seqe100,
       nvl(e110.vl_icms_recolher,0) vl_icms_recolher,
       nvl(sum(e116.vl_or),0) soma_e116,
       nvl(e110.vl_icms_recolher,0) - nvl(sum(e116.vl_or),0) diferenca
from sankhya.tgfefdfe110 e110
left join sankhya.tgfefdfe116 e116
  on e116.codemp = e110.codemp
 and e116.dtref = e110.dtref
 and e116.seqe100 = e110.seqe100
where trunc(e110.dtref) between date '2026-02-01' and date '2026-02-28'
group by e110.codemp, e110.dtref, e110.seqe100, e110.vl_icms_recolher
having round(nvl(e110.vl_icms_recolher,0) - nvl(sum(e116.vl_or),0), 2) <> 0
order by e110.dtref, e110.codemp, e110.seqe100;

exit;
