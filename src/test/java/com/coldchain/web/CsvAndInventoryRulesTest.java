package com.coldchain.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CsvAndInventoryRulesTest {
    @Test void formulaCellsArePrefixed() {
        assertEquals("'=SUM(A1)", CsvUtil.cell("=SUM(A1)"));
        assertEquals("'@cmd", CsvUtil.cell("@cmd"));
        assertEquals("-5.5", CsvUtil.cell(-5.5));
    }
    @Test void specialCharactersAreQuoted() {
        assertEquals("\"a,b\"", CsvUtil.cell("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvUtil.cell("say \"hi\""));
        assertEquals("1,\"x,y\",", CsvUtil.row(Arrays.asList(1, "x,y", null)));
    }
    @Test void stockStatusPriority() {
        LocalDate t = LocalDate.of(2026, 10, 1);
        assertEquals("EXPIRED", Rules.stockStatus(500, 20, t.minusDays(1), t));
        assertEquals("LOW", Rules.stockStatus(10, 20, t.plusDays(200), t));
        assertEquals("EXPIRING", Rules.stockStatus(100, 20, t.plusDays(10), t));
        assertEquals("OK", Rules.stockStatus(100, 20, t.plusDays(200), t));
    }
}
