package com.metrics.exportservice.dto;

public class BulkUploadRequest {
    private String fileName;
    private byte[] fileData;
    
    public BulkUploadRequest() {}
    
    public BulkUploadRequest(String fileName, byte[] fileData) {
        this.fileName = fileName;
        this.fileData = fileData;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
}
