package cn.hospital.rehab.common.importing;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

public final class ExcelSheetRows {
    private ExcelSheetRows() { }

    public static List<Map<String, String>> read(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<Map<String, String>> result = new ArrayList<>();
            for (Sheet sheet : workbook) {
                Row header = sheet.getRow(sheet.getFirstRowNum());
                if (header == null) continue;
                Map<Integer, String> headers = new HashMap<>();
                for (Cell cell : header) {
                    String value = formatter.formatCellValue(cell, evaluator).trim();
                    if (!value.isEmpty()) headers.put(cell.getColumnIndex(), value);
                }
                if (headers.isEmpty()) continue;
                for (Row row : sheet) {
                    if (row.getRowNum() <= header.getRowNum()) continue;
                    Map<String, String> values = new HashMap<>();
                    boolean present = false;
                    for (var entry : headers.entrySet()) {
                        Cell cell = row.getCell(entry.getKey());
                        String value = cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
                        if (!value.isEmpty()) present = true;
                        values.put(entry.getValue(), value);
                    }
                    if (present) result.add(values);
                }
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Excel读取失败：" + exception.getMessage(), exception);
        }
    }

    public static String value(Map<String, String> row, String... names) {
        for (String name : names) {
            String value = row.get(name);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    public static Integer integer(Map<String, String> row, String... names) {
        String value = value(row, names);
        if (value == null) return null;
        try { return new java.math.BigDecimal(value.replace(",", "")).intValueExact(); }
        catch (NumberFormatException | ArithmeticException exception) { return null; }
    }
}