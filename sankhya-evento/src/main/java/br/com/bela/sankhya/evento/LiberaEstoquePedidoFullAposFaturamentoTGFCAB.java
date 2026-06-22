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
 * Libera o efeito de estoque do pedido Full (TOP 3123) somente quando o
 * faturamento correspondente ja estiver processado e vinculado pela TGFVAR.
 *
 * Logica:
 * 1) Roda na TGFCAB apos insert/update de notas de venda da empresa 5.
 * 2) Procura itens da venda vinculados a pedido TIPMOV='P' via TGFVAR.
 * 3) Para cada item, calcula o delta ainda nao liberado em TGFVAR.QTDATENDIDA.
 * 4) Subtrai esse delta do TGFEST do local do pedido.
 * 5) Atualiza TGFVAR.QTDATENDIDA para evitar dupla baixa em reprocessamentos.
 *
 * Assim, o pedido continua existindo para BI, mas para de interferir no
 * estoque somente quando a NF importada efetivamente entrar no fluxo.
 */
public class LiberaEstoquePedidoFullAposFaturamentoTGFCAB extends AbstractEventoProgramavel
        implements EventoProgramavelJava {

    private static final Logger LOGGER =
            Logger.getLogger(LiberaEstoquePedidoFullAposFaturamentoTGFCAB.class.getName());

    private static final String TIPMOV_VENDA = "V";
    private static final String TIPMOV_PEDIDO = "P";
    private static final int CODEMP_MELI = 5;
    private static final BigDecimal CODPARC_ESTOQUE = BigDecimal.ZERO;
    private static final String CONTROLE_VAZIO = " ";
    private static final String TIPO_ESTOQUE = "P";

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        processar(event, "afterInsert");
    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {
        processar(event, "afterUpdate");
    }

    private void processar(PersistenceEvent event, String origemEvento) {
        Object vo = extrairVo(event);
        if (vo == null) return;

        BigDecimal nunota = getBigDecimal(vo, "NUNOTA");
        BigDecimal codemp = getBigDecimal(vo, "CODEMP");
        String tipmov = getString(vo, "TIPMOV");
        String statusnota = getString(vo, "STATUSNOTA");

        if (nunota == null) return;
        if (codemp == null || codemp.intValue() != CODEMP_MELI) return;
        if (!TIPMOV_VENDA.equalsIgnoreCase(trimToEmpty(tipmov))) return;
        if (!"L".equalsIgnoreCase(trimToEmpty(statusnota))) return;

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            List<ItemAtendimento> itens = buscarItensAtendidos(sql, nunota);
            if (itens.isEmpty()) {
                LOGGER.fine("[MELI-EST-FAT] NF sem pedido Full vinculado. NUNOTA=" + nunota
                        + ", evento=" + origemEvento);
                return;
            }

            for (ItemAtendimento item : itens) {
                if (item.deltaAProcessar.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                if (!existeLinhaEstoque(sql, item)) {
                    LOGGER.warning("[MELI-EST-FAT] TGFEST ausente para liberar pedido. NF=" + nunota
                            + ", PED=" + item.nunotaPedido
                            + ", CODPROD=" + item.codprod
                            + ", CODLOCAL=" + item.codlocalPedido
                            + ". Delta mantido pendente para reprocesso.");
                    continue;
                }

                subtrairTgfest(sql, item);
                atualizarQtdAtendidaTgfvar(sql, item);

                LOGGER.info("[MELI-EST-FAT] Estoque liberado pelo faturamento. NF=" + nunota
                        + ", PED=" + item.nunotaPedido
                        + ", SEQFAT=" + item.sequenciaFat
                        + ", SEQPED=" + item.sequenciaPedido
                        + ", CODPROD=" + item.codprod
                        + ", CODLOCAL=" + item.codlocalPedido
                        + ", DELTA=" + item.deltaAProcessar);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MELI-EST-FAT] Erro ao liberar estoque da NF " + nunota, e);
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private List<ItemAtendimento> buscarItensAtendidos(NativeSql sql, BigDecimal nunotaFat) throws Exception {
        List<ItemAtendimento> itens = new ArrayList<>();
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT v.NUNOTA, v.SEQUENCIA, v.NUNOTAORIG, v.SEQUENCIAORIG, ");
            sql.appendSql("       NVL(v.QTDATENDIDA, 0) AS QTDATENDIDA, ");
            sql.appendSql("       NVL(nf.QTDNEG, 0) AS QTDFAT, ");
            sql.appendSql("       nf.CODPROD, ");
            sql.appendSql("       ped.CODLOCALORIG, ");
            sql.appendSql("       NVL(ped.CONTROLE, ' ') AS CONTROLEPED, ");
            sql.appendSql("       cabPed.CODEMP ");
            sql.appendSql("  FROM TGFVAR v ");
            sql.appendSql("  JOIN TGFITE nf ");
            sql.appendSql("    ON nf.NUNOTA = v.NUNOTA ");
            sql.appendSql("   AND nf.SEQUENCIA = v.SEQUENCIA ");
            sql.appendSql("  JOIN TGFITE ped ");
            sql.appendSql("    ON ped.NUNOTA = v.NUNOTAORIG ");
            sql.appendSql("   AND ped.SEQUENCIA = v.SEQUENCIAORIG ");
            sql.appendSql("  JOIN TGFCAB cabPed ");
            sql.appendSql("    ON cabPed.NUNOTA = ped.NUNOTA ");
            sql.appendSql(" WHERE v.NUNOTA = :NUNOTAFAT ");
            sql.appendSql("   AND cabPed.TIPMOV = :TIPMOVPED ");
            sql.appendSql("   AND cabPed.CODEMP = :CODEMP ");

            sql.setNamedParameter("NUNOTAFAT", nunotaFat);
            sql.setNamedParameter("TIPMOVPED", TIPMOV_PEDIDO);
            sql.setNamedParameter("CODEMP", new BigDecimal(CODEMP_MELI));

            rs = sql.executeQuery();
            while (rs.next()) {
                ItemAtendimento item = new ItemAtendimento();
                item.nunotaFat = nunotaFat;
                item.sequenciaFat = rs.getBigDecimal("SEQUENCIA");
                item.nunotaPedido = rs.getBigDecimal("NUNOTAORIG");
                item.sequenciaPedido = rs.getBigDecimal("SEQUENCIAORIG");
                item.qtdAtendidaAtual = rs.getBigDecimal("QTDATENDIDA");
                item.qtdFat = rs.getBigDecimal("QTDFAT");
                item.codprod = rs.getBigDecimal("CODPROD");
                item.codlocalPedido = rs.getBigDecimal("CODLOCALORIG");
                item.controlePedido = trimToEmpty(rs.getString("CONTROLEPED"));
                if (item.controlePedido.length() == 0) {
                    item.controlePedido = CONTROLE_VAZIO;
                }
                item.codempPedido = rs.getBigDecimal("CODEMP");
                item.deltaAProcessar = nvl(item.qtdFat).subtract(nvl(item.qtdAtendidaAtual));
                itens.add(item);
            }
        } finally {
            closeQuietly(rs);
        }
        return itens;
    }

    private boolean existeLinhaEstoque(NativeSql sql, ItemAtendimento item) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT 1 ");
            sql.appendSql("  FROM TGFEST ");
            sql.appendSql(" WHERE CODEMP = :CODEMP ");
            sql.appendSql("   AND CODLOCAL = :CODLOCAL ");
            sql.appendSql("   AND CODPROD = :CODPROD ");
            sql.appendSql("   AND CONTROLE = :CONTROLE ");
            sql.appendSql("   AND CODPARC = :CODPARC ");
            sql.appendSql("   AND TIPO = :TIPO ");
            sql.appendSql("   AND ROWNUM = 1 ");
            preencherParametrosEstoque(sql, item);
            rs = sql.executeQuery();
            return rs.next();
        } finally {
            closeQuietly(rs);
        }
    }

    private void subtrairTgfest(NativeSql sql, ItemAtendimento item) throws Exception {
        sql.resetSqlBuf();
        sql.appendSql("UPDATE TGFEST ");
        sql.appendSql("   SET ESTOQUE = NVL(ESTOQUE, 0) - :DELTA ");
        sql.appendSql(" WHERE CODEMP = :CODEMP ");
        sql.appendSql("   AND CODLOCAL = :CODLOCAL ");
        sql.appendSql("   AND CODPROD = :CODPROD ");
        sql.appendSql("   AND CONTROLE = :CONTROLE ");
        sql.appendSql("   AND CODPARC = :CODPARC ");
        sql.appendSql("   AND TIPO = :TIPO ");
        preencherParametrosEstoque(sql, item);
        sql.setNamedParameter("DELTA", item.deltaAProcessar);
        sql.executeUpdate();
    }

    private void atualizarQtdAtendidaTgfvar(NativeSql sql, ItemAtendimento item) throws Exception {
        sql.resetSqlBuf();
        sql.appendSql("UPDATE TGFVAR ");
        sql.appendSql("   SET QTDATENDIDA = :QTDATENDIDA ");
        sql.appendSql(" WHERE NUNOTA = :NUNOTAFAT ");
        sql.appendSql("   AND SEQUENCIA = :SEQFAT ");
        sql.appendSql("   AND NUNOTAORIG = :NUNOTAPED ");
        sql.appendSql("   AND SEQUENCIAORIG = :SEQPED ");
        sql.setNamedParameter("QTDATENDIDA", item.qtdFat);
        sql.setNamedParameter("NUNOTAFAT", item.nunotaFat);
        sql.setNamedParameter("SEQFAT", item.sequenciaFat);
        sql.setNamedParameter("NUNOTAPED", item.nunotaPedido);
        sql.setNamedParameter("SEQPED", item.sequenciaPedido);
        sql.executeUpdate();
    }

    private void preencherParametrosEstoque(NativeSql sql, ItemAtendimento item) throws Exception {
        sql.setNamedParameter("CODEMP", item.codempPedido);
        sql.setNamedParameter("CODLOCAL", item.codlocalPedido);
        sql.setNamedParameter("CODPROD", item.codprod);
        sql.setNamedParameter("CONTROLE", item.controlePedido);
        sql.setNamedParameter("CODPARC", CODPARC_ESTOQUE);
        sql.setNamedParameter("TIPO", TIPO_ESTOQUE);
    }

    private Object extrairVo(PersistenceEvent event) {
        if (event == null) return null;
        for (String nome : new String[]{"getVo", "getVO"}) {
            try {
                return event.getClass().getMethod(nome).invoke(event);
            } catch (Exception ignored) {
            }
        }
        LOGGER.warning("[MELI-EST-FAT] Nao foi possivel obter VO do evento.");
        return null;
    }

    private Object getProperty(Object vo, String field) {
        if (vo == null) return null;
        try {
            Method m = vo.getClass().getMethod("getProperty", String.class);
            return m.invoke(vo, field);
        } catch (Exception e) {
            return null;
        }
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

    private String getString(Object vo, String field) {
        Object val = getProperty(vo, field);
        return val == null ? null : val.toString();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private void closeQuietly(ResultSet rs) {
        if (rs == null) return;
        try {
            rs.close();
        } catch (Exception ignored) {
        }
    }

    private static final class ItemAtendimento {
        BigDecimal nunotaFat;
        BigDecimal sequenciaFat;
        BigDecimal nunotaPedido;
        BigDecimal sequenciaPedido;
        BigDecimal qtdAtendidaAtual;
        BigDecimal qtdFat;
        BigDecimal deltaAProcessar;
        BigDecimal codprod;
        BigDecimal codlocalPedido;
        BigDecimal codempPedido;
        String controlePedido;
    }
}
