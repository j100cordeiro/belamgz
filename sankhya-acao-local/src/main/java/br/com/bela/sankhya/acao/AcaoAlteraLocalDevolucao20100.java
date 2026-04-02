package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

import java.math.BigDecimal;

public class AcaoAlteraLocalDevolucao20100 implements AcaoRotinaJava {

    private static final BigDecimal LOCAL_20100 = new BigDecimal("20100");

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        AcaoAlteraLocalDevolucaoSupport.executar(contexto, LOCAL_20100);
    }
}
