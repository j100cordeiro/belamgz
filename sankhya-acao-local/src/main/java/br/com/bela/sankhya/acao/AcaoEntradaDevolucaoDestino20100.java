package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

import java.math.BigDecimal;

/**
 * Gera a entrada da devolucao com destino fixo em 20100.
 */
public class AcaoEntradaDevolucaoDestino20100 implements AcaoRotinaJava {

    private static final BigDecimal DESTINO_20100 = new BigDecimal("20100");

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        AcaoEntradaDevolucaoDestinoSupport.executar(contexto, DESTINO_20100);
    }
}
