-- C120 duplicado - caso 26BR00001090233
select c.codemp, c.nunota, c.numnota, c.tipmov, c.codtipoper, to_char(c.dtmov,'YYYY-MM-DD') dtmov,
       i.sequencia, i.seqdi, i.docimp, i.numacdraw, i.nrodocumento, to_char(i.dtregistro,'YYYY-MM-DD') dtregistro,
       i.vlrpisimp, i.vlrcofinsimp
from sankhya.tgfidi i
join sankhya.tgfcab c on c.nunota = i.nunota
where upper(i.nrodocumento) = '26BR00001090233'
   or upper(i.numacdraw) = '26BR00001090233'
order by c.codemp, c.dtmov, c.nunota, i.sequencia;
