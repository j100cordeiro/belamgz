package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class AcaoAlteraLocalDevolucaoSupport {

    private static final Set<BigDecimal> LOCAIS_PERMITIDOS = Collections.unmodifiableSet(
            new LinkedHashSet<BigDecimal>(Arrays.asList(new BigDecimal("10100"), new BigDecimal("20100"))));

    private AcaoAlteraLocalDevolucaoSupport() {
    }

    static BigDecimal obterLocalDestino(ContextoAcao contexto) {
        Object valor = primeiroValorInformado(
                contexto.getParam("CODLOCALDESTINO"),
                contexto.getParam("Local Destino"),
                contexto.getParam("Local destino"),
                contexto.getParam("localDestino"));

        return asBigDecimal(valor);
    }

    static void executar(ContextoAcao contexto, BigDecimal localDestino) throws Exception {
        Registro[] linhas = contexto.getLinhas();

        if (linhas == null || linhas.length == 0) {
            contexto.mostraErro("Selecione ao menos uma nota para alterar o local.");
            return;
        }

        validarLocalDestino(localDestino, contexto);

        Set<BigDecimal> notas = coletarNotasSelecionadas(linhas, contexto);
        QueryExecutor query = contexto.getQuery();

        try {
            for (BigDecimal nunota : notas) {
                query.update("UPDATE TGFITE SET CODLOCALORIG = " + localDestino.toPlainString()
                        + " WHERE NUNOTA = " + nunota.toPlainString());
            }
        } finally {
            if (query != null) {
                query.close();
            }
        }

        contexto.setMensagemRetorno("Local alterado para " + localDestino.toPlainString() + " em "
                + notas.size() + " nota(s).");
    }

    static BigDecimal asBigDecimal(Object valor) {
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
        if (texto.isEmpty()) {
            return null;
        }

        return new BigDecimal(texto);
    }

    private static Object primeiroValorInformado(Object... valores) {
        if (valores == null) {
            return null;
        }

        for (Object valor : valores) {
            if (valor == null) {
                continue;
            }

            if (valor instanceof String && ((String) valor).trim().isEmpty()) {
                continue;
            }

            return valor;
        }

        return null;
    }

    private static Set<BigDecimal> coletarNotasSelecionadas(Registro[] linhas, ContextoAcao contexto) throws Exception {
        Set<BigDecimal> notas = new LinkedHashSet<BigDecimal>();

        for (Registro linha : linhas) {
            BigDecimal nunota = asBigDecimal(linha.getCampo("NUNOTA"));

            if (nunota == null) {
                contexto.mostraErro("Nao foi possivel identificar a NUNOTA de uma das linhas selecionadas.");
                return Collections.emptySet();
            }

            notas.add(nunota);
        }

        return notas;
    }

    private static void validarLocalDestino(BigDecimal localDestino, ContextoAcao contexto) throws Exception {
        if (localDestino == null) {
            contexto.mostraErro("Informe o parametro CODLOCALDESTINO com valor 10100 ou 20100.");
            return;
        }

        if (!LOCAIS_PERMITIDOS.contains(localDestino)) {
            contexto.mostraErro("Local invalido: " + localDestino.toPlainString()
                    + ". Valores permitidos: 10100 ou 20100.");
        }
    }
}
