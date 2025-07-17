package com.metrics.exportservice.dto;

public class BulkUploadResponse {
    private String fileName;
    private byte[] responseExcelData;
    private int totalRecords;
    private int successfulRecords;
    private int failedRecords;
    
    public BulkUploadResponse() {}
    
    public BulkUploadResponse(String fileName, byte[] responseExcelData, int totalRecords, int successfulRecords, int failedRecords) {
        this.fileName = fileName;
        this.responseExcelData = responseExcelData;
        this.totalRecords = totalRecords;
        this.successfulRecords = successfulRecords;
        this.failedRecords = failedRecords;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public byte[] getResponseExcelData() {
        return responseExcelData;
    }
    
    public void setResponseExcelData(byte[] responseExcelData) {
        this.responseExcelData = responseExcelData;
    }
    
    public int getTotalRecords() {
        return totalRecords;
    }
    
    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    public int getSuccessfulRecords() {
        return successfulRecords;
    }
    
    public void setSuccessfulRecords(int successfulRecords) {
        this.successfulRecords = successfulRecords;
    }
    
    public int getFailedRecords() {
        return failedRecords;
    }
    
    public void setFailedRecords(int failedRecords) {
        this.failedRecords = failedRecords;
    }
}
