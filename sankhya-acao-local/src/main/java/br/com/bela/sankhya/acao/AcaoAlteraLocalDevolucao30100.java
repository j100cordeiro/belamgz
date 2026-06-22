package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

/**
 * Etapa 1 do fluxo WMS: move a devolucao para a triagem 30100.
 */
public class AcaoAlteraLocalDevolucao30100 implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        AcaoAlteraLocalDevolucaoSupport.executarTriagem(contexto);
    }
}
