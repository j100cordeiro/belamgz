package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AcaoEntradaDevolucaoDestinoSupportV2 {

    private static final String VERSAO = "V2 2026-05-20 bf6f1ea";
    private static final BigDecimal LOCAL_TRIAGEM = new BigDecimal("30100");
    private static final BigDecimal LOCAL_10100 = new BigDecimal("10100");
    private static final BigDecimal LOCAL_20100 = new BigDecimal("20100");
    private static final BigDecimal CODPARC_ESTOQUE = BigDecimal.ZERO;
    private static final String TIPO_ESTOQUE = "P";
    private static final String CONTROLE_VAZIO = " ";
    private static final Set<BigDecimal> DESTINOS_PERMITIDOS = Collections.unmodifiableSet(
            new LinkedHashSet<BigDecimal>(Arrays.asList(LOCAL_10100, LOCAL_20100)));

    private AcaoEntradaDevolucaoDestinoSupportV2() {
    }

    static void executar(ContextoAcao contexto, BigDecimal localDestino) throws Exception {
        Registro[] linhas = contexto.getLinhas();
        if (linhas == null || linhas.length == 0) {
            contexto.mostraErro(prefixoErro() + "Selecione ao menos uma nota para alterar o local.");
            return;
        }

        validarDestino(localDestino, contexto);

        Set<BigDecimal> notas = coletarNotasSelecionadas(linhas, contexto);
        QueryExecutor query = contexto.getQuery();
        List<BigDecimal> notasProcessadas = new ArrayList<BigDecimal>();

        try {
            for (BigDecimal nunota : notas) {
                validarNotaOrigem(query, nunota, contexto);

                List<ItemMovimentacao> itens = buscarItensTriagem(query, nunota);
                if (itens.isEmpty()) {
                    contexto.mostraErro(prefixoErro() + "Nao encontrado nenhum item na Triagem (30100) para a nota "
                            + nunota.toPlainString() + ".");
                    return;
                }

                for (ItemMovimentacao item : itens) {
                    validarSaldoOrigem(query, item, contexto, nunota);
                    baixarOrigem(query, item);
                    entrarDestino(query, item, localDestino);
                }

                atualizarItensNota(query, nunota, localDestino);
                limparVinculoGeracao(query, nunota);
                notasProcessadas.add(nunota);
            }
        } finally {
            if (query != null) {
                query.close();
            }
        }

        contexto.setMensagemRetorno("[" + VERSAO + "] Estoque movido de 30100 para "
                + localDestino.toPlainString() + " em " + notasProcessadas.size() + " nota(s).");
    }

    private static void validarDestino(BigDecimal localDestino, ContextoAcao contexto) throws Exception {
        if (localDestino == null || !DESTINOS_PERMITIDOS.contains(localDestino)) {
            contexto.mostraErro(prefixoErro() + "Destino invalido. Use 10100 ou 20100.");
        }
    }

    private static Set<BigDecimal> coletarNotasSelecionadas(Registro[] linhas, ContextoAcao contexto) throws Exception {
        Set<BigDecimal> notas = new LinkedHashSet<BigDecimal>();

        for (Registro linha : linhas) {
            BigDecimal nunota = asBigDecimal(linha.getCampo("NUNOTA"));
            if (nunota == null) {
                contexto.mostraErro(prefixoErro()
                        + "Nao foi possivel identificar a NUNOTA de uma das linhas selecionadas.");
                return Collections.emptySet();
            }
            notas.add(nunota);
        }

        return notas;
    }

    private static void validarNotaOrigem(QueryExecutor query, BigDecimal nunota, ContextoAcao contexto)
            throws Exception {
        query.nativeSelect("SELECT TIPMOV, STATUSNOTA FROM TGFCAB WHERE NUNOTA = " + nunota.toPlainString());
        if (!query.next()) {
            contexto.mostraErro(prefixoErro() + "Nao foi possivel localizar a nota " + nunota.toPlainString() + ".");
            return;
        }

        String tipmov = trimToEmpty(query.getString("TIPMOV"));
        String status = trimToEmpty(query.getString("STATUSNOTA"));

        if (!"D".equalsIgnoreCase(tipmov)) {
            contexto.mostraErro(prefixoErro() + "A nota " + nunota.toPlainString()
                    + " nao e uma devolucao valida para esta acao.");
            return;
        }

        if (!"L".equalsIgnoreCase(status)) {
            contexto.mostraErro(prefixoErro() + "A nota " + nunota.toPlainString()
                    + " precisa estar confirmada para alterar o estoque.");
        }
    }

    private static List<ItemMovimentacao> buscarItensTriagem(QueryExecutor query, BigDecimal nunota) throws Exception {
        List<ItemMovimentacao> itens = new ArrayList<ItemMovimentacao>();

        String sql = "SELECT CAB.CODEMP, ITE.CODPROD, NVL(ITE.CONTROLE, ' ') AS CONTROLE, "
                + "SUM(NVL(ITE.QTDNEG, 0)) AS QTDNEG "
                + "FROM TGFCAB CAB "
                + "JOIN TGFITE ITE ON ITE.NUNOTA = CAB.NUNOTA "
                + "WHERE CAB.NUNOTA = " + nunota.toPlainString() + " "
                + "AND ITE.SEQUENCIA > 0 "
                + "AND ITE.CODLOCALORIG = " + LOCAL_TRIAGEM.toPlainString() + " "
                + "GROUP BY CAB.CODEMP, ITE.CODPROD, NVL(ITE.CONTROLE, ' ')";

        query.nativeSelect(sql);
        while (query.next()) {
            ItemMovimentacao item = new ItemMovimentacao();
            item.codemp = query.getBigDecimal("CODEMP");
            item.codprod = query.getBigDecimal("CODPROD");
            item.controle = trimControle(query.getString("CONTROLE"));
            item.qtdneg = nvl(query.getBigDecimal("QTDNEG"));
            if (item.codemp != null && item.codprod != null && item.qtdneg.compareTo(BigDecimal.ZERO) > 0) {
                itens.add(item);
            }
        }

        return itens;
    }

    private static void validarSaldoOrigem(QueryExecutor query, ItemMovimentacao item, ContextoAcao contexto,
            BigDecimal nunota) throws Exception {
        BigDecimal estoqueOrigem = consultarEstoque(query, item.codemp, item.codprod, LOCAL_TRIAGEM, item.controle);
        if (estoqueOrigem == null) {
            contexto.mostraErro(prefixoErro() + "Sem linha de estoque no 30100 para a nota "
                    + nunota.toPlainString() + ", produto " + item.codprod.toPlainString() + ".");
            return;
        }

        if (estoqueOrigem.compareTo(item.qtdneg) < 0) {
            contexto.mostraErro(prefixoErro() + "Saldo insuficiente no 30100 para a nota "
                    + nunota.toPlainString() + ", produto " + item.codprod.toPlainString()
                    + ". Estoque atual=" + estoqueOrigem.toPlainString()
                    + ", necessario=" + item.qtdneg.toPlainString() + ".");
        }
    }

    private static BigDecimal consultarEstoque(QueryExecutor query, BigDecimal codemp, BigDecimal codprod,
            BigDecimal codlocal, String controle) throws Exception {
        String sql = "SELECT ESTOQUE "
                + "FROM TGFEST "
                + "WHERE CODEMP = " + codemp.toPlainString() + " "
                + "AND CODPROD = " + codprod.toPlainString() + " "
                + "AND CODLOCAL = " + codlocal.toPlainString() + " "
                + "AND CODPARC = " + CODPARC_ESTOQUE.toPlainString() + " "
                + "AND TIPO = " + q(TIPO_ESTOQUE) + " "
                + "AND CONTROLE = " + q(controle);

        query.nativeSelect(sql);
        if (!query.next()) {
            return null;
        }
        return query.getBigDecimal("ESTOQUE");
    }

    private static void baixarOrigem(QueryExecutor query, ItemMovimentacao item) throws Exception {
        String sql = "UPDATE TGFEST "
                + "SET ESTOQUE = NVL(ESTOQUE, 0) - " + item.qtdneg.toPlainString() + " "
                + "WHERE CODEMP = " + item.codemp.toPlainString() + " "
                + "AND CODPROD = " + item.codprod.toPlainString() + " "
                + "AND CODLOCAL = " + LOCAL_TRIAGEM.toPlainString() + " "
                + "AND CODPARC = " + CODPARC_ESTOQUE.toPlainString() + " "
                + "AND TIPO = " + q(TIPO_ESTOQUE) + " "
                + "AND CONTROLE = " + q(item.controle);

        query.update(sql);
    }

    private static void entrarDestino(QueryExecutor query, ItemMovimentacao item, BigDecimal localDestino)
            throws Exception {
        String sql = "MERGE INTO TGFEST DST "
                + "USING (SELECT "
                + item.codemp.toPlainString() + " AS CODEMP, "
                + item.codprod.toPlainString() + " AS CODPROD, "
                + localDestino.toPlainString() + " AS CODLOCAL, "
                + q(item.controle) + " AS CONTROLE, "
                + CODPARC_ESTOQUE.toPlainString() + " AS CODPARC, "
                + q(TIPO_ESTOQUE) + " AS TIPO, "
                + item.qtdneg.toPlainString() + " AS QTD "
                + "FROM DUAL) SRC "
                + "ON (DST.CODEMP = SRC.CODEMP "
                + "AND DST.CODPROD = SRC.CODPROD "
                + "AND DST.CODLOCAL = SRC.CODLOCAL "
                + "AND DST.CONTROLE = SRC.CONTROLE "
                + "AND DST.CODPARC = SRC.CODPARC "
                + "AND DST.TIPO = SRC.TIPO) "
                + "WHEN MATCHED THEN UPDATE SET DST.ESTOQUE = NVL(DST.ESTOQUE, 0) + SRC.QTD "
                + "WHEN NOT MATCHED THEN INSERT "
                + "(CODEMP, CODPROD, CODLOCAL, CONTROLE, CODPARC, TIPO, ESTOQUE, RESERVADO) "
                + "VALUES (SRC.CODEMP, SRC.CODPROD, SRC.CODLOCAL, SRC.CONTROLE, SRC.CODPARC, SRC.TIPO, SRC.QTD, 0)";

        query.update(sql);
    }

    private static void atualizarItensNota(QueryExecutor query, BigDecimal nunota, BigDecimal localDestino)
            throws Exception {
        String sql = "UPDATE TGFITE "
                + "SET CODLOCALORIG = " + localDestino.toPlainString() + " "
                + "WHERE NUNOTA = " + nunota.toPlainString() + " "
                + "AND SEQUENCIA > 0 "
                + "AND CODLOCALORIG = " + LOCAL_TRIAGEM.toPlainString();
        query.update(sql);
    }

    private static void limparVinculoGeracao(QueryExecutor query, BigDecimal nunota) throws Exception {
        query.update("UPDATE TGFCAB SET AD_NUNOTADEV = NULL WHERE NUNOTA = " + nunota.toPlainString());
    }

    private static BigDecimal asBigDecimal(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }
        if (valor instanceof Number) {
            return new BigDecimal(valor.toString());
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : new BigDecimal(texto);
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String trimToEmpty(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String trimControle(String valor) {
        String texto = trimToEmpty(valor);
        return texto.length() == 0 ? CONTROLE_VAZIO : texto;
    }

    private static String q(String valor) {
        return "'" + valor.replace("'", "''") + "'";
    }

    private static String prefixoErro() {
        return "[AcaoEntradaDevolucaoDestinoV2 " + VERSAO + "] ";
    }

    private static final class ItemMovimentacao {
        BigDecimal codemp;
        BigDecimal codprod;
        String controle;
        BigDecimal qtdneg;
    }
}
