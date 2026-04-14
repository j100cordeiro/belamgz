CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'

WITH p AS (
    SELECT '200001556915951' AS xped FROM dual
)
SELECT 'BH_MKTSALES' origem, COUNT(*) qtd
FROM sankhya.bh_mktsales t, p
WHERE REGEXP_REPLACE(TO_CHAR(t.codmkt), '[^0-9]', '') IN (
    p.xped,
    p.xped || '1',
    p.xped || '2',
    p.xped || '3'
)
UNION ALL
SELECT 'BH_MKTSALESITE', COUNT(*)
FROM sankhya.bh_mktsalesite t, p
WHERE REGEXP_REPLACE(TO_CHAR(t.codmkt), '[^0-9]', '') IN (
    p.xped,
    p.xped || '1',
    p.xped || '2',
    p.xped || '3'
)
UNION ALL
SELECT 'BH_MKTKONCILI.ORDERID', COUNT(*)
FROM sankhya.bh_mktkoncili t, p
WHERE REGEXP_REPLACE(TO_CHAR(t.orderid), '[^0-9]', '') = p.xped
UNION ALL
SELECT 'BH_MKTKONCILI.ORDERIDHUBORIGIN', COUNT(*)
FROM sankhya.bh_mktkoncili t, p
WHERE REGEXP_REPLACE(TO_CHAR(t.orderidhuborigin), '[^0-9]', '') = p.xped
UNION ALL
SELECT 'AD_CONRES.ORDERID', COUNT(*)
FROM sankhya.ad_conres t, p
WHERE REGEXP_REPLACE(TO_CHAR(t.orderid), '[^0-9]', '') = p.xped
UNION ALL
SELECT 'AD_CONRES.ORDERIDHUBORIGIN', COUNT(*)
FROM sankhya.ad_conres t, p
WHERE REGEXP_REPLACE(TO_CHAR(t.orderidhuborigin), '[^0-9]', '') = p.xped;

EXIT
