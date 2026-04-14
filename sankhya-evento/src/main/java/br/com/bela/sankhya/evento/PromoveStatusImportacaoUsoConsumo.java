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
 * Promove TGFIXN.STATUS de 4 para 5 nas importacoes da TOP 2101 quando a nota
 * ja foi gerada e nao existe mais liberacao pendente na TSILIB.
 *
 * A propria rotina limpa a liberacao fiscal indevida do evento 75 quando a
 * divergencia for somente de NCM/CEST para uso e consumo.
 */
public class PromoveStatusImportacaoUsoConsumo extends AbstractEventoProgramavel
        implements EventoProgramavelJava {

    private static final Logger LOGGER =
            Logger.getLogger(PromoveStatusImportacaoUsoConsumo.class.getName());

    private static final BigDecimal CODTIPOPER_USO_CONSUMO = new BigDecimal("2101");
    private static final BigDecimal STATUS_PENDENTE_VALIDACAO = new BigDecimal("4");
    private static final BigDecimal STATUS_PROCESSADO = new BigDecimal("5");
    private static final BigDecimal EVENTO_DIVERGENCIA_FISCAL = new BigDecimal("75");

    private static final String FIELD_NUARQUIVO = "NUARQUIVO";
    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_CODTIPOPER = "CODTIPOPER";
    private static final String FIELD_STATUS = "STATUS";

    private static final String TEXTO_ERRO_NCM = "PRODS. ERRO NCM";
    private static final String TEXTO_ERRO_CEST = "PRODS. ERRO CEST";
    private static final String TEXTO_ERRO_ORIGEM = "PRODS. ERRO ORIGEM";
    private static final String TEXTO_ERRO_FCI = "PRODS. ERRO FCI";

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
        if (vo == null) {
            return;
        }

        BigDecimal nuarquivo = getBigDecimal(vo, FIELD_NUARQUIVO);
        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        BigDecimal codtipoper = getBigDecimal(vo, FIELD_CODTIPOPER);
        BigDecimal status = getBigDecimal(vo, FIELD_STATUS);

        if (nuarquivo == null || nunota == null) {
            return;
        }
        if (!CODTIPOPER_USO_CONSUMO.equals(codtipoper)) {
            return;
        }
        if (!STATUS_PENDENTE_VALIDACAO.equals(status)) {
            return;
        }

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            limparLiberacaoFiscalIndevida(sql, nunota);

            if (existeLiberacaoPendente(sql, nunota)) {
                return;
            }

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

            LOGGER.info("[IXN2101] Status promovido para 5. NUARQUIVO=" + nuarquivo
                    + ", NUNOTA=" + nunota
                    + ", evento=" + origemEvento);
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private void limparLiberacaoFiscalIndevida(NativeSql sql, BigDecimal nunota) throws Exception {
        java.sql.ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT OBSERVACAO ");
            sql.appendSql("  FROM TSILIB ");
            sql.appendSql(" WHERE NUCHAVE = :NUNOTA ");
            sql.appendSql("   AND TABELA = 'TGFCAB' ");
            sql.appendSql("   AND CODTIPOPER = :CODTIPOPER ");
            sql.appendSql("   AND EVENTO = :EVENTO ");
            sql.appendSql("   AND NVL(SEQUENCIA, 0) = 0 ");
            sql.appendSql("   AND ROWNUM = 1 ");
            sql.setNamedParameter("NUNOTA", nunota);
            sql.setNamedParameter("CODTIPOPER", CODTIPOPER_USO_CONSUMO);
            sql.setNamedParameter("EVENTO", EVENTO_DIVERGENCIA_FISCAL);
            rs = sql.executeQuery();

            String observacao = null;
            if (rs != null && rs.next()) {
                observacao = rs.getString("OBSERVACAO");
            }
            if (!isObservacaoSomenteDivergenciaIgnoravel(observacao)) {
                return;
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception ignored) {
                }
            }
        }

        sql.resetSqlBuf();
        sql.appendSql("DELETE FROM TSILIB ");
        sql.appendSql(" WHERE NUCHAVE = :NUNOTA ");
        sql.appendSql("   AND TABELA = 'TGFCAB' ");
        sql.appendSql("   AND CODTIPOPER = :CODTIPOPER ");
        sql.appendSql("   AND EVENTO = :EVENTO ");
        sql.appendSql("   AND (UPPER(NVL(OBSERVACAO, ' ')) LIKE :ERRONCM ");
        sql.appendSql("        OR UPPER(NVL(OBSERVACAO, ' ')) LIKE :ERROCEST) ");
        sql.appendSql("   AND UPPER(NVL(OBSERVACAO, ' ')) NOT LIKE :ERROORIGEM ");
        sql.appendSql("   AND UPPER(NVL(OBSERVACAO, ' ')) NOT LIKE :ERROFCI ");
        sql.setNamedParameter("NUNOTA", nunota);
        sql.setNamedParameter("CODTIPOPER", CODTIPOPER_USO_CONSUMO);
        sql.setNamedParameter("EVENTO", EVENTO_DIVERGENCIA_FISCAL);
        sql.setNamedParameter("ERRONCM", "%" + TEXTO_ERRO_NCM + "%");
        sql.setNamedParameter("ERROCEST", "%" + TEXTO_ERRO_CEST + "%");
        sql.setNamedParameter("ERROORIGEM", "%" + TEXTO_ERRO_ORIGEM + "%");
        sql.setNamedParameter("ERROFCI", "%" + TEXTO_ERRO_FCI + "%");
        sql.executeUpdate();
    }

    private boolean existeLiberacaoPendente(NativeSql sql, BigDecimal nunota) throws Exception {
        java.sql.ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT 1 ");
            sql.appendSql("  FROM TSILIB ");
            sql.appendSql(" WHERE NUCHAVE = :NUNOTA ");
            sql.appendSql("   AND TABELA IN ('TGFCAB', 'TGFITE') ");
            sql.appendSql("   AND (DHLIB IS NULL OR REPROVADO = 'S' OR VLRATUAL > VLRLIBERADO) ");
            sql.appendSql("   AND ROWNUM = 1 ");
            sql.setNamedParameter("NUNOTA", nunota);
            rs = sql.executeQuery();
            return rs != null && rs.next();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception ignored) {
                }
            }
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
                LOGGER.log(Level.WARNING, "[IXN2101] Nao foi possivel obter VO do evento.", e);
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

    private boolean isObservacaoSomenteDivergenciaIgnoravel(String observacao) {
        String texto = observacao == null ? "" : observacao.trim().toUpperCase(Locale.ROOT);
        if (texto.isEmpty()) {
            return false;
        }

        boolean temNcm = texto.contains(TEXTO_ERRO_NCM);
        boolean temCest = texto.contains(TEXTO_ERRO_CEST);
        boolean temOrigem = texto.contains(TEXTO_ERRO_ORIGEM);
        boolean temFci = texto.contains(TEXTO_ERRO_FCI);

        return (temNcm || temCest) && !temOrigem && !temFci;
    }
}
