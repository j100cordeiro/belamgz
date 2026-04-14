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
 * Bloqueia alteracoes sensiveis do cabecalho em requisicao confirmada.
 *
 * Campos tratados para o layout da Biologistica:
 * - DTNEG (Data)
 * - CODPARC (Cliente)
 * - CODCENCUS (Centro de Resultado)
 */
public class BloqueiaCabecalhoRequisicaoConfirmadaTGFCAB extends AbstractEventoProgramavel
        implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger.getLogger(BloqueiaCabecalhoRequisicaoConfirmadaTGFCAB.class.getName());

    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_DTNEG = "DTNEG";
    private static final String FIELD_CODPARC = "CODPARC";
    private static final String FIELD_CODCENCUS = "CODCENCUS";
    private static final String FIELD_TIPMOV = "TIPMOV";
    private static final String FIELD_STATUSNOTA = "STATUSNOTA";

    private static final String TIPMOV_REQUISICAO_PEDIDO = "J";
    private static final String STATUS_CONFIRMADA = "L";
    private static final int USUARIO_LIBERADO = 44;

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        Object vo = extrairVoCompat(event);
        if (vo == null) {
            return;
        }

        if (!isRequisicaoConfirmada(getString(vo, FIELD_TIPMOV), getString(vo, FIELD_STATUSNOTA))) {
            return;
        }

        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        if (nunota == null) {
            return;
        }

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            LinhaCabecalho atual = buscarLinhaAtual(sql, nunota);
            if (atual == null || !isRequisicaoConfirmada(atual.tipmov, atual.statusnota)) {
                return;
            }

            if (isUsuarioLiberado(atual.codusu)) {
                return;
            }

            boolean alterouData = !datasIguais(getPropertyValue(vo, FIELD_DTNEG), atual.dtneg);
            boolean alterouCliente = !bigDecimalsIguais(getBigDecimal(vo, FIELD_CODPARC), atual.codparc);
            boolean alterouCentroResultado = !bigDecimalsIguais(getBigDecimal(vo, FIELD_CODCENCUS), atual.codcencus);

            if (alterouData || alterouCliente || alterouCentroResultado) {
                throw new Exception("Requisicao confirmada. Nao e permitido alterar Data, Cliente "
                        + "ou Centro de Resultado apos a confirmacao.");
            }
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private LinhaCabecalho buscarLinhaAtual(NativeSql sql, BigDecimal nunota) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT TIPMOV, STATUSNOTA, CODUSU, DTNEG, CODPARC, CODCENCUS ");
            sql.appendSql("  FROM TGFCAB ");
            sql.appendSql(" WHERE NUNOTA = :NUNOTA");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            if (!rs.next()) {
                return null;
            }

            LinhaCabecalho linha = new LinhaCabecalho();
            linha.tipmov = rs.getString("TIPMOV");
            linha.statusnota = rs.getString("STATUSNOTA");
            linha.codusu = rs.getBigDecimal("CODUSU");
            linha.dtneg = rs.getTimestamp("DTNEG");
            linha.codparc = rs.getBigDecimal("CODPARC");
            linha.codcencus = rs.getBigDecimal("CODCENCUS");
            return linha;
        } finally {
            closeQuietly(rs);
        }
    }

    private boolean isRequisicaoConfirmada(String tipmov, String statusnota) {
        return TIPMOV_REQUISICAO_PEDIDO.equalsIgnoreCase(trimToEmpty(tipmov))
                && STATUS_CONFIRMADA.equalsIgnoreCase(trimToEmpty(statusnota));
    }

    private boolean isUsuarioLiberado(BigDecimal codusu) {
        return codusu != null && codusu.intValue() == USUARIO_LIBERADO;
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
                LOGGER.log(Level.WARNING, "[REQ-CAB] Nao foi possivel obter VO do evento.", e);
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
        Object value = getPropertyValue(vo, field);
        return value == null ? null : value.toString();
    }

    private boolean bigDecimalsIguais(BigDecimal a, BigDecimal b) {
        BigDecimal valorA = a == null ? BigDecimal.ZERO : a;
        BigDecimal valorB = b == null ? BigDecimal.ZERO : b;
        return valorA.compareTo(valorB) == 0;
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

    private static final class LinhaCabecalho {
        private String tipmov;
        private String statusnota;
        private BigDecimal codusu;
        private Timestamp dtneg;
        private BigDecimal codparc;
        private BigDecimal codcencus;
    }
}
