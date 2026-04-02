package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

import java.math.BigDecimal;

public class AcaoAlteraLocalDevolucao10100 implements AcaoRotinaJava {

    private static final BigDecimal LOCAL_10100 = new BigDecimal("10100");

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        AcaoAlteraLocalDevolucaoSupport.executar(contexto, LOCAL_10100);
    }
}
