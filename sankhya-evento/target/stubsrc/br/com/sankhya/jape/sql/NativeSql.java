package br.com.sankhya.jape.sql;

import java.sql.ResultSet;
import br.com.sankhya.jape.dao.JdbcWrapper;

public class NativeSql {
    public NativeSql(JdbcWrapper jdbc) {
    }

    public void resetSqlBuf() {
    }

    public NativeSql appendSql(String sql) {
        return this;
    }

    public void setNamedParameter(String name, Object value) {
    }

    public ResultSet executeQuery() {
        return null;
    }

    public boolean executeUpdate() {
        return false;
    }

    public static void releaseResources(NativeSql sql) {
    }
}
