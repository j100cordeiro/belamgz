connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 500 lines 280 trimspool on feedback on verify off
select rel.nunota,
       rel.sequencia,
       rel.codparctransp,
       rel.transportadora,
       rel.parcont,
       rel.codtipoper,
       rel.expedido,
       ite.codprod,
       par.codparc as codparc_par,
       hist.seq as hist_seq
from sankhya.bhz_reltransp rel
left join sankhya.ad_bhzhist hist
       on hist.nunota = rel.nunota
      and (rel.sequencia = hist.sequencia or hist.sequencia is null)
      and hist.seq = (
            select max(seq)
            from sankhya.ad_bhzhist
            where nunota = rel.nunota
              and (rel.sequencia = sequencia or sequencia is null)
       )
left join sankhya.ad_bhzotrat otrat
       on otrat.codotrat = hist.codotrat
left join sankhya.ad_bhztrat trat
       on trat.codtrat = hist.codtrat
left join sankhya.tsiusu usu
       on usu.codusu = hist.codusu
inner join sankhya.tgfpar par
        on par.codparc = rel.codparc
inner join sankhya.tgfite ite
        on ite.nunota = rel.nunota
       and ite.sequencia = rel.sequencia
where rel.nunota in (3837864,3839184,3837861,3837867,3839274,3837759,3839213,3837760,3838337,3837764)
  and rel.expedido >= trunc(sysdate) - 100
  and rel.codtipoper in
      ('3298','3296','3215','46','47','45','49','43','33','3250','3121','3209','23',
       '3211','71','3218','3231','3120','3124','87','3229','44','3223','3228','3210',
       '3226','3202','3233','2137','85','84','2138','86','73','72','3227','3299',
       '3200','3206','3224','3213','3234','3232','3225','3207','3297','3208','3212','3230')
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
  and rel.codemp in ('1','2','3','4','5','6')
order by rel.nunota, rel.sequencia;
exit
