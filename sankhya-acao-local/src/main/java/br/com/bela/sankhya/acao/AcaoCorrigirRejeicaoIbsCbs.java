package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Botao para corrigir rejeicao 1076 de IBS/CBS.
 * Ajusta a base de IBS/CBS quando o frete foi refletido indevidamente na base do item.
 */
public class AcaoCorrigirRejeicaoIbsCbs implements AcaoRotinaJava {

    private static final String VERSAO = "V5 2026-06-16";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();
        if (linhas == null || linhas.length == 0) {
            contexto.mostraErro("Selecione ao menos uma nota.");
            return;
        }

        Set<BigDecimal> notas = coletarNotasSelecionadas(linhas, contexto);
        if (notas.isEmpty()) {
            contexto.mostraErro("Nao foi possivel identificar a NUNOTA das notas selecionadas.");
            return;
        }

        QueryExecutor query = contexto.getQuery();
        int processadas = 0;
        int ajustadas = 0;
        StringBuilder detalhe = new StringBuilder();

        try {
            for (BigDecimal nunota : notas) {
                validarNota(query, nunota, contexto);

                DiagnosticoNota antes = diagnosticarNota(query, nunota);
                boolean houveAjuste = corrigirNota(query, nunota, antes);
                recalcularValoresItem(query, nunota, houveAjuste);
                sincronizarCamposReformaTributaria(query, nunota);
                DiagnosticoNota depois = diagnosticarNota(query, nunota);

                if (houveAjuste) {
                    ajustadas++;
                }

                if (detalhe.length() > 0) {
                    detalhe.append(" | ");
                }
                detalhe.append("NUNOTA ").append(nunota.toPlainString())
                        .append(": base item ").append(formatar(antes.somaBaseItens))
                        .append(" -> ").append(formatar(depois.somaBaseItens))
                        .append(", frete ").append(formatar(antes.valorFrete));

                processadas++;
            }
        } catch (Exception e) {
            contexto.mostraErro("[" + VERSAO + "] Falha ao recalcular IBS/CBS da nota. Detalhe: " + limparMensagem(e));
            return;
        } finally {
            if (query != null) {
                query.close();
            }
        }

        contexto.setMensagemRetorno("[" + VERSAO + "] Reprocesso IBS/CBS executado em " + processadas
                + " nota(s). Ajustadas: " + ajustadas + ". " + detalhe.toString());
    }

    private Set<BigDecimal> coletarNotasSelecionadas(Registro[] linhas, ContextoAcao contexto) throws Exception {
        Set<BigDecimal> notas = new LinkedHashSet<BigDecimal>();

        for (Registro linha : linhas) {
            BigDecimal nunota = asBigDecimal(linha.getCampo("NUNOTA"));
            if (nunota == null) {
                contexto.mostraErro("Nao foi possivel identificar a NUNOTA de uma das linhas selecionadas.");
                return notas;
            }
            notas.add(nunota);
        }

        return notas;
    }

    private void validarNota(QueryExecutor query, BigDecimal nunota, ContextoAcao contexto) throws Exception {
        query.nativeSelect("SELECT TIPMOV, STATUSNOTA FROM TGFCAB WHERE NUNOTA = " + nunota.toPlainString());
        if (!query.next()) {
            contexto.mostraErro("[" + VERSAO + "] Nao foi possivel localizar a nota " + nunota.toPlainString() + ".");
            return;
        }

        String tipmov = trimToEmpty(query.getString("TIPMOV"));
        String status = trimToEmpty(query.getString("STATUSNOTA"));

        if (!"V".equalsIgnoreCase(tipmov)) {
            contexto.mostraErro("[" + VERSAO + "] A nota " + nunota.toPlainString()
                    + " nao e uma nota de venda. TIPMOV atual: " + valorOuVazio(tipmov) + ".");
        }

        if (!"L".equalsIgnoreCase(status)) {
            contexto.mostraErro("[" + VERSAO + "] A nota " + nunota.toPlainString()
                    + " precisa estar confirmada. STATUS atual: " + valorOuVazio(status) + ".");
        }
    }

    private DiagnosticoNota diagnosticarNota(QueryExecutor query, BigDecimal nunota) throws Exception {
        DiagnosticoNota diagnostico = new DiagnosticoNota();

        String sql = "SELECT "
                + "NVL((SELECT SUM(NVL(ITE.VITEM_IBSCBS, 0)) FROM TGFITE ITE "
                + "WHERE ITE.NUNOTA = CAB.NUNOTA AND ITE.SEQUENCIA > 0), 0) AS SOMA_BASE_ITENS, "
                + "NVL((SELECT SUM(NVL(ITE.VLRTOT, 0)) FROM TGFITE ITE "
                + "WHERE ITE.NUNOTA = CAB.NUNOTA AND ITE.SEQUENCIA > 0), 0) AS SOMA_VLRTOT, "
                + "NVL(CAB.VLRFRETE, 0) AS VLRFRETE, "
                + "NVL((SELECT SUM(NVL(DIN.BASE, 0)) FROM TGFDIN DIN "
                + "WHERE DIN.NUNOTA = CAB.NUNOTA AND DIN.SEQUENCIA > 0 AND DIN.CODINC = 3 "
                + "AND DIN.CODIMP IN (12, 13, 14, 15, 16, 17)), 0) AS BASE_FRETE_RT, "
                + "NVL((SELECT COUNT(1) FROM TGFITE ITE "
                + "WHERE ITE.NUNOTA = CAB.NUNOTA AND ITE.SEQUENCIA > 0 "
                + "AND NVL(ITE.VITEM_IBSCBS, 0) > NVL(ITE.VLRTOT, 0)), 0) AS ITENS_EXCEDIDOS "
                + "FROM TGFCAB CAB "
                + "WHERE CAB.NUNOTA = " + nunota.toPlainString();

        query.nativeSelect(sql);
        if (!query.next()) {
            return diagnostico;
        }

        diagnostico.somaBaseItens = nvl(query.getBigDecimal("SOMA_BASE_ITENS"));
        diagnostico.somaVlrTot = nvl(query.getBigDecimal("SOMA_VLRTOT"));
        diagnostico.valorFrete = nvl(query.getBigDecimal("VLRFRETE"));
        diagnostico.baseFreteRt = nvl(query.getBigDecimal("BASE_FRETE_RT"));
        diagnostico.itensExcedidos = query.getBigDecimal("ITENS_EXCEDIDOS");
        return diagnostico;
    }

    private boolean corrigirNota(QueryExecutor query, BigDecimal nunota, DiagnosticoNota antes) throws Exception {
        if (!precisaAjuste(antes)) {
            return false;
        }

        query.update("UPDATE TGFDIN "
                + "SET BASE = 0, ALIQUOTA = 0, VALOR = 0 "
                + "WHERE NUNOTA = " + nunota.toPlainString() + " "
                + "AND SEQUENCIA > 0 "
                + "AND CODINC = 3 "
                + "AND CODIMP IN (12, 13, 14, 15, 16, 17) "
                + "AND NVL(BASE, 0) <> 0");

        query.update("UPDATE TGFITE "
                + "SET VITEM_IBSCBS = NVL(VLRTOT, 0) "
                + "WHERE NUNOTA = " + nunota.toPlainString() + " "
                + "AND SEQUENCIA > 0 "
                + "AND NVL(VITEM_IBSCBS, 0) > NVL(VLRTOT, 0)");

        return true;
    }

    private void recalcularValoresItem(QueryExecutor query, BigDecimal nunota, boolean houveAjuste) throws Exception {
        if (!houveAjuste) {
            return;
        }

        query.update("BEGIN STP_CALCULAVITEM_IBS_CBS(" + nunota.toPlainString() + ", 0); END;");
    }

    private void sincronizarCamposReformaTributaria(QueryExecutor query, BigDecimal nunota) throws Exception {
        query.update("UPDATE TGFDIN "
                + "SET VLRIBSUFREG = NVL(VALOR, 0), "
                + "PALIQIBSUFREG = NVL(ALIQUOTA, 0) "
                + "WHERE NUNOTA = " + nunota.toPlainString() + " "
                + "AND CODIMP = 12");

        query.update("UPDATE TGFDIN "
                + "SET VLRIBSMUNREG = NVL(VALOR, 0), "
                + "PALIQIBSMUNREG = NVL(ALIQUOTA, 0) "
                + "WHERE NUNOTA = " + nunota.toPlainString() + " "
                + "AND CODIMP = 13");

        query.update("UPDATE TGFDIN "
                + "SET VLRCBSREG = NVL(VALOR, 0), "
                + "PALIQCBSREG = NVL(ALIQUOTA, 0) "
                + "WHERE NUNOTA = " + nunota.toPlainString() + " "
                + "AND CODIMP = 14");
    }

    private boolean precisaAjuste(DiagnosticoNota antes) {
        return antes != null
                && antes.valorFrete.compareTo(ZERO) > 0
                && (antes.baseFreteRt.compareTo(ZERO) > 0
                || antes.somaBaseItens.compareTo(antes.somaVlrTot) > 0
                || nvl(antes.itensExcedidos).compareTo(ZERO) > 0);
    }

    private BigDecimal asBigDecimal(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }
        if (valor instanceof Number) {
            return BigDecimal.valueOf(((Number) valor).longValue());
        }

        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        return new BigDecimal(texto);
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor == null ? ZERO : valor;
    }

    private String trimToEmpty(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String valorOuVazio(String valor) {
        return valor == null || valor.trim().isEmpty() ? "<vazio>" : valor;
    }

    private String formatar(BigDecimal valor) {
        return nvl(valor).toPlainString();
    }

    private String limparMensagem(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            msg = e.getClass().getSimpleName();
        }
        return msg.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static final class DiagnosticoNota {
        private BigDecimal somaBaseItens = ZERO;
        private BigDecimal somaVlrTot = ZERO;
        private BigDecimal valorFrete = ZERO;
        private BigDecimal baseFreteRt = ZERO;
        private BigDecimal itensExcedidos = ZERO;
    }
}
