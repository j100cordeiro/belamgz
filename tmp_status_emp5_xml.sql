CONNECT jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
SET PAGESIZE 500
SET LINESIZE 32767
SET FEEDBACK OFF
SET VERIFY OFF
SET HEADING ON
SET COLSEP ';'

PROMPT FATURAMENTO_XML_EMP5_DESDE_2026_02_01
WITH base AS (
    SELECT
        i.nuarquivo,
        i.nunota AS nunota_fat,
        i.ad_nunotaorig AS nunota_ped,
        c.numnota,
        c.dtneg
    FROM sankhya.tgfixn i
    INNER JOIN sankhya.tgfcab c
        ON c.nunota = i.nunota
    WHERE i.status = 5
      AND c.codemp = 5
      AND c.tipmov = 'V'
      AND TRUNC(c.dtneg) >= DATE '2026-02-01'
)
SELECT
    COUNT(*) AS total_xml_faturado,
    SUM(CASE WHEN b.nunota_ped IS NOT NULL THEN 1 ELSE 0 END) AS com_ad_nunotaorig,
    SUM(CASE WHEN b.nunota_ped IS NOT NULL AND EXISTS (
            SELECT 1 FROM sankhya.tgfvar v
             WHERE v.nunota = b.nunota_fat
               AND v.nunotaorig = b.nunota_ped
        ) THEN 1 ELSE 0 END) AS com_tgfvar,
    SUM(CASE WHEN b.nunota_ped IS NOT NULL AND EXISTS (
            SELECT 1 FROM sankhya.tgfcab p
             WHERE p.nunota = b.nunota_ped
               AND UPPER(TRIM(NVL(p.pendente, 'S'))) = 'N'
        ) THEN 1 ELSE 0 END) AS pedido_nao_pendente,
    SUM(CASE WHEN b.nunota_ped IS NOT NULL AND EXISTS (
            SELECT 1 FROM sankhya.tgfcab p
             WHERE p.nunota = b.nunota_ped
               AND NVL(p.ad_nrontoaorigem, 0) > 0
        ) THEN 1 ELSE 0 END) AS pedido_com_num_nota_origem
FROM base b;

PROMPT DEVOLUCAO_EMP5_DESDE_2026_02_01
SELECT
    COUNT(*) AS total_devolucao,
    SUM(CASE WHEN EXISTS (
            SELECT 1 FROM sankhya.tgfcab p
             WHERE p.ad_nunotadev = d.nunota
        ) THEN 1 ELSE 0 END) AS devolucao_refletida_no_pedido
FROM sankhya.tgfcab d
WHERE d.codemp = 5
  AND d.tipmov = 'D'
  AND TRUNC(d.dtneg) >= DATE '2026-02-01';

EXIT
