package br.com.sankhya.extensions.eventoprogramavel;

import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;

public interface EventoProgramavelJava {
    void beforeInsert(PersistenceEvent event) throws Exception;
    void afterInsert(PersistenceEvent event) throws Exception;
    void beforeUpdate(PersistenceEvent event) throws Exception;
    void afterUpdate(PersistenceEvent event) throws Exception;
    void beforeDelete(PersistenceEvent event) throws Exception;
    void afterDelete(PersistenceEvent event) throws Exception;
    void beforeCommit(TransactionContext ctx) throws Exception;
}
