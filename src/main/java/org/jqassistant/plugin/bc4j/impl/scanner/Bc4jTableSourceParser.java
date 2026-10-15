package org.jqassistant.plugin.bc4j.impl.scanner;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.AbstractJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.LinkedHashSet;
import java.util.Set;

/** Extrae tablas de FromList/SQLQuery con JSqlParser y marca los errores de análisis. */
final class Bc4jTableSourceParser {

    private Bc4jTableSourceParser() {
    }

    static TableMappings extract(String fromList, String sqlQuery) {
        String sql = sqlQuery == null || sqlQuery.isBlank() ? selectFrom(fromList) : sqlQuery;
        if (sql == null) {
            return new TableMappings(new String[0], false);
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql,
                    parser -> parser.withDialect(AbstractJSqlParser.Dialect.ORACLE));
            Set<String> tableNames = new LinkedHashSet<>();
            for (String table : new TablesNamesFinder<>().getTables(statement)) {
                String name = qualifiedName(table);
                if (!name.isEmpty()) {
                    tableNames.add(name);
                }
            }
            return new TableMappings(tableNames.toArray(String[]::new), !tableNames.isEmpty());
        } catch (JSQLParserException e) {
            return new TableMappings(new String[0], false);
        }
    }

    private static String selectFrom(String fromList) {
        if (fromList == null || fromList.isBlank()) {
            return null;
        }
        String from = fromList.strip();
        if (from.regionMatches(true, 0, "FROM", 0, 4)
                && (from.length() == 4 || Character.isWhitespace(from.charAt(4)))) {
            return "SELECT 1 " + from;
        }
        return "SELECT 1 FROM " + from;
    }

    private static String qualifiedName(String table) {
        return table == null ? "" : table.strip();
    }

    record TableMappings(String[] tableNames, boolean complete) {
    }
}
