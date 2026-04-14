-- Diagnostico SPED Bela Magazine - 4 erros base
-- Periodo alvo: fevereiro/2026
-- Empresa alvo: ajustar CODEMP conforme necessario
-- Observacao: no ambiente consultado, as tabelas TGFEFD* nao estao com dados persistidos para 2026-02.
-- Por isso o script traz 2 abordagens:
--   1) diagnostico na origem (fonte Sankhya)
--   2) diagnostico no SPED gerado (quando houver persistencia nas TGFEFD*)

--------------------------------------------------------------------------------
-- PARAMETROS
--------------------------------------------------------------------------------
-- Ajuste aqui quando precisar rodar outro periodo/empresa.

-- Fonte/origem
-- Empresa Bela principal costuma ser CODEMP = 1.
-- Para rodar em outra empresa, altere o filtro c.codemp.

--------------------------------------------------------------------------------
-- 1) C120 - DUPLICIDADE NA ORIGEM (TGFIDI)
-- Objetivo: localizar documentos de importacao repetidos na base fonte.
--------------------------------------------------------------------------------
select *
from (
    select
        c.codemp,
        c.nunota,
        c.numnota,
        c.dtmov,
        i.docimp,
        nvl(trim(i.numacdraw), '<null>') as num_acdraw,
        count(*) over (
            partition by c.codemp, nvl(to_char(i.docimp), '<null>'), nvl(trim(i.numacdraw), '<null>')
        ) as qtd_duplicada
    from sankhya.tgfidi i
    join sankhya.tgfcab c
      on c.nunota = i.nunota
    where c.codemp = 1
      and c.dtmov between date '2026-02-01' and date '2026-02-28'
) x
where x.qtd_duplicada > 1
order by x.docimp, x.num_acdraw, x.nunota;

--------------------------------------------------------------------------------
-- 2) C120 - REGISTRO GERADO NO SPED (TGFEFDFC120)
-- Objetivo: localizar o C120 exatamente como saiu no arquivo SPED.
-- Use este bloco se a geracao estiver persistida em TGFEFD*.
--------------------------------------------------------------------------------
select
    c120.codemp,
    c120.dtref,
    c120.regniv1,
    c120.chave,
    c120.sequencia,
    c120.cod_doc_imp,
    c120.num_doc_imp,
    nvl(trim(c120.num_acdraw), '<null>') as num_acdraw,
    c120.pis_imp,
    c120.cofins_imp,
    count(*) over (
        partition by c120.codemp, c120.dtref, c120.num_doc_imp, nvl(trim(c120.num_acdraw), '<null>')
    ) as qtd_duplicada
from sankhya.tgfefdfc120 c120
where c120.codemp = 1
  and trunc(c120.dtref) between date '2026-02-01' and date '2026-02-28'
order by c120.dtref, c120.num_doc_imp, c120.num_acdraw, c120.sequencia;

--------------------------------------------------------------------------------
-- 3) C120 - FOCO NO DOCUMENTO DO ERRO
-- Alvo do relatorio mostrado: NUM_DOC_IMP = 26BR00001090233
--------------------------------------------------------------------------------
select
    c120.codemp,
    c120.dtref,
    c120.regniv1,
    c120.chave,
    c120.sequencia,
    c120.cod_doc_imp,
    c120.num_doc_imp,
    c120.num_acdraw,
    c120.pis_imp,
    c120.cofins_imp
from sankhya.tgfefdfc120 c120
where c120.codemp = 1
  and c120.num_doc_imp = '26BR00001090233'
order by c120.dtref, c120.sequencia;

--------------------------------------------------------------------------------
-- 4) MUNICIPIO INVALIDO NO D100 GERADO
-- Objetivo: achar COD_MUN_ORIG/COD_MUN_DEST que nao existem em TSICID.CODMUNFIS.
--------------------------------------------------------------------------------
select
    d.codemp,
    d.dtref,
    d.regniv1,
    d.chave,
    d.cod_mod,
    d.num_doc,
    d.cod_part,
    d.cod_mun_orig,
    cid_o.codcid as codcid_orig_encontrado,
    cid_o.nomecid as nomecid_orig,
    uf_o.uf as uf_orig,
    d.cod_mun_dest,
    cid_d.codcid as codcid_dest_encontrado,
    cid_d.nomecid as nomecid_dest,
    uf_d.uf as uf_dest,
    case when d.cod_mun_orig is not null and cid_o.codcid is null then 'ORIG_INVALIDO' end as erro_orig,
    case when d.cod_mun_dest is not null and cid_d.codcid is null then 'DEST_INVALIDO' end as erro_dest
from sankhya.tgfefdfd100 d
left join sankhya.tsicid cid_o
       on cid_o.codmunfis = d.cod_mun_orig
left join sankhya.tsiufs uf_o
       on uf_o.coduf = cid_o.uf
left join sankhya.tsicid cid_d
       on cid_d.codmunfis = d.cod_mun_dest
left join sankhya.tsiufs uf_d
       on uf_d.coduf = cid_d.uf
where d.codemp = 1
  and trunc(d.dtref) between date '2026-02-01' and date '2026-02-28'
  and (
        (d.cod_mun_orig is not null and cid_o.codcid is null)
     or (d.cod_mun_dest is not null and cid_d.codcid is null)
  )
order by d.dtref, d.num_doc;

--------------------------------------------------------------------------------
-- 5) E110 X E116
-- Objetivo: conferir qual apuracao E110 nao bate com a soma do E116.
--------------------------------------------------------------------------------
select
    e110.codemp,
    e110.dtref,
    e110.seqe100,
    nvl(e110.vl_icms_recolher, 0) as vl_icms_recolher,
    nvl(sum(e116.vl_or), 0) as soma_e116,
    round(nvl(e110.vl_icms_recolher, 0) - nvl(sum(e116.vl_or), 0), 2) as diferenca
from sankhya.tgfefdfe110 e110
left join sankhya.tgfefdfe116 e116
       on e116.codemp = e110.codemp
      and e116.dtref = e110.dtref
      and e116.seqe100 = e110.seqe100
where e110.codemp = 1
  and trunc(e110.dtref) between date '2026-02-01' and date '2026-02-28'
group by e110.codemp, e110.dtref, e110.seqe100, e110.vl_icms_recolher
having round(nvl(e110.vl_icms_recolher, 0) - nvl(sum(e116.vl_or), 0), 2) <> 0
order by e110.dtref, e110.seqe100;

--------------------------------------------------------------------------------
-- 6) DETALHE DO E116
-- Objetivo: detalhar os registros filhos que compoem a soma do E116.
--------------------------------------------------------------------------------
select
    e116.codemp,
    e116.dtref,
    e116.seqe100,
    e116.sequencia,
    e116.cod_or,
    e116.vl_or,
    e116.dt_vcto,
    e116.cod_rec,
    e116.num_proc,
    e116.ind_proc,
    e116.txt_compl,
    e116.mes_ref
from sankhya.tgfefdfe116 e116
where e116.codemp = 1
  and trunc(e116.dtref) between date '2026-02-01' and date '2026-02-28'
order by e116.dtref, e116.seqe100, e116.sequencia;

--------------------------------------------------------------------------------
-- 7) APOIO - PERIODOS REALMENTE PERSISTIDOS NAS TGFEFD*
-- Use este bloco quando os selects acima nao retornarem linhas.
--------------------------------------------------------------------------------
select 'TGFEFDFC120' as tabela, to_char(dtref,'YYYY-MM-DD') as dtref, count(*) as qtd
from sankhya.tgfefdfc120
group by to_char(dtref,'YYYY-MM-DD')
union all
select 'TGFEFDFD100' as tabela, to_char(dtref,'YYYY-MM-DD') as dtref, count(*) as qtd
from sankhya.tgfefdfd100
group by to_char(dtref,'YYYY-MM-DD')
union all
select 'TGFEFDFE110' as tabela, to_char(dtref,'YYYY-MM-DD') as dtref, count(*) as qtd
from sankhya.tgfefdfe110
group by to_char(dtref,'YYYY-MM-DD')
union all
select 'TGFEFDFE116' as tabela, to_char(dtref,'YYYY-MM-DD') as dtref, count(*) as qtd
from sankhya.tgfefdfe116
group by to_char(dtref,'YYYY-MM-DD')
order by 1, 2;
