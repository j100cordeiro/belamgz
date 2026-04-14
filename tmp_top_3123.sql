SET PAGESIZE 200
SET LINESIZE 320

SELECT t.codtipoper,
       t.dhalter,
       t.descroper,
       t.tipmov,
       t.atualest,
       t.atualestmp,
       t.statusbaixaest,
       t.reservasemlote,
       t.consdisbaixest,
       t.valest,
       t.valestmaximo
  FROM sankhya.tgftop t
 WHERE t.codtipoper = 3123
 ORDER BY t.dhalter DESC;

EXIT
