CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 200
SET LINESIZE 32767
SET FEEDBACK OFF
SET HEADING ON
SET LONG 4000
SET COLSEP ';'
SELECT nuevento, nomeinstancia, descricao, ativo, tipo, ordem,
       DBMS_LOB.SUBSTR(config, 1000, 1) config
FROM sankhya.tsievp
WHERE nomeinstancia IN ('CabecalhoNota','ImportacaoXMLNotas')
  AND LOWER(descricao) LIKE '%vincula pedido%'
ORDER BY nomeinstancia, nuevento;
EXIT
