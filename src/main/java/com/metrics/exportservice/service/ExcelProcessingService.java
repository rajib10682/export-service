package com.metrics.exportservice.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ExcelProcessingService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public Map<String, List<Map<String, Object>>> parseExcelFile(byte[] fileData) throws IOException {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileData);
             Workbook workbook = new XSSFWorkbook(bis)) {
            
            result.put("Plans", parseWorksheet(workbook, "Plans", getPlanColumns()));
            result.put("Overrides", parseWorksheet(workbook, "Overrides", getOverrideColumns()));
            result.put("Items", parseWorksheet(workbook, "Items", getItemColumns()));
        }
        
        return result;
    }
    
    private List<Map<String, Object>> parseWorksheet(Workbook workbook, String sheetName, List<String> expectedColumns) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Sheet sheet = workbook.getSheet(sheetName);
        
        if (sheet == null) {
            return rows;
        }
        
        Iterator<Row> rowIterator = sheet.iterator();
        if (!rowIterator.hasNext()) {
            return rows;
        }
        
        Row headerRow = rowIterator.next();
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            headers.add(getCellValueAsString(cell));
        }
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Map<String, Object> rowData = new HashMap<>();
            
            for (int i = 0; i < headers.size() && i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                String header = headers.get(i);
                Object value = getCellValue(cell, header);
                rowData.put(header, value);
            }
            
            if (!isEmptyRow(rowData)) {
                rows.add(rowData);
            }
        }
        
        return rows;
    }
    
    private Object getCellValue(Cell cell, String header) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    private boolean isEmptyRow(Map<String, Object> rowData) {
        return rowData.values().stream().allMatch(value -> 
            value == null || (value instanceof String && ((String) value).trim().isEmpty())
        );
    }
    
    public byte[] generateResponseExcel(Map<String, List<Map<String, Object>>> originalData,
                                      Map<String, List<String>> statusData,
                                      Map<String, List<String>> reasonData) throws IOException {
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            generateResponseSheet(workbook, "Plans", originalData.get("Plans"), 
                                statusData.get("Plans"), reasonData.get("Plans"), getPlanColumns());
            generateResponseSheet(workbook, "Overrides", originalData.get("Overrides"), 
                                statusData.get("Overrides"), reasonData.get("Overrides"), getOverrideColumns());
            generateResponseSheet(workbook, "Items", originalData.get("Items"), 
                                statusData.get("Items"), reasonData.get("Items"), getItemColumns());
            
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
    
    private void generateResponseSheet(Workbook workbook, String sheetName, 
                                     List<Map<String, Object>> originalData,
                                     List<String> statusList, List<String> reasonList,
                                     List<String> columns) {
        Sheet sheet = workbook.createSheet(sheetName);
        
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        
        for (String column : columns) {
            headerRow.createCell(colIndex++).setCellValue(column);
        }
        headerRow.createCell(colIndex++).setCellValue("Status");
        headerRow.createCell(colIndex).setCellValue("Reason");
        
        for (int i = 0; i < originalData.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> rowData = originalData.get(i);
            
            colIndex = 0;
            for (String column : columns) {
                Object value = rowData.get(column);
                Cell cell = row.createCell(colIndex++);
                setCellValue(cell, value);
            }
            
            row.createCell(colIndex++).setCellValue(statusList.get(i));
            row.createCell(colIndex).setCellValue(reasonList.get(i));
        }
        
        for (int i = 0; i < columns.size() + 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof LocalDate) {
            cell.setCellValue(((LocalDate) value).format(DATE_FORMATTER));
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(TIMESTAMP_FORMATTER));
        } else {
            cell.setCellValue(value.toString());
        }
    }
    
    private List<String> getPlanColumns() {
        return Arrays.asList("Plan_Name", "For_Date", "Data_ID", "Delete");
    }
    
    private List<String> getOverrideColumns() {
        return Arrays.asList("Plan_Name", "Override_Name", "Total_Execution_Time", 
                           "On_Hold_Time", "Core_Execution_Time", "Request_Type");
    }
    
    private List<String> getItemColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("Override_Name");
        columns.add("Item_Name");
        columns.add("Currency_Code");
        columns.add("Create_Timestamp");
        columns.add("Updated_Timestamp");
        columns.add("Created_By");
        columns.add("Updated_By");
        
        for (int i = 1; i <= 20; i++) {
            columns.add("Item_Value_Month_" + i);
        }
        
        return columns;
    }
}
