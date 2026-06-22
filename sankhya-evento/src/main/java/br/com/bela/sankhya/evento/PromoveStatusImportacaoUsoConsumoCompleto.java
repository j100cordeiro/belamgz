package br.com.bela.sankhya.evento;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

/**
 * Classe completa para TOP 2101: remove evento 75 indevido em TSILIB e promove status em TGFIXN.
 * Use apenas esta classe em TGFIXN para resolver tudo com 1 evento.
 */
public class PromoveStatusImportacaoUsoConsumoCompleto extends AbstractEventoProgramavel
        implements EventoProgramavelJava {

    private static final Logger LOGGER =
            Logger.getLogger(PromoveStatusImportacaoUsoConsumoCompleto.class.getName());

    private static final BigDecimal CODTIPOPER_USO_CONSUMO = new BigDecimal("2101");
    private static final BigDecimal STATUS_PENDENTE_VALIDACAO = new BigDecimal("4");
    private static final BigDecimal STATUS_PROCESSADO = new BigDecimal("5");
    private static final BigDecimal EVENTO_DIVERGENCIA_FISCAL = new BigDecimal("75");

    private static final String FIELD_NUARQUIVO = "NUARQUIVO";
    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_CODTIPOPER = "CODTIPOPER";
    private static final String FIELD_STATUS = "STATUS";

    private static final String TEXTO_ERRO_CFOP = "CFOP";

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        processar(event, "afterInsert");
    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {
        processar(event, "afterUpdate");
    }

    private void processar(PersistenceEvent event, String origemEvento) throws Exception {
        Object vo = extrairVoCompat(event);
        if (vo == null) return;

        BigDecimal nuarquivo = getBigDecimal(vo, FIELD_NUARQUIVO);
        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        BigDecimal codtipoper = getBigDecimal(vo, FIELD_CODTIPOPER);
        BigDecimal status = getBigDecimal(vo, FIELD_STATUS);

        if (nuarquivo == null || nunota == null) return;
        if (!CODTIPOPER_USO_CONSUMO.equals(codtipoper)) return;
        if (!STATUS_PENDENTE_VALIDACAO.equals(status)) return;

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            // Primeiro: limpar TSILIB (remover evento 75 indevido)
            limparLiberacaoFiscal(sql, nunota);

            // Segundo: verificar se ainda há liberações pendentes
            if (existeLiberacaoPendente(sql, nunota)) {
                return;
            }

            // Terceiro: promover status em TGFIXN
            sql.resetSqlBuf();
            sql.appendSql("UPDATE TGFIXN ");
            sql.appendSql("   SET STATUS = :STATUSPROCESSADO ");
            sql.appendSql(" WHERE NUARQUIVO = :NUARQUIVO ");
            sql.appendSql("   AND CODTIPOPER = :CODTIPOPER ");
            sql.appendSql("   AND STATUS = :STATUSPENDENTE ");
            sql.setNamedParameter("STATUSPROCESSADO", STATUS_PROCESSADO);
            sql.setNamedParameter("NUARQUIVO", nuarquivo);
            sql.setNamedParameter("CODTIPOPER", CODTIPOPER_USO_CONSUMO);
            sql.setNamedParameter("STATUSPENDENTE", STATUS_PENDENTE_VALIDACAO);
            sql.executeUpdate();

            LOGGER.info("[IXN2101-COMPLETO] Status promovido para 5 e TSILIB limpo. NUARQUIVO=" + nuarquivo
                    + ", NUNOTA=" + nunota
                    + ", evento=" + origemEvento);

        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    /**
     * Remove evento 75 indevido em TSILIB, exceto se for CFOP.
     */
    private void limparLiberacaoFiscal(NativeSql sql, BigDecimal nunota) throws Exception {
        java.sql.ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT OBSERVACAO ");
            sql.appendSql("  FROM TSILIB ");
            sql.appendSql(" WHERE NUCHAVE = :NUNOTA ");
            sql.appendSql("   AND TABELA = 'TGFCAB' ");
            sql.appendSql("   AND CODTIPOPER = :CODTIPOPER ");
            sql.appendSql("   AND EVENTO = :EVENTO ");
            sql.appendSql("   AND ROWNUM = 1 ");
            sql.setNamedParameter("NUNOTA", nunota);
            sql.setNamedParameter("CODTIPOPER", CODTIPOPER_USO_CONSUMO);
            sql.setNamedParameter("EVENTO", EVENTO_DIVERGENCIA_FISCAL);
            rs = sql.executeQuery();

            String observacao = "";
            if (rs != null && rs.next()) {
                observacao = rs.getString("OBSERVACAO");
            }

            String texto = observacao == null ? "" : observacao.toUpperCase(Locale.ROOT);

            // 🚨 SE TIVER CFOP → NÃO REMOVE
            if (texto.contains(TEXTO_ERRO_CFOP)) {
                LOGGER.info("[IXN2101-COMPLETO] Divergência de CFOP encontrada. Não será removida.");
                return;
            }

        } finally {
            if (rs != null) {
                try { rs.close(); } catch (Exception ignored) {}
            }
        }

        // 🔥 REMOVE QUALQUER OUTRA DIVERGÊNCIA
        sql.resetSqlBuf();
        sql.appendSql("DELETE FROM TSILIB ");
        sql.appendSql(" WHERE NUCHAVE = :NUNOTA ");
        sql.appendSql("   AND TABELA = 'TGFCAB' ");
        sql.appendSql("   AND CODTIPOPER = :CODTIPOPER ");
        sql.appendSql("   AND EVENTO = :EVENTO ");
        sql.setNamedParameter("NUNOTA", nunota);
        sql.setNamedParameter("CODTIPOPER", CODTIPOPER_USO_CONSUMO);
        sql.setNamedParameter("EVENTO", EVENTO_DIVERGENCIA_FISCAL);
        sql.executeUpdate();

        LOGGER.info("[IXN2101-COMPLETO] Liberação fiscal removida automaticamente. NUNOTA=" + nunota);
    }

    private boolean existeLiberacaoPendente(NativeSql sql, BigDecimal nunota) throws Exception {
        java.sql.ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT 1 FROM TSILIB ");
            sql.appendSql(" WHERE NUCHAVE = :NUNOTA ");
            sql.appendSql("   AND (DHLIB IS NULL OR REPROVADO = 'S') ");
            sql.appendSql("   AND ROWNUM = 1 ");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            return rs != null && rs.next();
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (Exception ignored) {}
            }
        }
    }

    private Object extrairVoCompat(PersistenceEvent event) {
        if (event == null) return null;
        try {
            Method method = event.getClass().getMethod("getVo");
            return method.invoke(event);
        } catch (Exception e) {
            try {
                Method method = event.getClass().getMethod("getVO");
                return method.invoke(event);
            } catch (Exception ignored) {
                LOGGER.log(Level.WARNING, "Erro ao obter VO", e);
                return null;
            }
        }
    }

    private Object getPropertyValue(Object vo, String field) {
        if (vo == null) return null;
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
            if (value == null) return null;
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}