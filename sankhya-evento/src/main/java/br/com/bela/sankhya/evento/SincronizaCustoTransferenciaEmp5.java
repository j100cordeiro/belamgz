package br.com.bela.sankhya.evento;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

/**
 * Replica o custo da empresa 1 para a empresa 5 no momento em que a
 * transferencia entre empresas e efetivada na TGFCAB.
 *
 * Regra aplicada:
 * - somente para TGFCAB da empresa 5;
 * - somente em transferencia entre empresas (TIPMOV='T' e TOP 78);
 * - somente quando a nota estiver liberada;
 * - cria/atualiza a linha de TGFCUS em CODEMP=5 com DTATUAL = DTNEG da transferencia;
 * - copia o ultimo custo conhecido da empresa 1 para cada produto do documento.
 */
public class SincronizaCustoTransferenciaEmp5 extends AbstractEventoProgramavel implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger.getLogger(SincronizaCustoTransferenciaEmp5.class.getName());

    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final int EMPRESA_DESTINO = 5;
    private static final int EMPRESA_ORIGEM = 1;
    private static final int TOP_TRANSFERENCIA = 78;

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        sincronizarCustos(event, "afterInsert");
    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {
        sincronizarCustos(event, "afterUpdate");
    }

    private void sincronizarCustos(PersistenceEvent event, String origemEvento) throws Exception {
        Object vo = extrairVoCompat(event);
        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        if (nunota == null) {
            LOGGER.fine("[CUS-TRANSF] NUNOTA ausente no evento " + origemEvento + ".");
            return;
        }

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            CabecalhoTransferencia cabecalho = buscarCabecalhoTransferencia(sql, nunota);
            if (cabecalho == null) {
                LOGGER.fine("[CUS-TRANSF] TGFCAB nao encontrada. NUNOTA=" + nunota);
                return;
            }

            if (!deveProcessar(cabecalho)) {
                LOGGER.fine("[CUS-TRANSF] Documento fora da regra. NUNOTA=" + nunota
                        + ", CODEMP=" + cabecalho.codemp
                        + ", TIPMOV=" + cabecalho.tipmov
                        + ", CODTIPOPER=" + cabecalho.codtipoper
                        + ", STATUSNOTA=" + cabecalho.statusnota);
                return;
            }

            int atualizados = sincronizarItens(sql, nunota, cabecalho.dtneg);
            LOGGER.info("[CUS-TRANSF] Custos sincronizados da empresa 1 para 5. NUNOTA="
                    + nunota + ", itensAtualizados=" + atualizados + ", evento=" + origemEvento);
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private int sincronizarItens(NativeSql sql, BigDecimal nunota, Timestamp dtneg) throws Exception {
        ResultSet rs = null;
        int total = 0;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT DISTINCT CODPROD ");
            sql.appendSql("  FROM TGFITE ");
            sql.appendSql(" WHERE NUNOTA = :NUNOTA ");
            sql.appendSql("   AND CODPROD IS NOT NULL ");
            sql.appendSql("   AND SEQUENCIA > 0 ");
            sql.setNamedParameter("NUNOTA", nunota);

            rs = sql.executeQuery();
            while (rs.next()) {
                BigDecimal codprod = rs.getBigDecimal("CODPROD");
                if (codprod == null) {
                    continue;
                }
                if (replicarCustoProduto(sql, nunota, codprod, dtneg)) {
                    total++;
                }
            }
            return total;
        } finally {
            closeQuietly(rs);
        }
    }

    private boolean replicarCustoProduto(NativeSql sql, BigDecimal nunota, BigDecimal codprod, Timestamp dtneg)
            throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT CODLOCAL, CONTROLE, CUSMEDICM, CUSSEMICM, CUSREP, CUSVARIAVEL, ");
            sql.appendSql("       CUSGER, CUSMED, VLRVENDAFIXO, ENTRADACOMICMS, ENTRADASEMICMS, ");
            sql.appendSql("       QTDNEG, AUTOMATICO, ALTPRECO, TOTALCOMICMS, TOTALSEMICMS, ");
            sql.appendSql("       TOTALCOMICMSANT, TOTALSEMICMSANT, CUSMEDCALC, RECARGA, ");
            sql.appendSql("       QTDNEGGER, TIPO, PROCESSO ");
            sql.appendSql("  FROM TGFCUS ");
            sql.appendSql(" WHERE CODPROD = :CODPROD ");
            sql.appendSql("   AND CODEMP = :CODEMPORIG ");
            sql.appendSql("   AND DTATUAL = ( ");
            sql.appendSql("       SELECT MAX(DTATUAL) ");
            sql.appendSql("         FROM TGFCUS ");
            sql.appendSql("        WHERE CODPROD = :CODPROD ");
            sql.appendSql("          AND CODEMP = :CODEMPORIG ");
            sql.appendSql("          AND DTATUAL <= :DTNEG) ");
            sql.appendSql(" ORDER BY CODLOCAL, CONTROLE ");
            sql.setNamedParameter("CODPROD", codprod);
            sql.setNamedParameter("CODEMPORIG", BigDecimal.valueOf(EMPRESA_ORIGEM));
            sql.setNamedParameter("DTNEG", dtneg);

            rs = sql.executeQuery();
            if (!rs.next()) {
                LOGGER.warning("[CUS-TRANSF] Sem custo origem na empresa 1 para o produto "
                        + codprod + " na data " + dtneg + ". NUNOTA=" + nunota);
                return false;
            }

            BigDecimal codlocal = rs.getBigDecimal("CODLOCAL");
            String controle = rs.getString("CONTROLE");

            sql.resetSqlBuf();
            sql.appendSql("MERGE INTO TGFCUS DST ");
            sql.appendSql("USING (SELECT :CODPROD CODPROD, :CODEMP CODEMP, :DTATUAL DTATUAL, :CODLOCAL CODLOCAL, :CONTROLE CONTROLE FROM DUAL) SRC ");
            sql.appendSql("   ON (DST.CODPROD = SRC.CODPROD ");
            sql.appendSql("   AND DST.CODEMP = SRC.CODEMP ");
            sql.appendSql("   AND DST.DTATUAL = SRC.DTATUAL ");
            sql.appendSql("   AND DST.CODLOCAL = SRC.CODLOCAL ");
            sql.appendSql("   AND NVL(DST.CONTROLE,' ') = NVL(SRC.CONTROLE,' ')) ");
            sql.appendSql("WHEN MATCHED THEN UPDATE SET ");
            sql.appendSql("    DST.CUSMEDICM = :CUSMEDICM, DST.CUSSEMICM = :CUSSEMICM, DST.CUSREP = :CUSREP, ");
            sql.appendSql("    DST.CUSVARIAVEL = :CUSVARIAVEL, DST.CUSGER = :CUSGER, DST.CUSMED = :CUSMED, ");
            sql.appendSql("    DST.VLRVENDAFIXO = :VLRVENDAFIXO, DST.ENTRADACOMICMS = :ENTRADACOMICMS, ");
            sql.appendSql("    DST.ENTRADASEMICMS = :ENTRADASEMICMS, DST.QTDNEG = :QTDNEG, DST.AUTOMATICO = :AUTOMATICO, ");
            sql.appendSql("    DST.ALTPRECO = :ALTPRECO, DST.NUNOTA = :NUNOTA, DST.SEQUENCIA = 0, ");
            sql.appendSql("    DST.TOTALCOMICMS = :TOTALCOMICMS, DST.TOTALSEMICMS = :TOTALSEMICMS, ");
            sql.appendSql("    DST.TOTALCOMICMSANT = :TOTALCOMICMSANT, DST.TOTALSEMICMSANT = :TOTALSEMICMSANT, ");
            sql.appendSql("    DST.CUSMEDCALC = :CUSMEDCALC, DST.RECARGA = :RECARGA, DST.QTDNEGGER = :QTDNEGGER, ");
            sql.appendSql("    DST.TIPO = :TIPO, DST.PROCESSO = :PROCESSO, DST.DHALTER = SYSDATE ");
            sql.appendSql("WHEN NOT MATCHED THEN INSERT ( ");
            sql.appendSql("    CODPROD, CODEMP, DTATUAL, CODLOCAL, CONTROLE, CUSMEDICM, CUSSEMICM, CUSREP, ");
            sql.appendSql("    CUSVARIAVEL, CUSGER, CUSMED, VLRVENDAFIXO, ENTRADACOMICMS, ENTRADASEMICMS, ");
            sql.appendSql("    QTDNEG, AUTOMATICO, ALTPRECO, NUNOTA, SEQUENCIA, TOTALCOMICMS, TOTALSEMICMS, ");
            sql.appendSql("    TOTALCOMICMSANT, TOTALSEMICMSANT, CUSMEDCALC, RECARGA, QTDNEGGER, TIPO, PROCESSO, DHALTER) ");
            sql.appendSql("VALUES ( ");
            sql.appendSql("    :CODPROD, :CODEMP, :DTATUAL, :CODLOCAL, :CONTROLE, :CUSMEDICM, :CUSSEMICM, :CUSREP, ");
            sql.appendSql("    :CUSVARIAVEL, :CUSGER, :CUSMED, :VLRVENDAFIXO, :ENTRADACOMICMS, :ENTRADASEMICMS, ");
            sql.appendSql("    :QTDNEG, :AUTOMATICO, :ALTPRECO, :NUNOTA, 0, :TOTALCOMICMS, :TOTALSEMICMS, ");
            sql.appendSql("    :TOTALCOMICMSANT, :TOTALSEMICMSANT, :CUSMEDCALC, :RECARGA, :QTDNEGGER, :TIPO, :PROCESSO, SYSDATE) ");

            sql.setNamedParameter("CODPROD", codprod);
            sql.setNamedParameter("CODEMP", BigDecimal.valueOf(EMPRESA_DESTINO));
            sql.setNamedParameter("DTATUAL", dtneg);
            sql.setNamedParameter("CODLOCAL", codlocal == null ? BigDecimal.ZERO : codlocal);
            sql.setNamedParameter("CONTROLE", controle == null ? " " : controle);
            sql.setNamedParameter("CUSMEDICM", rs.getBigDecimal("CUSMEDICM"));
            sql.setNamedParameter("CUSSEMICM", rs.getBigDecimal("CUSSEMICM"));
            sql.setNamedParameter("CUSREP", rs.getBigDecimal("CUSREP"));
            sql.setNamedParameter("CUSVARIAVEL", rs.getBigDecimal("CUSVARIAVEL"));
            sql.setNamedParameter("CUSGER", rs.getBigDecimal("CUSGER"));
            sql.setNamedParameter("CUSMED", rs.getBigDecimal("CUSMED"));
            sql.setNamedParameter("VLRVENDAFIXO", rs.getBigDecimal("VLRVENDAFIXO"));
            sql.setNamedParameter("ENTRADACOMICMS", rs.getBigDecimal("ENTRADACOMICMS"));
            sql.setNamedParameter("ENTRADASEMICMS", rs.getBigDecimal("ENTRADASEMICMS"));
            sql.setNamedParameter("QTDNEG", rs.getBigDecimal("QTDNEG"));
            sql.setNamedParameter("AUTOMATICO", rs.getString("AUTOMATICO"));
            sql.setNamedParameter("ALTPRECO", rs.getString("ALTPRECO"));
            sql.setNamedParameter("NUNOTA", nunota);
            sql.setNamedParameter("TOTALCOMICMS", rs.getBigDecimal("TOTALCOMICMS"));
            sql.setNamedParameter("TOTALSEMICMS", rs.getBigDecimal("TOTALSEMICMS"));
            sql.setNamedParameter("TOTALCOMICMSANT", rs.getBigDecimal("TOTALCOMICMSANT"));
            sql.setNamedParameter("TOTALSEMICMSANT", rs.getBigDecimal("TOTALSEMICMSANT"));
            sql.setNamedParameter("CUSMEDCALC", rs.getBigDecimal("CUSMEDCALC"));
            sql.setNamedParameter("RECARGA", rs.getBigDecimal("RECARGA"));
            sql.setNamedParameter("QTDNEGGER", rs.getBigDecimal("QTDNEGGER"));
            sql.setNamedParameter("TIPO", rs.getString("TIPO"));
            sql.setNamedParameter("PROCESSO", rs.getString("PROCESSO"));
            sql.executeUpdate();

            LOGGER.fine("[CUS-TRANSF] Custo replicado. NUNOTA=" + nunota + ", CODPROD=" + codprod
                    + ", CODLOCAL=" + (codlocal == null ? BigDecimal.ZERO : codlocal)
                    + ", CONTROLE=" + (controle == null ? " " : controle));
            return true;
        } finally {
            closeQuietly(rs);
        }
    }

    private CabecalhoTransferencia buscarCabecalhoTransferencia(NativeSql sql, BigDecimal nunota) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT NUNOTA, CODEMP, TIPMOV, CODTIPOPER, STATUSNOTA, DTNEG ");
            sql.appendSql("  FROM TGFCAB ");
            sql.appendSql(" WHERE NUNOTA = :NUNOTA");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            if (!rs.next()) {
                return null;
            }

            CabecalhoTransferencia cabecalho = new CabecalhoTransferencia();
            cabecalho.nunota = rs.getBigDecimal("NUNOTA");
            cabecalho.codemp = rs.getBigDecimal("CODEMP");
            cabecalho.tipmov = rs.getString("TIPMOV");
            cabecalho.codtipoper = rs.getBigDecimal("CODTIPOPER");
            cabecalho.statusnota = rs.getString("STATUSNOTA");
            cabecalho.dtneg = rs.getTimestamp("DTNEG");
            return cabecalho;
        } finally {
            closeQuietly(rs);
        }
    }

    private boolean deveProcessar(CabecalhoTransferencia cabecalho) {
        return cabecalho != null
                && BigDecimal.valueOf(EMPRESA_DESTINO).equals(cabecalho.codemp)
                && BigDecimal.valueOf(TOP_TRANSFERENCIA).equals(cabecalho.codtipoper)
                && "T".equalsIgnoreCase(trimToEmpty(cabecalho.tipmov))
                && "L".equalsIgnoreCase(trimToEmpty(cabecalho.statusnota))
                && cabecalho.dtneg != null;
    }

    private Object extrairVoCompat(PersistenceEvent event) {
        if (event == null) {
            return null;
        }
        try {
            Method method = event.getClass().getMethod("getVo");
            return method.invoke(event);
        } catch (Exception e) {
            try {
                Method method = event.getClass().getMethod("getVO");
                return method.invoke(event);
            } catch (Exception ignored) {
                LOGGER.log(Level.WARNING, "[CUS-TRANSF] Nao foi possivel obter VO do evento.", e);
                return null;
            }
        }
    }

    private BigDecimal getBigDecimal(Object vo, String field) {
        try {
            Object value = getPropertyValue(vo, field);
            if (value == null) {
                return null;
            }
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return new BigDecimal(value.toString());
            }
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Object getPropertyValue(Object vo, String field) {
        if (vo == null || field == null) {
            return null;
        }
        try {
            Method getter = vo.getClass().getMethod("getProperty", String.class);
            return getter.invoke(vo, field);
        } catch (Exception e) {
            return null;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void closeQuietly(ResultSet rs) {
        if (rs == null) {
            return;
        }
        try {
            rs.close();
        } catch (Exception ignored) {
        }
    }

    private static final class CabecalhoTransferencia {
        private BigDecimal nunota;
        private BigDecimal codemp;
        private String tipmov;
        private BigDecimal codtipoper;
        private String statusnota;
        private Timestamp dtneg;
    }
}
