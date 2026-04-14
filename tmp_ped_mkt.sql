CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET COLSEP ';'
SELECT c.nunota, c.ad_pedidomktplace, c.ad_codmkt, c.bh_codemkt
FROM sankhya.tgfcab c
WHERE c.nunota IN (3827125, 3827126);
EXIT
