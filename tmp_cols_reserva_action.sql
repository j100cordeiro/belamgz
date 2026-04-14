connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 220 trimspool on
col column_name format a20
col data_type format a12
select column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and table_name='TGFEST'
  and column_name in ('CODEMP','CODPROD','CODLOCAL','CONTROLE','ESTOQUE','RESERVADO','DISPONIVEL')
order by column_name;

select column_name, data_type
from all_tab_columns
where owner='SANKHYA'
  and table_name='TGFITE'
  and column_name in ('NUNOTA','SEQUENCIA','CODEMP','CODPROD','CODLOCALORIG','CONTROLE','RESERVA','QTDNEG','QTDENTREGUE')
order by column_name;
exit;
