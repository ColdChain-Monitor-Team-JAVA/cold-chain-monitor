package com.coldchain.web;

import java.util.List;
import java.util.stream.Collectors;

/** CSV escaping with protection against spreadsheet formula injection. */
public final class CsvUtil {
    private CsvUtil() {}

    public static String cell(Object v) {
        if (v == null) return "";
        if (v instanceof Number) return v.toString();
        String s = String.valueOf(v);
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        boolean quote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        return quote ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    public static String row(List<?> values) { return values.stream().map(CsvUtil::cell).collect(Collectors.joining(",")); }
}
