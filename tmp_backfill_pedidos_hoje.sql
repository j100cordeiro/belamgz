SET SERVEROUTPUT ON
SET PAGESIZE 200
SET LINESIZE 300
SET FEEDBACK ON

PROMPT === PRE-CHECK: pedidos de hoje com faturamento e campos divergentes ===
WITH vinc AS (
    SELECT ped.nunota AS nunota_ped,
           fat.nunota AS nunota_fat,
           fat.numnota AS numnota_fat,
           ROW_NUMBER() OVER (PARTITION BY ped.nunota ORDER BY fat.dtneg DESC, fat.nunota DESC) rn
      FROM SANKHYA.TGFCAB ped
      JOIN SANKHYA.TGFVAR v ON v.nunotaorig = ped.nunota
      JOIN SANKHYA.TGFCAB fat ON fat.nunota = v.nunota
     WHERE ped.tipmov = 'P'
       AND fat.tipmov = 'V'
       AND ped.codemp = 5
       AND fat.codemp = 5
       AND TRUNC(fat.dtneg) = TRUNC(SYSDATE)
       AND NVL(fat.numnota,0) > 0
)
SELECT COUNT(*) AS QTD_A_AJUSTAR
  FROM SANKHYA.TGFCAB ped
  JOIN vinc x ON x.nunota_ped = ped.nunota AND x.rn = 1
 WHERE NVL(ped.ad_nrontoaorigem,0) <> x.numnota_fat
    OR NVL(ped.numnota,0) <> x.numnota_fat
    OR NVL(TRIM(ped.pendente),'S') <> 'N';

PROMPT === EXECUTANDO UPDATE ===
DECLARE
  v_rows NUMBER := 0;
BEGIN
  MERGE INTO SANKHYA.TGFCAB ped
  USING (
      SELECT nunota_ped, numnota_fat
        FROM (
          SELECT ped.nunota AS nunota_ped,
                 fat.numnota AS numnota_fat,
                 ROW_NUMBER() OVER (PARTITION BY ped.nunota ORDER BY fat.dtneg DESC, fat.nunota DESC) rn
            FROM SANKHYA.TGFCAB ped
            JOIN SANKHYA.TGFVAR v ON v.nunotaorig = ped.nunota
            JOIN SANKHYA.TGFCAB fat ON fat.nunota = v.nunota
           WHERE ped.tipmov = 'P'
             AND fat.tipmov = 'V'
             AND ped.codemp = 5
             AND fat.codemp = 5
             AND TRUNC(fat.dtneg) = TRUNC(SYSDATE)
             AND NVL(fat.numnota,0) > 0
        )
       WHERE rn = 1
  ) src
  ON (ped.nunota = src.nunota_ped)
  WHEN MATCHED THEN UPDATE SET
       ped.ad_nrontoaorigem = src.numnota_fat,
       ped.numnota = src.numnota_fat,
       ped.pendente = 'N'
   WHERE NVL(ped.ad_nrontoaorigem,0) <> src.numnota_fat
      OR NVL(ped.numnota,0) <> src.numnota_fat
      OR NVL(TRIM(ped.pendente),'S') <> 'N';

  v_rows := SQL%ROWCOUNT;
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('PEDIDOS_ATUALIZADOS=' || v_rows);
END;
/

PROMPT === POS-CHECK ===
WITH vinc AS (
    SELECT ped.nunota AS nunota_ped,
           fat.nunota AS nunota_fat,
           fat.numnota AS numnota_fat,
           ROW_NUMBER() OVER (PARTITION BY ped.nunota ORDER BY fat.dtneg DESC, fat.nunota DESC) rn
      FROM SANKHYA.TGFCAB ped
      JOIN SANKHYA.TGFVAR v ON v.nunotaorig = ped.nunota
      JOIN SANKHYA.TGFCAB fat ON fat.nunota = v.nunota
     WHERE ped.tipmov = 'P'
       AND fat.tipmov = 'V'
       AND ped.codemp = 5
       AND fat.codemp = 5
       AND TRUNC(fat.dtneg) = TRUNC(SYSDATE)
       AND NVL(fat.numnota,0) > 0
)
SELECT COUNT(*) AS QTD_AINDA_DIVERGENTE
  FROM SANKHYA.TGFCAB ped
  JOIN vinc x ON x.nunota_ped = ped.nunota AND x.rn = 1
 WHERE NVL(ped.ad_nrontoaorigem,0) <> x.numnota_fat
    OR NVL(ped.numnota,0) <> x.numnota_fat
    OR NVL(TRIM(ped.pendente),'S') <> 'N';

PROMPT === AMOSTRA ATUALIZADA (HOJE) ===
SELECT *
  FROM (
    SELECT ped.nunota AS nunota_pedido,
           ped.numnota AS numnota_pedido,
           ped.ad_nrontoaorigem,
           ped.pendente,
           fat.nunota AS nunota_fat,
           fat.numnota AS numnota_fat,
           fat.dtneg AS dt_fat
      FROM SANKHYA.TGFCAB ped
      JOIN SANKHYA.TGFVAR v ON v.nunotaorig = ped.nunota
      JOIN SANKHYA.TGFCAB fat ON fat.nunota = v.nunota
     WHERE ped.tipmov = 'P'
       AND fat.tipmov = 'V'
       AND ped.codemp = 5
       AND fat.codemp = 5
       AND TRUNC(fat.dtneg) = TRUNC(SYSDATE)
     ORDER BY fat.nunota DESC
  )
 WHERE ROWNUM <= 20;

EXIT
