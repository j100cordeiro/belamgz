package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

import java.math.BigDecimal;

/**
 * Variante tolerante a configuracoes de parametro salvas incorretamente no
 * cadastro da acao. Aceita o valor do destino em chaves diferentes.
 */
public class AcaoGeraEntradaDevolucaoDestinoFlexivel implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        BigDecimal localDestino = resolverLocalDestino(contexto);

        if (localDestino == null) {
            contexto.mostraErro("Informe o campo Local Destino com valor 10100 ou 20100.");
            return;
        }

        AcaoGeraEntradaDevolucaoRma20100Support.executar(contexto, localDestino);
    }

    private BigDecimal resolverLocalDestino(ContextoAcao contexto) {
        Object valor = primeiroValorInformado(
                contexto.getParam("CODLOCALDESTINO"),
                contexto.getParam("CODLOCALORIG"),
                contexto.getParam("Local Destino"),
                contexto.getParam("Local DEstino:"),
                contexto.getParam("Local destino"),
                contexto.getParam("localDestino"));

        return asBigDecimal(valor);
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

    private BigDecimal asBigDecimal(Object valor) {
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
}
