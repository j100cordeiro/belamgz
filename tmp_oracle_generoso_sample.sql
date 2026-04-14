connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 200 trimspool on feedback on verify off
select * from (
  select rel.nunota, rel.codparctransp, rel.transportadora, rel.parcont, rel.dtfatur, rel.dtneg
  from sankhya.bhz_reltransp rel
  where rel.codparctransp = 64749
  order by rel.dtfatur desc
) where rownum <= 5;
exit
