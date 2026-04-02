package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

/**
 * Permite reaproveitar a rotina da BHZ mesmo quando a devolucao ainda esta no
 * local 20100. Se nao houver item em triagem (30100), move os itens positivos
 * de 20100 para 30100 e delega para a rotina original.
 */
public class AcaoGeraEntradaDevolucaoRma20100 implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        AcaoGeraEntradaDevolucaoRma20100Support.executar(contexto);
    }
}
