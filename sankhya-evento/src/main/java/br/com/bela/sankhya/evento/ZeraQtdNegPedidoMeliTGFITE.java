package br.com.bela.sankhya.evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zera QTDNEG em itens de pedido (TIPMOV='P') da empresa 5 (MELI/Full).
 *
 * Motivo: pedidos do marketplace Full acrescentam saldo nos relatorios
 * de movimentacao pois QTDNEG fica positivo, distorcendo o estoque
 * exibido. Como esses pedidos nao devem movimentar estoque nem livro,
 * a quantidade negociada e zerada na insercao do item.
 *
 * Registrar na TGFITE: BEFORE INSERT.
 */
public class ZeraQtdNegPedidoMeliTGFITE extends AbstractEventoProgramavel implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger.getLogger(ZeraQtdNegPedidoMeliTGFITE.class.getName());

    private static final String TIPMOV_PEDIDO = "P";
    private static final int    CODEMP_MELI   = 5;

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        processarItem(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        processarItem(event);
    }

    private void processarItem(PersistenceEvent event) throws Exception {
        Object vo = extrairVo(event);
        if (vo == null) return;

        BigDecimal nunota = getBigDecimal(vo, "NUNOTA");
        if (nunota == null) return;

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);
        ResultSet rs = null;

        try {
            jdbc.openSession();

            sql.appendSql("SELECT TIPMOV, CODEMP FROM TGFCAB WHERE NUNOTA = :NUNOTA");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();

            if (!rs.next()) return;

            String tipmov = rs.getString("TIPMOV");
            int    codemp = rs.getInt("CODEMP");

            if (TIPMOV_PEDIDO.equalsIgnoreCase(tipmov != null ? tipmov.trim() : "")
                    && codemp == CODEMP_MELI) {
                setProperty(vo, "QTDNEG",  BigDecimal.ZERO);
                setProperty(vo, "VLRTOT",  BigDecimal.ZERO);
                setProperty(vo, "VLRUNIT", BigDecimal.ZERO);
                LOGGER.fine("[MELI-PEDIDO] NUNOTA=" + nunota + " QTDNEG zerado (TIPMOV=P, CODEMP=5).");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MELI-PEDIDO] Erro ao processar NUNOTA=" + nunota, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private Object extrairVo(PersistenceEvent event) {
        if (event == null) return null;
        for (String nome : new String[]{"getVo", "getVO"}) {
            try {
                Method m = event.getClass().getMethod(nome);
                return m.invoke(event);
            } catch (Exception ignored) {}
        }
        LOGGER.warning("[MELI-PEDIDO] Nao foi possivel obter VO do evento.");
        return null;
    }

    private BigDecimal getBigDecimal(Object vo, String field) {
        try {
            Object val = getProperty(vo, field);
            if (val == null) return null;
            if (val instanceof BigDecimal) return (BigDecimal) val;
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Object getProperty(Object vo, String field) {
        try {
            Method m = vo.getClass().getMethod("getProperty", String.class);
            return m.invoke(vo, field);
        } catch (Exception e) {
            return null;
        }
    }

    private void setProperty(Object vo, String field, Object value) {
        try {
            Method m = vo.getClass().getMethod("setProperty", String.class, Object.class);
            m.invoke(vo, field, value);
        } catch (Exception e) {
            LOGGER.warning("[MELI-PEDIDO] Nao foi possivel setar " + field + ": " + e.getMessage());
        }
    }
}
