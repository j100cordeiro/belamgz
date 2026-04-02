package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

import java.math.BigDecimal;

/**
 * Botao unico para gerar a entrada aceitando origem em 30100 ou 20100 e
 * permitindo escolher o local de destino 10100 ou 20100.
 */
public class AcaoGeraEntradaDevolucaoSelecionavel implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        BigDecimal localDestino = resolverLocalDestino(contexto);
        AcaoGeraEntradaDevolucaoRma20100Support.executar(contexto, localDestino);
    }

    private BigDecimal resolverLocalDestino(ContextoAcao contexto) {
        Object valor = primeiroValorInformado(
                contexto.getParam("CODLOCALDESTINO"),
                contexto.getParam("Local Destino"),
                contexto.getParam("Local destino"),
                contexto.getParam("localDestino"));

        return AcaoAlteraLocalDevolucaoSupport.asBigDecimal(valor);
    }

    private Object primeiroValorInformado(Object... valores) {
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
}
