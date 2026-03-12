package br.com.bela.sankhya.evento;

import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;

public abstract class AbstractEventoProgramavel {

    public void beforeInsert(PersistenceEvent event) throws Exception {
        // Default no-op
    }

    public void afterInsert(PersistenceEvent event) throws Exception {
        // Default no-op
    }

    public void beforeUpdate(PersistenceEvent event) throws Exception {
        // Default no-op
    }

    public void afterUpdate(PersistenceEvent event) throws Exception {
        // Default no-op
    }

    public void beforeDelete(PersistenceEvent event) throws Exception {
        // Default no-op
    }

    public void afterDelete(PersistenceEvent event) throws Exception {
        // Default no-op
    }

    public void beforeCommit(TransactionContext ctx) throws Exception {
        // Default no-op
    }
}
