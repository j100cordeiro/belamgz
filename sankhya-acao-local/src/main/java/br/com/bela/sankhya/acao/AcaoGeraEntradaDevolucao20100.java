package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

import java.math.BigDecimal;

/**
 * Gera a entrada usando destino fixo em 20100, sem abrir formulario.
 */
public class AcaoGeraEntradaDevolucao20100 implements AcaoRotinaJava {

    private static final BigDecimal LOCAL_DESTINO = new BigDecimal("20100");

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        AcaoGeraEntradaDevolucaoRma20100Support.executar(contexto, LOCAL_DESTINO);
    }
}
