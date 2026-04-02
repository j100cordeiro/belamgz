package br.com.bela.sankhya.evento;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

/**
 * Impede que nota de compra com entrada de estoque seja liberada sem numero da
 * nota e chave fiscal.
 */
public class BloqueiaLiberacaoCompraSemNumeroEChaveTGFCAB extends AbstractEventoProgramavel
        implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger
            .getLogger(BloqueiaLiberacaoCompraSemNumeroEChaveTGFCAB.class.getName());

    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_NUMNOTA = "NUMNOTA";
    private static final String FIELD_CHAVENFE = "CHAVENFE";
    private static final String FIELD_STATUSNOTA = "STATUSNOTA";
    private static final String FIELD_TIPMOV = "TIPMOV";
    private static final String FIELD_CODTIPOPER = "CODTIPOPER";
    private static final String FIELD_DHTIPOPER = "DHTIPOPER";

    private static final String STATUS_LIBERADA = "L";
    private static final String TIPMOV_COMPRA = "C";
    private static final String ATUALEST_ENTRADA = "E";

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        validarLiberacao(event, true);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        validarLiberacao(event, false);
    }

    private void validarLiberacao(PersistenceEvent event, boolean beforeInsert) throws Exception {
        Object vo = extrairVoCompat(event);
        if (vo == null) {
            return;
        }

        if (!TIPMOV_COMPRA.equalsIgnoreCase(trimToEmpty(getString(vo, FIELD_TIPMOV)))) {
            return;
        }

        if (!STATUS_LIBERADA.equalsIgnoreCase(trimToEmpty(getString(vo, FIELD_STATUSNOTA)))) {
            return;
        }

        BigDecimal codTipOper = getBigDecimal(vo, FIELD_CODTIPOPER);
        Object dhTipOper = getPropertyValue(vo, FIELD_DHTIPOPER);
        if (codTipOper == null || dhTipOper == null) {
            LOGGER.warning("[VALCOMPRA] Evento sem CODTIPOPER/DHTIPOPER. Validacao ignorada.");
            return;
        }

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            if (!isTopEntrada(sql, codTipOper, dhTipOper)) {
                return;
            }

            if (!beforeInsert && !estaMudandoParaLiberada(sql, getBigDecimal(vo, FIELD_NUNOTA))) {
                return;
            }

            validarCamposObrigatorios(vo);
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private boolean estaMudandoParaLiberada(NativeSql sql, BigDecimal nunota) throws Exception {
        if (nunota == null) {
            return true;
        }

        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT STATUSNOTA FROM TGFCAB WHERE NUNOTA = :NUNOTA");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            if (!rs.next()) {
                return true;
            }

            String statusAtual = rs.getString("STATUSNOTA");
            return !STATUS_LIBERADA.equalsIgnoreCase(trimToEmpty(statusAtual));
        } finally {
            closeQuietly(rs);
        }
    }

    private boolean isTopEntrada(NativeSql sql, BigDecimal codTipOper, Object dhTipOper) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT ATUALEST ");
            sql.appendSql("  FROM TGFTOP ");
            sql.appendSql(" WHERE CODTIPOPER = :CODTIPOPER ");
            sql.appendSql("   AND DHALTER = :DHALTER ");
            sql.setNamedParameter("CODTIPOPER", codTipOper);
            sql.setNamedParameter("DHALTER", dhTipOper);
            rs = sql.executeQuery();
            if (!rs.next()) {
                LOGGER.warning("[VALCOMPRA] TOP nao encontrada para CODTIPOPER=" + codTipOper);
                return false;
            }

            return ATUALEST_ENTRADA.equalsIgnoreCase(trimToEmpty(rs.getString("ATUALEST")));
        } finally {
            closeQuietly(rs);
        }
    }

    private void validarCamposObrigatorios(Object vo) throws Exception {
        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        BigDecimal numNota = getBigDecimal(vo, FIELD_NUMNOTA);
        String chave = trimToEmpty(getString(vo, FIELD_CHAVENFE));

        if (numNota == null || numNota.compareTo(BigDecimal.ZERO) <= 0 || chave.isEmpty()) {
            throw new Exception("Nao e permitido liberar nota de compra com entrada de estoque sem NUMNOTA e "
                    + "CHAVENFE preenchidos. NUNOTA=" + safe(nunota));
        }
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
                LOGGER.log(Level.WARNING, "[VALCOMPRA] Nao foi possivel obter VO do evento.", e);
                return null;
            }
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

    private String getString(Object vo, String field) {
        try {
            Object value = getPropertyValue(vo, field);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(BigDecimal value) {
        return value == null ? "<null>" : value.toPlainString();
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
}
