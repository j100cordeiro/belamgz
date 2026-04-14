package br.com.sankhya.modelcore.util;

import br.com.sankhya.jape.EntityFacade;

public class EntityFacadeFactory {
    public static EntityFacade getDWFFacade() {
        return new EntityFacade();
    }
}
