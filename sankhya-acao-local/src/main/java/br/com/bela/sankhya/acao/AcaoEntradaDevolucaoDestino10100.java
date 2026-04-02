package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

import java.math.BigDecimal;

/**
 * Gera a entrada da devolucao com destino fixo em 10100.
 */
public class AcaoEntradaDevolucaoDestino10100 implements AcaoRotinaJava {

    private static final BigDecimal DESTINO_10100 = new BigDecimal("10100");

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        AcaoEntradaDevolucaoDestinoSupport.executar(contexto, DESTINO_10100);
    }
}
