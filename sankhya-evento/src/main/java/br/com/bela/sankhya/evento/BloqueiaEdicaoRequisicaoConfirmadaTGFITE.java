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
 * Bloqueia alteracoes sensiveis em itens de requisicao confirmada.
 *
 * Regra herdada da trigger antiga da Biologistica:
 * - cabecalho com TIPMOV = 'J' e STATUSNOTA = 'L';
 * - bloqueia exclusao de item;
 * - bloqueia update se alterar qtd, preco, desconto, produto, localidade ou controle;
 * - mantem livres os campos operacionais de entrega, pois nao entram na lista sensivel.
 */
public class BloqueiaEdicaoRequisicaoConfirmadaTGFITE extends AbstractEventoProgramavel implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger.getLogger(BloqueiaEdicaoRequisicaoConfirmadaTGFITE.class.getName());

    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_SEQUENCIA = "SEQUENCIA";
    private static final String FIELD_QTDNEG = "QTDNEG";
    private static final String FIELD_VLRUNIT = "VLRUNIT";
    private static final String FIELD_VLRTOT = "VLRTOT";
    private static final String FIELD_VLRDESC = "VLRDESC";
    private static final String FIELD_PERCDESC = "PERCDESC";
    private static final String FIELD_CODPROD = "CODPROD";
    private static final String FIELD_CODLOCALORIG = "CODLOCALORIG";
    private static final String FIELD_CONTROLE = "CONTROLE";

    private static final String TIPMOV_REQUISICAO_PEDIDO = "J";
    private static final String STATUS_CONFIRMADA = "L";

    private static final int USUARIO_LIBERADO = 44;

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        validarAlteracao(event, false);
    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {
        validarAlteracao(event, true);
    }

    private void validarAlteracao(PersistenceEvent event, boolean deleting) throws Exception {
        Object vo = extrairVoCompat(event);
        if (vo == null) {
            return;
        }

        BigDecimal nunota = getBigDecimal(vo, FIELD_NUNOTA);
        BigDecimal sequencia = getBigDecimal(vo, FIELD_SEQUENCIA);
        if (nunota == null || sequencia == null) {
            LOGGER.fine("[REQ-BLOQ] NUNOTA/SEQUENCIA ausentes no evento da TGFITE.");
            return;
        }

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            ContextoItem contexto = buscarContexto(sql, nunota, sequencia);
            if (contexto == null || !isRequisicaoConfirmada(contexto)) {
                return;
            }

            if (isUsuarioLiberado(contexto.codusuCab)) {
                return;
            }

            if (deleting) {
                throw new Exception("Requisicao confirmada. Nao e permitido excluir itens apos a confirmacao.");
            }

            if (houveEdicaoSensivel(vo, contexto)) {
                throw new Exception("Requisicao confirmada. Nao e permitido alterar item "
                        + "(quantidade, preco, desconto, produto ou controle) apos a confirmacao.");
            }
        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private ContextoItem buscarContexto(NativeSql sql, BigDecimal nunota, BigDecimal sequencia) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT CAB.TIPMOV, CAB.STATUSNOTA, CAB.CODUSU, ");
            sql.appendSql("       ITE.QTDNEG, ITE.VLRUNIT, ITE.VLRTOT, ITE.VLRDESC, ITE.PERCDESC, ");
            sql.appendSql("       ITE.CODPROD, ITE.CODLOCALORIG, ITE.CONTROLE ");
            sql.appendSql("  FROM TGFITE ITE ");
            sql.appendSql("  JOIN TGFCAB CAB ON CAB.NUNOTA = ITE.NUNOTA ");
            sql.appendSql(" WHERE ITE.NUNOTA = :NUNOTA ");
            sql.appendSql("   AND ITE.SEQUENCIA = :SEQUENCIA");
            sql.setNamedParameter("NUNOTA", nunota);
            sql.setNamedParameter("SEQUENCIA", sequencia);
            rs = sql.executeQuery();
            if (!rs.next()) {
                return null;
            }

            ContextoItem contexto = new ContextoItem();
            contexto.tipmov = rs.getString("TIPMOV");
            contexto.statusnota = rs.getString("STATUSNOTA");
            contexto.codusuCab = rs.getBigDecimal("CODUSU");
            contexto.qtdneg = rs.getBigDecimal("QTDNEG");
            contexto.vlrunit = rs.getBigDecimal("VLRUNIT");
            contexto.vlrtot = rs.getBigDecimal("VLRTOT");
            contexto.vlrdesc = rs.getBigDecimal("VLRDESC");
            contexto.percdesc = rs.getBigDecimal("PERCDESC");
            contexto.codprod = rs.getBigDecimal("CODPROD");
            contexto.codlocalorig = rs.getBigDecimal("CODLOCALORIG");
            contexto.controle = rs.getString("CONTROLE");
            return contexto;
        } finally {
            closeQuietly(rs);
        }
    }

    private boolean houveEdicaoSensivel(Object vo, ContextoItem atual) {
        return !bigDecimalsIguais(getBigDecimal(vo, FIELD_QTDNEG), atual.qtdneg)
                || !bigDecimalsIguais(getBigDecimal(vo, FIELD_VLRUNIT), atual.vlrunit)
                || !bigDecimalsIguais(getBigDecimal(vo, FIELD_VLRTOT), atual.vlrtot)
                || !bigDecimalsIguais(getBigDecimal(vo, FIELD_VLRDESC), atual.vlrdesc)
                || !bigDecimalsIguais(getBigDecimal(vo, FIELD_PERCDESC), atual.percdesc)
                || !bigDecimalsIguais(getBigDecimal(vo, FIELD_CODPROD), atual.codprod)
                || !bigDecimalsIguais(getBigDecimal(vo, FIELD_CODLOCALORIG), atual.codlocalorig)
                || !stringsIguais(getString(vo, FIELD_CONTROLE), atual.controle);
    }

    private boolean isRequisicaoConfirmada(ContextoItem contexto) {
        return TIPMOV_REQUISICAO_PEDIDO.equalsIgnoreCase(trimToEmpty(contexto.tipmov))
                && STATUS_CONFIRMADA.equalsIgnoreCase(trimToEmpty(contexto.statusnota));
    }

    private boolean isUsuarioLiberado(BigDecimal codusuCab) {
        if (codusuCab == null) {
            return false;
        }
        int codusu = codusuCab.intValue();
        return codusu == USUARIO_LIBERADO;
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
                LOGGER.log(Level.WARNING, "[REQ-BLOQ] Nao foi possivel obter VO do evento.", e);
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

    private boolean stringsIguais(String a, String b) {
        return trimToEmpty(a).equals(trimToEmpty(b));
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

    private static final class ContextoItem {
        private String tipmov;
        private String statusnota;
        private BigDecimal codusuCab;
        private BigDecimal qtdneg;
        private BigDecimal vlrunit;
        private BigDecimal vlrtot;
        private BigDecimal vlrdesc;
        private BigDecimal percdesc;
        private BigDecimal codprod;
        private BigDecimal codlocalorig;
        private String controle;
    }
}
