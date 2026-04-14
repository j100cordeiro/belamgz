package br.com.sankhya.jape;

import br.com.sankhya.jape.dao.JdbcWrapper;

public class EntityFacade {
    public JdbcWrapper getJdbcWrapper() {
        return new JdbcWrapper();
    }
}
