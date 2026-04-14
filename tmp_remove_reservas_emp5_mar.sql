connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set serveroutput on
set pages 300 lines 240 trimspool on
col nunota format 9999999
col itens format 999999
col qtd_aberta format 999999.999
col total_itens format 999999
col total_qtd_aberta format 999999.999
col codprod format 999999
col codlocal format 999999
col estoque format 999999.999
col reservado format 999999.999
col qtd_a_liberar format 999999.999

prompt ===== BEFORE (pedidos com reserva) =====
with alvo as (
    select i.nunota,
           i.sequencia,
           i.codemp,
           i.codprod,
           i.codlocalorig as codlocal,
           nvl(i.controle,' ') as controle,
           greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) as qtd_aberta
      from sankhya.tgfite i
      join sankhya.tgfcab c on c.nunota = i.nunota
     where c.codemp = 5
       and c.tipmov = 'P'
       and c.codtipoper = 3123
       and trunc(c.dtneg) between trunc(sysdate,'MM') and trunc(last_day(sysdate))
       and nvl(c.pendente,'N') = 'S'
       and nvl(i.reserva,'N') = 'S'
       and greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) > 0
)
select count(*) total_itens, sum(qtd_aberta) total_qtd_aberta from alvo;

with alvo as (
    select i.nunota,
           greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) as qtd_aberta
      from sankhya.tgfite i
      join sankhya.tgfcab c on c.nunota = i.nunota
     where c.codemp = 5
       and c.tipmov = 'P'
       and c.codtipoper = 3123
       and trunc(c.dtneg) between trunc(sysdate,'MM') and trunc(last_day(sysdate))
       and nvl(c.pendente,'N') = 'S'
       and nvl(i.reserva,'N') = 'S'
       and greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) > 0
)
select nunota, count(*) itens, sum(qtd_aberta) qtd_aberta
  from alvo
 group by nunota
 order by nunota;

prompt ===== BEFORE (produto 16595/local 10500) =====
select e.codemp, e.codprod, e.codlocal, e.estoque, e.reservado
  from sankhya.tgfest e
 where e.codemp = 5
   and e.codprod = 16595
   and e.codlocal = 10500;

prompt ===== UPDATE TGFEST.RESERVADO =====
update sankhya.tgfest e
   set e.reservado = greatest(
         nvl(e.reservado,0) - nvl((
            select sum(greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0))
              from sankhya.tgfite i
              join sankhya.tgfcab c on c.nunota = i.nunota
             where c.codemp = 5
               and c.tipmov = 'P'
               and c.codtipoper = 3123
               and trunc(c.dtneg) between trunc(sysdate,'MM') and trunc(last_day(sysdate))
               and nvl(c.pendente,'N') = 'S'
               and nvl(i.reserva,'N') = 'S'
               and greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) > 0
               and i.codemp = e.codemp
               and i.codprod = e.codprod
               and i.codlocalorig = e.codlocal
               and nvl(i.controle,' ') = nvl(e.controle,' ')
         ),0),
         0
       )
 where exists (
            select 1
              from sankhya.tgfite i
              join sankhya.tgfcab c on c.nunota = i.nunota
             where c.codemp = 5
               and c.tipmov = 'P'
               and c.codtipoper = 3123
               and trunc(c.dtneg) between trunc(sysdate,'MM') and trunc(last_day(sysdate))
               and nvl(c.pendente,'N') = 'S'
               and nvl(i.reserva,'N') = 'S'
               and greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) > 0
               and i.codemp = e.codemp
               and i.codprod = e.codprod
               and i.codlocalorig = e.codlocal
               and nvl(i.controle,' ') = nvl(e.controle,' ')
       );

dbms_output.put_line('TGFEST linhas atualizadas: ' || SQL%ROWCOUNT);

prompt ===== UPDATE TGFITE.RESERVA =====
update sankhya.tgfite i
   set i.reserva = 'N'
 where nvl(i.reserva,'N') = 'S'
   and greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) > 0
   and exists (
            select 1
              from sankhya.tgfcab c
             where c.nunota = i.nunota
               and c.codemp = 5
               and c.tipmov = 'P'
               and c.codtipoper = 3123
               and trunc(c.dtneg) between trunc(sysdate,'MM') and trunc(last_day(sysdate))
               and nvl(c.pendente,'N') = 'S'
       );

dbms_output.put_line('TGFITE linhas atualizadas: ' || SQL%ROWCOUNT);

commit;

prompt ===== AFTER (pedidos com reserva) =====
with alvo as (
    select i.nunota,
           greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) as qtd_aberta
      from sankhya.tgfite i
      join sankhya.tgfcab c on c.nunota = i.nunota
     where c.codemp = 5
       and c.tipmov = 'P'
       and c.codtipoper = 3123
       and trunc(c.dtneg) between trunc(sysdate,'MM') and trunc(last_day(sysdate))
       and nvl(c.pendente,'N') = 'S'
       and nvl(i.reserva,'N') = 'S'
       and greatest(nvl(i.qtdneg,0)-nvl(i.qtdentregue,0),0) > 0
)
select count(*) total_itens, nvl(sum(qtd_aberta),0) total_qtd_aberta from alvo;

prompt ===== AFTER (produto 16595/local 10500) =====
select e.codemp, e.codprod, e.codlocal, e.estoque, e.reservado
  from sankhya.tgfest e
 where e.codemp = 5
   and e.codprod = 16595
   and e.codlocal = 10500;

exit;
