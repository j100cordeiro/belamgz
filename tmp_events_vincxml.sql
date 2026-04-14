CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET LONG 20000
SET LONGCHUNKSIZE 20000
SET COLSEP ';'

SELECT nuevento, nomeinstancia, descricao, ativo, tipo, resourceid, ordem,
       DBMS_LOB.SUBSTR(config, 4000, 1) AS config
FROM sankhya.tsievp
WHERE LOWER(DBMS_LOB.SUBSTR(config, 4000, 1)) LIKE '%vinculapedidomarketplacexmltgfcab%'
   OR LOWER(descricao) LIKE '%vincula pedido%'
ORDER BY nomeinstancia, ordem;

EXIT
