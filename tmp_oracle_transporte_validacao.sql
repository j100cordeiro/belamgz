connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 500 lines 260 long 50000 trimspool on feedback on verify off serveroutput off
prompt === MAPEAMENTO TGFPAR ===
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where codparc in (64749,1058304,1062294,536176,79264,299790,918566)
   or ad_codtranspint in ('64749','1058304','1062294')
order by codparc;

prompt === BHZ_RELTRANSP DIRETO POR CODPARCTRANSP ===
select rel.codparctransp, rel.parcont, count(*) qtd
from sankhya.bhz_reltransp rel
where rel.expedido >= trunc(sysdate) - 100
  and rel.codparctransp in (64749,1058304,1062294)
group by rel.codparctransp, rel.parcont
order by rel.codparctransp, rel.parcont;

prompt === CODPARC ELEGIVEIS PELO FILTRO ATUAL ===
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where ad_codtranspint in (
'285038','285038','519252','536527','386550','180173','241789',
'220344','220344','180168','180096','224984','224984','180152',
'536524','462086','548686','281091','288270','299851','180095',
'519553','481118','1058304','64749','1062294'
)
order by ad_codtranspint, codparc;

prompt === LINHAS QUE PASSAM NO FILTRO EXATO E BATEM COM OS CODIGOS NOVOS ===
select rel.codparctransp,
       rel.transportadora,
       rel.parcont,
       count(*) qtd
from sankhya.bhz_reltransp rel
where rel.expedido >= trunc(sysdate) - 100
  and rel.codparctransp not in (401961,271827,18789,422862,16,0)
  and (
        rel.codparctransp = 918566
        or rel.codparctransp in (
              select codparc
              from sankhya.tgfpar
              where ad_codtranspint in (
                    '285038','285038','519252','536527','386550','180173','241789',
                    '220344','220344','180168','180096','224984','224984','180152',
                    '536524','462086','548686','281091','288270','299851','180095',
                    '519553','481118','1058304','64749','1062294'
              )
        )
      )
  and rel.codparctransp in (64749,1058304,1062294)
group by rel.codparctransp, rel.transportadora, rel.parcont
order by rel.codparctransp, rel.parcont;

prompt === O QUE ENTRARIA SE O FILTRO FOSSE DIRETO POR CODPARCTRANSP ===
select rel.codparctransp,
       rel.transportadora,
       rel.parcont,
       count(*) qtd
from sankhya.bhz_reltransp rel
where rel.expedido >= trunc(sysdate) - 100
  and rel.codparctransp not in (401961,271827,18789,422862,16,0)
  and rel.codparctransp in (64749,1058304,1062294)
group by rel.codparctransp, rel.transportadora, rel.parcont
order by rel.codparctransp, rel.parcont;
exit
