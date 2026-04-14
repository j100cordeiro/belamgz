connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 300 lines 260 trimspool on
col nomemethod format a40
col nomeparc format a40
col ad_melhorenvio format a5
select * from sankhya.tgfcont where nunota = 3828407 order by 1;
exit;
