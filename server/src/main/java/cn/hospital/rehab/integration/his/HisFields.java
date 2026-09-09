package cn.hospital.rehab.integration.his;

import java.math.BigDecimal;
import java.util.Map;

final class HisFields {
    private HisFields() {}
    static String text(Map<String, Object> row, String... names) {
        for (String name : names) for (var entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name) && entry.getValue() != null) {
                String value = String.valueOf(entry.getValue()).trim();
                if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) return value;
            }
        }
        return null;
    }
    static Integer positiveInteger(Map<String, Object> row, String... names) {
        String value = text(row, names);
        if (value == null) return null;
        try { return new BigDecimal(value.replace(",", "")).intValueExact(); }
        catch (ArithmeticException | NumberFormatException ignored) { return null; }
    }
}
