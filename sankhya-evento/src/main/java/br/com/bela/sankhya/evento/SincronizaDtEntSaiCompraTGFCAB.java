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
 * Mantem DTENTSAI igual a DTNEG em movimentos de compra da TGFCAB.
 *
 * Regras:
 * - no before insert/update: tenta sincronizar o VO em memoria;
 * - no after insert/update: grava direto na TGFCAB para garantir persistencia;
 * - edicao manual nao permanece apos salvar, pois o after update volta DTENTSAI para DTNEG.
 */
public class SincronizaDtEntSaiCompraTGFCAB extends AbstractEventoProgramavel implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger.getLogger(SincronizaDtEntSaiCompraTGFCAB.class.getName());

    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_TIPMOV = "TIPMOV";
    private static final String FIELD_DTNEG = "DTNEG";
    private static final String FIELD_DTENTSAI = "DTENTSAI";
    private static final String TIPMOV_COMPRA = "C";
    private static final String TIPMOV_PEDIDO_COMPRA = "O";

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        sincronizarDtEntSai(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        validarEdicaoManualDtEntSai(event);
        sincronizarDtEntSai(event);
    }

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        sincronizarDtEntSaiBanco(event);
    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {
        sincronizarDtEntSaiBanco(event);
    }

    private void sincronizarDtEntSai(PersistenceEvent event) throws Exception {
        Object vo = extrairVoCompat(event);
        if (vo == null) {
            return;
        }

        if (!isMovimentoCompra(getString(vo, FIELD_TIPMOV))) {
            return;
        }

        Object dtneg = getPropertyValue(vo, FIELD_DTNEG);
        if (dtneg == null) {
            LOGGER.fine("[DTENTSAI] DTNEG nula em movimento de compra. Sincronizacao ignorada.");
            return;
        }

        setPropertyValue(vo, FIELD_DTENTSAI, normalizarData(dtneg));
    }

    private void sincronizarDtEntSaiBanco(PersistenceEvent event) throws Exception {
        Object vo = extrairVoCompat(event);
        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        if (nunota == null) {
            LOGGER.fine("[DTENTSAI] NUNOTA nula no after event. Sincronizacao em banco ignorada.");
            return;
        }

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            if (!isCompra(sql, nunota)) {
                return;
            }

            sql.resetSqlBuf();
            sql.appendSql("UPDATE TGFCAB ");
            sql.appendSql("   SET DTENTSAI = DTNEG ");
            sql.appendSql(" WHERE NUNOTA = :NUNOTA ");
            sql.appendSql("   AND TIPMOV = 'C' ");
            sql.appendSql("   AND (DTENTSAI IS NULL OR DTENTSAI <> DTNEG) ");
            sql.setNamedParameter("NUNOTA", nunota);
            sql.executeUpdate();
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private void validarEdicaoManualDtEntSai(PersistenceEvent event) throws Exception {
        Object vo = extrairVoCompat(event);
        if (vo == null) {
            return;
        }

        if (!isMovimentoCompra(getString(vo, FIELD_TIPMOV))) {
            return;
        }

        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        if (nunota == null) {
            return;
        }

        Object dtnegAtual = getPropertyValue(vo, FIELD_DTNEG);
        Object dtEntSaiAtual = getPropertyValue(vo, FIELD_DTENTSAI);

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            LinhaCabecalho linhaAtual = buscarLinhaAtual(sql, nunota);
            if (linhaAtual == null || !isMovimentoCompra(linhaAtual.tipmov)) {
                return;
            }

            boolean dtnegMudou = !datasIguais(dtnegAtual, linhaAtual.dtneg);
            boolean dtEntSaiMudou = !datasIguais(dtEntSaiAtual, linhaAtual.dtentsai);

            if (dtEntSaiMudou && !dtnegMudou) {
                throw new Exception("Nao e permitido editar manualmente a Dt. Entrada/Saida. "
                        + "Altere apenas a Dt. Negociacao.");
            }
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private boolean isCompra(NativeSql sql, BigDecimal nunota) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT TIPMOV FROM TGFCAB WHERE NUNOTA = :NUNOTA");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            if (!rs.next()) {
                return false;
            }
            return isMovimentoCompra(rs.getString("TIPMOV"));
        } finally {
            closeQuietly(rs);
        }
    }

    private LinhaCabecalho buscarLinhaAtual(NativeSql sql, BigDecimal nunota) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT TIPMOV, DTNEG, DTENTSAI ");
            sql.appendSql("  FROM TGFCAB ");
            sql.appendSql(" WHERE NUNOTA = :NUNOTA");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            if (!rs.next()) {
                return null;
            }

            LinhaCabecalho linha = new LinhaCabecalho();
            linha.tipmov = rs.getString("TIPMOV");
            linha.dtneg = rs.getTimestamp("DTNEG");
            linha.dtentsai = rs.getTimestamp("DTENTSAI");
            return linha;
        } finally {
            closeQuietly(rs);
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
                LOGGER.log(Level.WARNING, "[DTENTSAI] Nao foi possivel obter VO do evento.", e);
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

    private void setPropertyValue(Object vo, String field, Object value) throws Exception {
        try {
            Method setter = vo.getClass().getMethod("setProperty", String.class, Object.class);
            setter.invoke(vo, field, value);
            return;
        } catch (NoSuchMethodException ignored) {
            // fallback abaixo
        }

        try {
            Method setter = vo.getClass().getMethod("set", String.class, Object.class);
            setter.invoke(vo, field, value);
            return;
        } catch (NoSuchMethodException ignored) {
            // fallback abaixo
        }

        throw new Exception("Nao foi possivel atribuir " + field + " no VO da TGFCAB.");
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

    private Object normalizarData(Object value) {
        if (value instanceof Timestamp) {
            return value;
        }
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        return value;
    }

    private String getString(Object vo, String field) {
        Object value = getPropertyValue(vo, field);
        return value == null ? null : value.toString();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isMovimentoCompra(String tipmov) {
        String valor = trimToEmpty(tipmov);
        return TIPMOV_COMPRA.equalsIgnoreCase(valor)
                || TIPMOV_PEDIDO_COMPRA.equalsIgnoreCase(valor);
    }

    private boolean datasIguais(Object data1, Object data2) {
        Timestamp ts1 = toTimestamp(data1);
        Timestamp ts2 = toTimestamp(data2);

        if (ts1 == null && ts2 == null) {
            return true;
        }
        if (ts1 == null || ts2 == null) {
            return false;
        }
        return ts1.getTime() == ts2.getTime();
    }

    private Timestamp toTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        return null;
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

    private static final class LinhaCabecalho {
        private String tipmov;
        private Timestamp dtneg;
        private Timestamp dtentsai;
    }
}
