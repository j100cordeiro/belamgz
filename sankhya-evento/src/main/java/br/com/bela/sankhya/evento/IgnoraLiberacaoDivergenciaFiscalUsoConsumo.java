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
 * Remove solicitacoes de liberacao fiscal da TOP 2101 quando o importador XML
 * gerar divergencia apenas de NCM/CEST em uso e consumo.
 */
public class IgnoraLiberacaoDivergenciaFiscalUsoConsumo extends AbstractEventoProgramavel
        implements EventoProgramavelJava {

    private static final Logger LOGGER =
            Logger.getLogger(IgnoraLiberacaoDivergenciaFiscalUsoConsumo.class.getName());

    private static final BigDecimal CODTIPOPER_USO_CONSUMO = new BigDecimal("2101");
    private static final BigDecimal EVENTO_DIVERGENCIA_FISCAL = new BigDecimal("75");

    private static final String FIELD_CODTIPOPER = "CODTIPOPER";
    private static final String FIELD_EVENTO = "EVENTO";
    private static final String FIELD_TABELA = "TABELA";
    private static final String FIELD_NUCHAVE = "NUCHAVE";
    private static final String FIELD_SEQUENCIA = "SEQUENCIA";
    private static final String FIELD_OBSERVACAO = "OBSERVACAO";

    private static final String TABELA_CABECALHO = "TGFCAB";
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

        BigDecimal codtipoper = getBigDecimal(vo, FIELD_CODTIPOPER);
        BigDecimal eventoLiberacao = getBigDecimal(vo, FIELD_EVENTO);
        String tabela = trimToEmpty(getString(vo, FIELD_TABELA));
        BigDecimal nuchave = getBigDecimal(vo, FIELD_NUCHAVE);
        BigDecimal sequencia = normalizarSequencia(getBigDecimal(vo, FIELD_SEQUENCIA));
        String observacao = getString(vo, FIELD_OBSERVACAO);

        if (!CODTIPOPER_USO_CONSUMO.equals(codtipoper)) {
            return;
        }
        if (!EVENTO_DIVERGENCIA_FISCAL.equals(eventoLiberacao)) {
            return;
        }
        if (!TABELA_CABECALHO.equalsIgnoreCase(tabela)) {
            return;
        }
        if (nuchave == null) {
            return;
        }
        if (!isObservacaoSomenteDivergenciaIgnoravel(observacao)) {
            return;
        }

        removerLiberacao(nuchave, sequencia);
        LOGGER.info("[LIB2101] Liberacao removida para NUCHAVE=" + nuchave
                + ", SEQUENCIA=" + sequencia
                + ", evento=" + origemEvento);
    }

    private void removerLiberacao(BigDecimal nuchave, BigDecimal sequencia) throws Exception {
        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            sql.resetSqlBuf();
            sql.appendSql("DELETE FROM TSILIB ");
            sql.appendSql(" WHERE CODTIPOPER = :CODTIPOPER ");
            sql.appendSql("   AND EVENTO = :EVENTO ");
            sql.appendSql("   AND TABELA = :TABELA ");
            sql.appendSql("   AND NUCHAVE = :NUCHAVE ");
            sql.appendSql("   AND NVL(SEQUENCIA, 0) = :SEQUENCIA ");
            sql.appendSql("   AND (UPPER(NVL(OBSERVACAO, ' ')) LIKE :ERRONCM ");
            sql.appendSql("        OR UPPER(NVL(OBSERVACAO, ' ')) LIKE :ERROCEST) ");
            sql.appendSql("   AND UPPER(NVL(OBSERVACAO, ' ')) NOT LIKE :ERROORIGEM ");
            sql.appendSql("   AND UPPER(NVL(OBSERVACAO, ' ')) NOT LIKE :ERROFCI ");

            sql.setNamedParameter("CODTIPOPER", CODTIPOPER_USO_CONSUMO);
            sql.setNamedParameter("EVENTO", EVENTO_DIVERGENCIA_FISCAL);
            sql.setNamedParameter("TABELA", TABELA_CABECALHO);
            sql.setNamedParameter("NUCHAVE", nuchave);
            sql.setNamedParameter("SEQUENCIA", sequencia);
            sql.setNamedParameter("ERRONCM", "%" + TEXTO_ERRO_NCM + "%");
            sql.setNamedParameter("ERROCEST", "%" + TEXTO_ERRO_CEST + "%");
            sql.setNamedParameter("ERROORIGEM", "%" + TEXTO_ERRO_ORIGEM + "%");
            sql.setNamedParameter("ERROFCI", "%" + TEXTO_ERRO_FCI + "%");
            sql.executeUpdate();
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private boolean isObservacaoSomenteDivergenciaIgnoravel(String observacao) {
        String texto = trimToEmpty(observacao).toUpperCase(Locale.ROOT);
        if (texto.isEmpty()) {
            return false;
        }

        boolean temNcm = texto.contains(TEXTO_ERRO_NCM);
        boolean temCest = texto.contains(TEXTO_ERRO_CEST);
        boolean temOrigem = texto.contains(TEXTO_ERRO_ORIGEM);
        boolean temFci = texto.contains(TEXTO_ERRO_FCI);

        return (temNcm || temCest) && !temOrigem && !temFci;
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
                LOGGER.log(Level.WARNING, "[LIB2101] Nao foi possivel obter VO do evento.", e);
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

    private BigDecimal normalizarSequencia(BigDecimal sequencia) {
        return sequencia == null ? BigDecimal.ZERO : sequencia;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
