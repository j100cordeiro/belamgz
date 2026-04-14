connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 100 lines 220 feedback on verify off
prompt === ANTES 64749 ===
select codparc, nomeparc, ad_codtranspint from sankhya.tgfpar where codparc = 64749;
update sankhya.tgfpar set ad_codtranspint = 64749 where codparc = 64749 and nvl(ad_codtranspint,-1) <> 64749;
commit;
prompt === DEPOIS 64749 ===
select codparc, nomeparc, ad_codtranspint from sankhya.tgfpar where codparc = 64749;
exit
