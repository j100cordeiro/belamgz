CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET FEEDBACK OFF
SET HEADING ON
SET COLSEP ';'
SELECT nuarquivo,
       DBMS_LOB.INSTR(xml, '200001556915951') AS has_xped,
       DBMS_LOB.INSTR(xml, '2000012057490327') AS has_ped_327,
       DBMS_LOB.INSTR(xml, '2000012057490329') AS has_ped_329,
       DBMS_LOB.INSTR(xml, '<xPed>') AS has_tag_xped,
       DBMS_LOB.INSTR(xml, 'pack_id') AS has_pack,
       DBMS_LOB.INSTR(xml, 'order_id') AS has_order
FROM sankhya.tgfixn
WHERE nuarquivo IN (1132981,1132982)
ORDER BY nuarquivo;
EXIT
