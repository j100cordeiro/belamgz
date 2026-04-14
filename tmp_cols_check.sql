CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET COLSEP ';'
SELECT column_name
FROM all_tab_columns
WHERE owner='SANKHYA'
  AND table_name='TGFCAB'
  AND column_name IN ('AD_NRONTOAORIGEM','AD_NUNOTADEV','AD_PEDIDOMKTPLACE','AD_CODMKT','BH_CODEMKT','PENDENTE')
ORDER BY column_name;
EXIT
