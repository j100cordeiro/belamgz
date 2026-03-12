package br.com.bela.sankhya.evento;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

/**
 * Vincula automaticamente pedido marketplace x faturamento processado via TGFIXN.
 *
 * Fluxo:
 * 1) Executa apos INSERT/UPDATE de TGFCAB (faturamento).
 * 2) Busca TGFIXN da NUNOTA (STATUS=5) e extrai xPed do XML.
 * 3) Localiza o pedido (TIPMOV='P') por campos de marketplace.
 * 4) Atualiza TGFIXN.AD_NUNOTAORIG e cria relacionamento item-a-item na TGFVAR.
 */
public class VinculaPedidoMarketplaceXmlTGFCAB extends AbstractEventoProgramavel implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger.getLogger(VinculaPedidoMarketplaceXmlTGFCAB.class.getName());

    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_TIPMOV = "TIPMOV";
    private static final String FIELD_CODPARC = "CODPARC";
    private static final String FIELD_CODEMP = "CODEMP";
    private static final String FIELD_VLRNOTA = "VLRNOTA";

    private static final int STATUS_XML_PROCESSADO = 5;
    private static final int JANELA_DIAS_FALLBACK = 15;

    // Campos comuns de marketplace ja vistos em ambientes Sankhya.
    private static final List<String> CANDIDATE_MARKETPLACE_COLUMNS = Arrays.asList(
            "AD_ML_ORDERID_BASE",
            "AD_ML_ORDERID",
            "AD_ANYMKTID_ORDER",
            "AD_PEDIDOMKTPLACE",
            "AD_CODMKT",
            "AD_CODEMKT",
            "BH_CODMKT",
            "BH_CODEMKT");

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        processarComSeguranca(event, "afterInsert");
    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {
        processarComSeguranca(event, "afterUpdate");
    }

    private void processarComSeguranca(PersistenceEvent event, String origemEvento) {
        try {
            processar(event, origemEvento);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[VINCXML] Falha no evento " + origemEvento, e);
        }
    }

    private void processar(PersistenceEvent event, String origemEvento) throws Exception {
        DynamicVO cab = (DynamicVO) event.getVo();
        BigDecimal nunotaFat = getBigDecimal(cab, FIELD_NUNOTA);
        String tipmov = getString(cab, FIELD_TIPMOV);

        if (nunotaFat == null) return;
        if (!"V".equalsIgnoreCase(tipmov)) return;

        BigDecimal codParc = getBigDecimal(cab, FIELD_CODPARC);
        BigDecimal codEmp = getBigDecimal(cab, FIELD_CODEMP);
        BigDecimal vlrNota = getBigDecimal(cab, FIELD_VLRNOTA);

        EntityFacade facade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = facade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        try {
            jdbc.openSession();

            XmlInfo xmlInfo = buscarXmlInfo(sql, nunotaFat);
            if (xmlInfo == null) return;

            BigDecimal nunotaPedido = xmlInfo.nunotaOrigem;
            String xped = normalizarIdMarketplace(xmlInfo.xped);

            if (nunotaPedido == null) {
                nunotaPedido = localizarPedidoPorXPed(sql, xped, codEmp, codParc, vlrNota);
            }

            if (nunotaPedido == null) {
                nunotaPedido = localizarPedidoPorFallback(sql, nunotaFat, codEmp, codParc, vlrNota);
            }

            if (nunotaPedido == null) {
                registrarLogXml(sql, xmlInfo.nuarquivo,
                        "Nao foi possivel localizar pedido para NUNOTA FAT=" + nunotaFat
                                + ", XPED=" + safe(xped)
                                + ", evento=" + origemEvento);
                return;
            }

            atualizarNunotaOrigemNoXml(sql, xmlInfo.nuarquivo, nunotaPedido);
            boolean executouInsercaoTgfvar = inserirVinculosTgfvar(sql, nunotaFat, nunotaPedido);

            if (!executouInsercaoTgfvar && !existeVinculoTgfvar(sql, nunotaFat, nunotaPedido)) {
                registrarLogXml(sql, xmlInfo.nuarquivo,
                        "Pedido localizado, mas nao houve mapeamento de itens em TGFVAR. FAT="
                                + nunotaFat + ", PED=" + nunotaPedido);
                return;
            }

            registrarLogXml(sql, xmlInfo.nuarquivo,
                    "Vinculo OK. FAT=" + nunotaFat
                            + ", PED=" + nunotaPedido
                            + ", XPED=" + safe(xped)
                            + ", tgfvarAtualizado=" + executouInsercaoTgfvar
                            + ", evento=" + origemEvento);

        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    private XmlInfo buscarXmlInfo(NativeSql sql, BigDecimal nunotaFat) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT * FROM (");
            sql.appendSql("  SELECT i.NUARQUIVO, i.AD_NUNOTAORIG, ");
            sql.appendSql("         REGEXP_SUBSTR(DBMS_LOB.SUBSTR(i.XML, 4000, 1), '<xPed>([^<]+)</xPed>', 1, 1, NULL, 1) AS XPED ");
            sql.appendSql("    FROM TGFIXN i ");
            sql.appendSql("   WHERE i.NUNOTA = :NUNOTA ");
            sql.appendSql("     AND i.STATUS = :STATUS ");
            sql.appendSql("   ORDER BY i.DHPROCESS DESC NULLS LAST, i.NUARQUIVO DESC ");
            sql.appendSql(") WHERE ROWNUM = 1 ");

            sql.setNamedParameter("NUNOTA", nunotaFat);
            sql.setNamedParameter("STATUS", new BigDecimal(STATUS_XML_PROCESSADO));
            rs = sql.executeQuery();

            if (!rs.next()) return null;

            XmlInfo info = new XmlInfo();
            info.nuarquivo = rs.getBigDecimal("NUARQUIVO");
            info.nunotaOrigem = rs.getBigDecimal("AD_NUNOTAORIG");
            info.xped = rs.getString("XPED");
            return info;
        } finally {
            closeQuietly(rs);
        }
    }

    private BigDecimal localizarPedidoPorXPed(NativeSql sql, String xped, BigDecimal codEmp, BigDecimal codParc,
                                              BigDecimal vlrNota) throws Exception {
        if (xped == null) return null;

        Set<String> colunasExistentes = buscarColunasExistentes(sql, CANDIDATE_MARKETPLACE_COLUMNS);
        if (colunasExistentes.isEmpty()) return null;

        StringBuilder or = new StringBuilder();
        for (String col : colunasExistentes) {
            if (or.length() > 0) or.append(" OR ");
            String digits = "REGEXP_REPLACE(TO_CHAR(c." + col + "), '[^0-9]', '')";
            or.append("(")
                    .append(digits).append(" = :XPED ")
                    .append(" OR (LENGTH(").append(digits).append(") = LENGTH(:XPED) + 1 ")
                    .append("     AND SUBSTR(").append(digits).append(", 1, LENGTH(").append(digits).append(") - 1) = :XPED)")
                    .append(")");
        }

        sql.resetSqlBuf();
        sql.appendSql("SELECT c.NUNOTA ");
        sql.appendSql("  FROM TGFCAB c ");
        sql.appendSql(" WHERE c.TIPMOV = 'P' ");
        if (codEmp != null) {
            sql.appendSql("   AND c.CODEMP = :CODEMP ");
        }
        if (codParc != null) {
            sql.appendSql("   AND c.CODPARC = :CODPARC ");
        }
        if (vlrNota != null) {
            sql.appendSql("   AND ABS(NVL(c.VLRNOTA, 0) - :VLRNOTA) <= 0.01 ");
        }
        sql.appendSql("   AND (").appendSql(or.toString()).appendSql(") ");
        sql.appendSql(" ORDER BY c.DTNEG DESC, c.NUNOTA DESC ");

        sql.setNamedParameter("XPED", xped);
        if (codEmp != null) sql.setNamedParameter("CODEMP", codEmp);
        if (codParc != null) sql.setNamedParameter("CODPARC", codParc);
        if (vlrNota != null) sql.setNamedParameter("VLRNOTA", vlrNota);

        return obterUnicoOuNulo(sql, "NUNOTA");
    }

    private BigDecimal localizarPedidoPorFallback(NativeSql sql, BigDecimal nunotaFat, BigDecimal codEmp,
                                                  BigDecimal codParc, BigDecimal vlrNota) throws Exception {
        sql.resetSqlBuf();
        sql.appendSql("SELECT c.NUNOTA ");
        sql.appendSql("  FROM TGFCAB c ");
        sql.appendSql(" WHERE c.TIPMOV = 'P' ");
        sql.appendSql("   AND c.DTNEG >= TRUNC(SYSDATE) - :DIAS ");
        if (codEmp != null) {
            sql.appendSql("   AND c.CODEMP = :CODEMP ");
        }
        if (codParc != null) {
            sql.appendSql("   AND c.CODPARC = :CODPARC ");
        }
        if (vlrNota != null) {
            sql.appendSql("   AND ABS(NVL(c.VLRNOTA, 0) - :VLRNOTA) <= 0.01 ");
        }
        sql.appendSql("   AND EXISTS ( ");
        sql.appendSql("         SELECT 1 ");
        sql.appendSql("           FROM TGFITE ped ");
        sql.appendSql("          WHERE ped.NUNOTA = c.NUNOTA ");
        sql.appendSql("            AND EXISTS ( ");
        sql.appendSql("                  SELECT 1 ");
        sql.appendSql("                    FROM TGFITE nf ");
        sql.appendSql("                   WHERE nf.NUNOTA = :NUNOTAFAT ");
        sql.appendSql("                     AND nf.CODPROD = ped.CODPROD ");
        sql.appendSql("            ) ");
        sql.appendSql("   ) ");
        sql.appendSql(" ORDER BY c.DTNEG DESC, c.NUNOTA DESC ");

        sql.setNamedParameter("DIAS", new BigDecimal(JANELA_DIAS_FALLBACK));
        sql.setNamedParameter("NUNOTAFAT", nunotaFat);
        if (codEmp != null) sql.setNamedParameter("CODEMP", codEmp);
        if (codParc != null) sql.setNamedParameter("CODPARC", codParc);
        if (vlrNota != null) sql.setNamedParameter("VLRNOTA", vlrNota);

        return obterUnicoOuNulo(sql, "NUNOTA");
    }

    private boolean inserirVinculosTgfvar(NativeSql sql, BigDecimal nunotaFat, BigDecimal nunotaPedido) throws Exception {
        sql.resetSqlBuf();
        sql.appendSql("INSERT INTO TGFVAR (");
        sql.appendSql("  NUNOTA, SEQUENCIA, NUNOTAORIG, SEQUENCIAORIG, QTDATENDIDA, CUSATEND, STATUSNOTA ");
        sql.appendSql(") ");
        sql.appendSql("SELECT :NUNOTAFAT, nf.SEQUENCIA, :NUNOTAPED, ped.SEQUENCIA, 0, 0, 'L' ");
        sql.appendSql("  FROM ( ");
        sql.appendSql("        SELECT SEQUENCIA, CODPROD, ");
        sql.appendSql("               ROW_NUMBER() OVER (PARTITION BY CODPROD ORDER BY SEQUENCIA) AS RN ");
        sql.appendSql("          FROM TGFITE ");
        sql.appendSql("         WHERE NUNOTA = :NUNOTAFAT ");
        sql.appendSql("  ) nf ");
        sql.appendSql("  JOIN ( ");
        sql.appendSql("        SELECT SEQUENCIA, CODPROD, ");
        sql.appendSql("               ROW_NUMBER() OVER (PARTITION BY CODPROD ORDER BY SEQUENCIA) AS RN ");
        sql.appendSql("          FROM TGFITE ");
        sql.appendSql("         WHERE NUNOTA = :NUNOTAPED ");
        sql.appendSql("  ) ped ");
        sql.appendSql("    ON ped.CODPROD = nf.CODPROD ");
        sql.appendSql("   AND ped.RN = nf.RN ");
        sql.appendSql(" WHERE NOT EXISTS ( ");
        sql.appendSql("       SELECT 1 ");
        sql.appendSql("         FROM TGFVAR v ");
        sql.appendSql("        WHERE v.NUNOTA = :NUNOTAFAT ");
        sql.appendSql("          AND v.SEQUENCIA = nf.SEQUENCIA ");
        sql.appendSql("          AND v.NUNOTAORIG = :NUNOTAPED ");
        sql.appendSql("          AND v.SEQUENCIAORIG = ped.SEQUENCIA ");
        sql.appendSql(" ) ");

        sql.setNamedParameter("NUNOTAFAT", nunotaFat);
        sql.setNamedParameter("NUNOTAPED", nunotaPedido);
        return sql.executeUpdate();
    }

    private boolean existeVinculoTgfvar(NativeSql sql, BigDecimal nunotaFat, BigDecimal nunotaPedido) throws Exception {
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT 1 ");
            sql.appendSql("  FROM TGFVAR v ");
            sql.appendSql(" WHERE v.NUNOTA = :NUNOTAFAT ");
            sql.appendSql("   AND v.NUNOTAORIG = :NUNOTAPED ");
            sql.appendSql("   AND ROWNUM = 1 ");
            sql.setNamedParameter("NUNOTAFAT", nunotaFat);
            sql.setNamedParameter("NUNOTAPED", nunotaPedido);
            rs = sql.executeQuery();
            return rs.next();
        } finally {
            closeQuietly(rs);
        }
    }

    private void atualizarNunotaOrigemNoXml(NativeSql sql, BigDecimal nuarquivo, BigDecimal nunotaPedido) throws Exception {
        if (nuarquivo == null || nunotaPedido == null) return;
        sql.resetSqlBuf();
        sql.appendSql("UPDATE TGFIXN ");
        sql.appendSql("   SET AD_NUNOTAORIG = :NUNOTAPED ");
        sql.appendSql(" WHERE NUARQUIVO = :NUARQ ");
        sql.appendSql("   AND (AD_NUNOTAORIG IS NULL OR AD_NUNOTAORIG <> :NUNOTAPED) ");
        sql.setNamedParameter("NUNOTAPED", nunotaPedido);
        sql.setNamedParameter("NUARQ", nuarquivo);
        sql.executeUpdate();
    }

    private Set<String> buscarColunasExistentes(NativeSql sql, List<String> colunas) throws Exception {
        Set<String> existentes = new LinkedHashSet<String>();
        ResultSet rs = null;
        try {
            sql.resetSqlBuf();
            sql.appendSql("SELECT COLUMN_NAME ");
            sql.appendSql("  FROM ALL_TAB_COLUMNS ");
            sql.appendSql(" WHERE OWNER = 'SANKHYA' ");
            sql.appendSql("   AND TABLE_NAME = 'TGFCAB' ");
            sql.appendSql("   AND COLUMN_NAME IN (");

            for (int i = 0; i < colunas.size(); i++) {
                if (i > 0) sql.appendSql(", ");
                sql.appendSql(":COL").appendSql(String.valueOf(i));
            }
            sql.appendSql(") ");

            for (int i = 0; i < colunas.size(); i++) {
                sql.setNamedParameter("COL" + i, colunas.get(i));
            }

            rs = sql.executeQuery();
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (col != null) existentes.add(col.trim());
            }
            return existentes;
        } finally {
            closeQuietly(rs);
        }
    }

    private BigDecimal obterUnicoOuNulo(NativeSql sql, String fieldName) throws Exception {
        ResultSet rs = null;
        try {
            rs = sql.executeQuery();
            BigDecimal primeiro = null;
            int count = 0;
            while (rs.next()) {
                BigDecimal nunota = rs.getBigDecimal(fieldName);
                if (nunota == null) continue;
                if (count == 0) {
                    primeiro = nunota;
                }
                count++;
                if (count > 1) {
                    return null; // Ambiguo: evita vinculo incorreto.
                }
            }
            return count == 1 ? primeiro : null;
        } finally {
            closeQuietly(rs);
        }
    }

    private void registrarLogXml(NativeSql sql, BigDecimal nuarquivo, String mensagem) {
        if (nuarquivo == null || mensagem == null) return;
        try {
            String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            String msg = "[VINCXML " + stamp + "] " + mensagem;

            sql.resetSqlBuf();
            sql.appendSql("UPDATE TGFIXN ");
            sql.appendSql("   SET DETALHESIMPORTACAO = SUBSTR(");
            sql.appendSql("       NVL(DETALHESIMPORTACAO, '') ");
            sql.appendSql("       || CASE WHEN NVL(DETALHESIMPORTACAO, '') = '' THEN '' ELSE ' | ' END ");
            sql.appendSql("       || :MSG, 1, 4000) ");
            sql.appendSql(" WHERE NUARQUIVO = :NUARQ ");
            sql.setNamedParameter("MSG", msg);
            sql.setNamedParameter("NUARQ", nuarquivo);
            sql.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[VINCXML] Falha ao registrar log no XML NUARQUIVO=" + nuarquivo, e);
        }
    }

    private String normalizarIdMarketplace(String valor) {
        if (valor == null) return null;
        String digits = valor.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private String safe(String txt) {
        return txt == null ? "<null>" : txt;
    }

    private BigDecimal getBigDecimal(DynamicVO vo, String field) {
        try {
            Object value = vo.getProperty(field);
            if (value == null) return null;
            if (value instanceof BigDecimal) return (BigDecimal) value;
            if (value instanceof Number) return new BigDecimal(value.toString());
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(DynamicVO vo, String field) {
        try {
            Object value = vo.getProperty(field);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void closeQuietly(ResultSet rs) {
        if (rs == null) return;
        try {
            rs.close();
        } catch (Exception ignored) {
        }
    }

    private static final class XmlInfo {
        private BigDecimal nuarquivo;
        private BigDecimal nunotaOrigem;
        private String xped;
    }
}
