connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 240 trimspool on
col nomeparc format a50
select codparc, nomeparc
from sankhya.tgfpar
where codparc in (79264,536527,386550,1058304,511941,0,111066,106709,79918)
order by codparc;
exit;
