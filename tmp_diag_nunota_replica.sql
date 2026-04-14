CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 500
SET LINESIZE 32767
SET FEEDBACK OFF
SET VERIFY OFF
SET HEADING ON
SET COLSEP ';'

PROMPT CAB
SELECT
    c.nunota,
    c.numnota,
    c.tipmov,
    c.codemp,
    c.codtipoper,
    TO_CHAR(c.dtneg, 'YYYY-MM-DD') AS dtneg,
    c.codparc,
    c.vlrnota,
    c.statusnfe,
    c.pendente,
    c.ad_nrontoaorigem,
    c.ad_nunotadev,
    c.ad_pedidomktplace,
    c.ad_codmkt,
    c.bh_codemkt
FROM sankhya.tgfcab c
WHERE c.nunota IN (3827125, 3827126, 3827675, 3827676)
ORDER BY c.nunota;

PROMPT IXN
SELECT
    i.nuarquivo,
    i.nunota,
    i.numnota,
    i.codtipoper,
    i.status,
    TO_CHAR(i.dhprocess, 'YYYY-MM-DD HH24:MI:SS') AS dhprocess,
    i.ad_nunotaorig,
    REGEXP_SUBSTR(DBMS_LOB.SUBSTR(i.xml, 4000, 1), '<xPed>([^<]+)</xPed>', 1, 1, NULL, 1) AS xped,
    SUBSTR(i.detalhesimportacao, 1, 300) AS detalhes
FROM sankhya.tgfixn i
WHERE i.nunota IN (3827125, 3827126, 3827675, 3827676)
   OR i.ad_nunotaorig IN (3827125, 3827126, 3827675, 3827676)
ORDER BY i.nuarquivo DESC;

PROMPT VAR
SELECT
    v.nunota,
    v.sequencia,
    v.nunotaorig,
    v.sequenciaorig,
    v.statusnota
FROM sankhya.tgfvar v
WHERE v.nunota IN (3827125, 3827126, 3827675, 3827676)
   OR v.nunotaorig IN (3827125, 3827126, 3827675, 3827676)
ORDER BY v.nunota, v.sequencia;

EXIT
