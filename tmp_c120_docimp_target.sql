connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 260 trimspool on
select c.codemp, c.nunota, c.numnota, c.tipmov, c.codtipoper, to_char(c.dtmov,'YYYY-MM-DD') dtmov,
       i.sequencia, i.seqdi, i.docimp, i.numacdraw, i.nrodocumento, to_char(i.dtregistro,'YYYY-MM-DD') dtregistro,
       i.vlrpisimp, i.vlrcofinsimp
from sankhya.tgfidi i
join sankhya.tgfcab c on c.nunota = i.nunota
where to_char(i.docimp) = '26BR00001090233'
order by c.codemp, c.dtmov, c.nunota, i.sequencia;
exit;
