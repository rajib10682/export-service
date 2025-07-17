package com.metrics.exportservice.controller;

import com.metrics.exportservice.exception.BulkProcessingException;
import com.metrics.exportservice.service.BulkDataService;
import com.metrics.exportservice.service.ExcelProcessingService;
import com.metrics.exportservice.service.ParallelBulkDataService;
import com.metrics.exportservice.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export-service")
public class ExportServiceController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportServiceController.class);
    
    @Autowired
    private ExcelProcessingService excelProcessingService;
    
    @Autowired
    private BulkDataService bulkDataService;
    
    @Autowired
    private ParallelBulkDataService parallelBulkDataService;
    
    @Autowired
    private ExportService exportService;
    
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    @PostMapping("/upload/bulk")
    public ResponseEntity<byte[]> bulkUpload(@RequestParam("file") MultipartFile file) {
        logger.info("Received file upload request: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            logger.error("File is empty");
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            logger.error("File size exceeds limit: {} bytes", file.getSize());
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 50MB");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            logger.error("Invalid file format: {}", fileName);
            throw new IllegalArgumentException("File must be an Excel file (.xlsx or .xls)");
        }
        
        try {
            logger.info("Processing Excel file: {} ({} bytes)", fileName, file.getSize());
            byte[] fileData = file.getBytes();
            Map<String, List<Map<String, Object>>> excelData = excelProcessingService.parseExcelFile(fileData);
            
            logger.info("Parsed Excel data - Plans: {}, Overrides: {}, Items: {}", 
                excelData.get("Plans").size(), 
                excelData.get("Overrides").size(), 
                excelData.get("Items").size());
            
            Map<String, List<String>> processingResult = parallelBulkDataService.processParallelBulkData(excelData);
            
            Map<String, List<String>> statusMap = new HashMap<>();
            Map<String, List<String>> reasonMap = new HashMap<>();
            
            int planCount = excelData.get("Plans").size();
            int overrideCount = excelData.get("Overrides").size();
            int itemCount = excelData.get("Items").size();
            
            List<String> allStatuses = processingResult.get("status");
            List<String> allReasons = processingResult.get("reason");
            
            statusMap.put("Plans", allStatuses.subList(0, planCount));
            statusMap.put("Overrides", allStatuses.subList(planCount, planCount + overrideCount));
            statusMap.put("Items", allStatuses.subList(planCount + overrideCount, planCount + overrideCount + itemCount));
            
            reasonMap.put("Plans", allReasons.subList(0, planCount));
            reasonMap.put("Overrides", allReasons.subList(planCount, planCount + overrideCount));
            reasonMap.put("Items", allReasons.subList(planCount + overrideCount, planCount + overrideCount + itemCount));
            
            byte[] responseExcel = excelProcessingService.generateResponseExcel(excelData, statusMap, reasonMap);
            
            String responseFilename = "bulk_upload_response_" + System.currentTimeMillis() + ".xlsx";
            logger.info("Successfully processed file. Response file: {} ({} bytes)", responseFilename, responseExcel.length);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", responseFilename);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            
            return ResponseEntity.ok().headers(headers).body(responseExcel);
            
        } catch (Exception e) {
            logger.error("Failed to process file: {}", e.getMessage(), e);
            throw new BulkProcessingException("Failed to process bulk upload: " + e.getMessage(), e);
        }
    }
    
    @GetMapping("/download/csv")
    public ResponseEntity<byte[]> exportToCsv(@RequestParam(required = false) Integer dataId) {
        try {
            byte[] csvData = exportService.exportToCsv(dataId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "metrics_export.csv");
            return ResponseEntity.ok().headers(headers).body(csvData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/upload/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        return ResponseEntity.ok().build();
    }
}
