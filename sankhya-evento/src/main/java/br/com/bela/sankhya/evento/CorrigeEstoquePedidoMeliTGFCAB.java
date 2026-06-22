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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Corrige TGFEST apos insercao de pedido Full (TOP 3123) da empresa 5 (MELI).
 *
 * Causa: o Sankhya acrescenta QTDNEG ao TGFEST.ESTOQUE quando um pedido
 * TOP 3123 / TIPMOV='P' / CODEMP=5 e inserido, apesar de ATUALEST=N.
 * Isso infla o estoque com cada pedido novo do marketplace.
 *
 * Correcao: apos o INSERT, subtrai a QTDNEG de volta no TGFEST para
 * cada item do pedido, revertendo o incremento indevido.
 *
 * Registrar no Sankhya:
 *   Entidade : CabecalhoNota (TGFCAB)
 *   Disparar : AFTER_INSERT
 *   Classe   : br.com.bela.sankhya.evento.CorrigeEstoquePedidoMeliTGFCAB
 */
public class CorrigeEstoquePedidoMeliTGFCAB extends AbstractEventoProgramavel
        implements EventoProgramavelJava {

    private static final Logger LOGGER =
            Logger.getLogger(CorrigeEstoquePedidoMeliTGFCAB.class.getName());

    private static final String TIPMOV_PEDIDO   = "P";
    private static final int    CODEMP_MELI     = 5;
    private static final int    CODTIPOPER_FULL = 3123;

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        Object vo = extrairVo(event);
        if (vo == null) return;

        String     tipmov     = getString(vo, "TIPMOV");
        BigDecimal codemp     = getBigDecimal(vo, "CODEMP");
        BigDecimal codtipoper = getBigDecimal(vo, "CODTIPOPER");
        BigDecimal nunota     = getBigDecimal(vo, "NUNOTA");

        if (!TIPMOV_PEDIDO.equalsIgnoreCase(trimToEmpty(tipmov))) return;
        if (codemp == null || codemp.intValue() != CODEMP_MELI)   return;
        if (codtipoper == null || codtipoper.intValue() != CODTIPOPER_FULL) return;
        if (nunota == null) return;

        LOGGER.info("[MELI-EST] Pedido Full NUNOTA=" + nunota + " CODEMP=5. Revertendo incremento no TGFEST...");

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper  jdbc   = facade.getJdbcWrapper();
        NativeSql    sql    = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            List<ItemPedido> itens = buscarItens(sql, nunota);
            for (ItemPedido item : itens) {
                subtrairTgfest(sql, item.codprod, codemp, item.codlocal, item.qtdneg);
                LOGGER.info("[MELI-EST] CODPROD=" + item.codprod
                        + " CODLOCAL=" + item.codlocal
                        + " QTDNEG=" + item.qtdneg + " subtraida do TGFEST.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MELI-EST] Erro ao reverter TGFEST NUNOTA=" + nunota, e);
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    // Busca itens do pedido: CODPROD, CODLOCALORIG (local do estoque), QTDNEG
    private List<ItemPedido> buscarItens(NativeSql sql, BigDecimal nunota) throws Exception {
        List<ItemPedido> lista = new ArrayList<>();
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT CODPROD, CODLOCALORIG, QTDNEG ");
            sql.appendSql("  FROM TGFITE WHERE NUNOTA = :NUNOTA");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            while (rs.next()) {
                ItemPedido item = new ItemPedido();
                item.codprod  = rs.getBigDecimal("CODPROD");
                item.codlocal = rs.getBigDecimal("CODLOCALORIG");
                item.qtdneg   = rs.getBigDecimal("QTDNEG");
                if (item.codprod != null && item.qtdneg != null
                        && item.qtdneg.compareTo(BigDecimal.ZERO) > 0) {
                    lista.add(item);
                }
            }
        } finally {
            closeQuietly(rs);
        }
        return lista;
    }

    // Subtrai QTDNEG do TGFEST (reverte incremento indevido)
    private void subtrairTgfest(NativeSql sql, BigDecimal codprod,
                                 BigDecimal codemp, BigDecimal codlocal,
                                 BigDecimal qtdneg) throws Exception {
        sql.resetSqlBuf();
        sql.appendSql("UPDATE TGFEST                              ");
        sql.appendSql("   SET ESTOQUE = ESTOQUE - :QTDNEG         ");
        sql.appendSql(" WHERE CODPROD  = :CODPROD                 ");
        sql.appendSql("   AND CODEMP   = :CODEMP                  ");
        sql.appendSql("   AND CODLOCAL = :CODLOCAL                ");
        sql.setNamedParameter("QTDNEG",   qtdneg);
        sql.setNamedParameter("CODPROD",  codprod);
        sql.setNamedParameter("CODEMP",   codemp);
        sql.setNamedParameter("CODLOCAL", codlocal);
        sql.executeUpdate();
    }

    // --- utilitarios ---
    private Object extrairVo(PersistenceEvent event) {
        if (event == null) return null;
        for (String nome : new String[]{"getVo", "getVO"}) {
            try { return event.getClass().getMethod(nome).invoke(event); }
            catch (Exception ignored) {}
        }
        LOGGER.warning("[MELI-EST] Nao foi possivel obter VO do evento.");
        return null;
    }

    private Object getProperty(Object vo, String field) {
        if (vo == null) return null;
        try {
            Method m = vo.getClass().getMethod("getProperty", String.class);
            return m.invoke(vo, field);
        } catch (Exception e) { return null; }
    }

    private BigDecimal getBigDecimal(Object vo, String field) {
        try {
            Object val = getProperty(vo, field);
            if (val == null) return null;
            if (val instanceof BigDecimal) return (BigDecimal) val;
            return new BigDecimal(val.toString());
        } catch (Exception e) { return null; }
    }

    private String getString(Object vo, String field) {
        Object val = getProperty(vo, field);
        return val == null ? null : val.toString();
    }

    private String trimToEmpty(String s) { return s == null ? "" : s.trim(); }

    private void closeQuietly(ResultSet rs) {
        if (rs == null) return;
        try { rs.close(); } catch (Exception ignored) {}
    }

    private static final class ItemPedido {
        BigDecimal codprod;
        BigDecimal codlocal;
        BigDecimal qtdneg;
    }
}
