connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 500 lines 280 trimspool on feedback on verify off
prompt === NUNOTAS PRINT ORACLE ===
select rel.nunota,
       rel.codparctransp,
       rel.transportadora,
       rel.parcont,
       rel.numnota,
       rel.codparc,
       rel.parceiro,
       rel.expedido,
       rel.codtipoper
from sankhya.bhz_reltransp rel
where rel.nunota in (3837864,3839184,3837861,3837867,3839274,3837759,3839213,3837760,3838337,3837764)
order by rel.nunota;

prompt === PASSA NO FILTRO EXATO DO TRANSPORTE.SQL ===
select rel.nunota,
       rel.codparctransp,
       rel.transportadora,
       rel.parcont,
       rel.expedido,
       case when rel.codparctransp = 918566 then 'S'
            when rel.codparctransp in (
              select codparc
              from sankhya.tgfpar
              where ad_codtranspint in (
                    '285038','285038','519252','536527','386550','180173','241789',
                    '220344','220344','180168','180096','224984','224984','180152',
                    '536524','462086','548686','281091','288270','299851','180095',
                    '519553','481118','1058304','64749','1062294'
              )
            ) then 'S'
            else 'N'
       end as passa_filtro
from sankhya.bhz_reltransp rel
where rel.nunota in (3837864,3839184,3837861,3837867,3839274,3837759,3839213,3837760,3838337,3837764)
order by rel.nunota;

prompt === MAPEAMENTO DO CODPARCTRANSP ===
select codparc, nomeparc, ad_codtranspint
from sankhya.tgfpar
where codparc in (1058304,1062294,64749)
order by codparc;
exit
