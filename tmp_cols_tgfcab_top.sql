set pages 200 lines 200 feedback off verify off heading on
connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
select table_name, column_name from all_tab_columns where owner='SANKHYA' and table_name='TGFCAB' and column_name in ('NUNOTA','NUMNOTA','CHAVENFE','STATUSNOTA','TIPMOV','CODTIPOPER','DHTIPOPER') order by column_name;
select table_name, column_name from all_tab_columns where owner='SANKHYA' and table_name='TGFTOP' and column_name in ('CODTIPOPER','DHALTER','ATUALEST') order by column_name;
exit
