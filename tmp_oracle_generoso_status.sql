connect jackson/"kiriku#1060"@//127.0.0.1:11521/skwpdb.sub03311532240.vncbela.oraclevcn.com
set pages 200 lines 200 trimspool on feedback on verify off
SELECT
  rel.nunota,
  rel.transportadora,
  rel.parcont,
  rel.dtneg,
  rel.dtfatur,
  hist.situacao,
  CASE
    WHEN hist.situacao IS NOT NULL THEN SANKHYA.OPTION_LABEL('AD_BHZHIST','SITUACAO',hist.situacao)
    WHEN rel.dtentreganf IS NOT NULL THEN 'Entregue'
    ELSE 'Em Rota'
  END AS situacao_label,
  (SELECT ms.nomepadrao
     FROM SANKHYA.AD_MICROSTATUSINTELIPOST ms
    WHERE ms.codigo = (
            SELECT s.codmicro
              FROM SANKHYA.AD_STATUSINTELIPOST s
             WHERE s.nunota = rel.nunota
               AND s.dtalter = (
                     SELECT MAX(s2.dtalter)
                       FROM SANKHYA.AD_STATUSINTELIPOST s2
                      WHERE s2.nunota = s.nunota
                   )
         )
  ) AS nomepadrao
FROM sankhya.bhz_reltransp rel
LEFT JOIN SANKHYA.AD_BHZHIST hist
       ON hist.nunota = rel.nunota
      AND (rel.sequencia = hist.sequencia OR hist.sequencia IS NULL)
      AND hist.seq = (
            SELECT MAX(H2.SEQ)
            FROM SANKHYA.AD_BHZHIST H2
            WHERE H2.NUNOTA = rel.NUNOTA
              AND (rel.sequencia = H2.sequencia OR H2.sequencia IS NULL)
      )
WHERE rel.nunota = 3840831;
exit
