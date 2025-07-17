package com.metrics.exportservice.service;

import com.metrics.exportservice.entity.Item;
import com.metrics.exportservice.entity.Override;
import com.metrics.exportservice.entity.Plan;
import com.metrics.exportservice.repository.ItemRepository;
import com.metrics.exportservice.repository.OverrideRepository;
import com.metrics.exportservice.repository.PlanRepository;
import com.metrics.exportservice.validation.SpecialCharacterValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class BulkDataService {
    
    @Autowired
    private PlanRepository planRepository;
    
    @Autowired
    private OverrideRepository overrideRepository;
    
    @Autowired
    private ItemRepository itemRepository;
    
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Transactional
    public Map<String, List<String>> processBulkData(Map<String, List<Map<String, Object>>> excelData) {
        Map<String, List<String>> statusMap = new HashMap<>();
        Map<String, List<String>> reasonMap = new HashMap<>();
        
        Set<String> successfulPlans = new HashSet<>();
        Set<String> successfulOverrides = new HashSet<>();
        
        statusMap.put("Plans", processPlans(excelData.get("Plans"), reasonMap, successfulPlans));
        statusMap.put("Overrides", processOverrides(excelData.get("Overrides"), reasonMap, successfulPlans, successfulOverrides));
        statusMap.put("Items", processItems(excelData.get("Items"), reasonMap, successfulOverrides));
        
        Map<String, List<String>> result = new HashMap<>();
        result.put("status", new ArrayList<>());
        result.put("reason", new ArrayList<>());
        
        for (String worksheet : Arrays.asList("Plans", "Overrides", "Items")) {
            result.get("status").addAll(statusMap.get(worksheet));
            result.get("reason").addAll(reasonMap.get(worksheet));
        }
        
        return result;
    }
    
    private List<String> processPlans(List<Map<String, Object>> planData, Map<String, List<String>> reasonMap, Set<String> successfulPlans) {
        List<String> statusList = new ArrayList<>();
        List<String> reasonList = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        System.out.println("Phase 1: Starting batch processing of " + planData.size() + " plans");
        
        List<List<Map<String, Object>>> batches = partitionDataIntoBatches(planData, 1000);
        
        for (List<Map<String, Object>> batch : batches) {
            List<Plan> plansToSave = new ArrayList<>();
            List<Plan> plansToDelete = new ArrayList<>();
            
            for (Map<String, Object> rowData : batch) {
                try {
                    String planName = getString(rowData, "Plan_Name");
                    String forDateStr = getString(rowData, "For_Date");
                    String dataIdStr = getString(rowData, "Data_ID");
                    String deleteFlag = getString(rowData, "Delete");
                    
                    String validationError = validatePlanData(planName, forDateStr, dataIdStr);
                    if (validationError != null) {
                        statusList.add("Error");
                        reasonList.add(validationError);
                        continue;
                    }
                    
                    if ("D".equalsIgnoreCase(deleteFlag)) {
                        Optional<Plan> existingPlan = planRepository.findByPlanName(planName);
                        if (existingPlan.isPresent()) {
                            plansToDelete.add(existingPlan.get());
                            statusList.add("Success");
                            reasonList.add("Plan deleted successfully");
                        } else {
                            statusList.add("Error");
                            reasonList.add("Plan not found for deletion");
                        }
                    } else {
                        LocalDate forDate = LocalDate.parse(forDateStr, DATE_FORMATTER);
                        Integer dataId = Integer.parseInt(dataIdStr);
                        
                        Optional<Plan> existingPlan = planRepository.findByPlanName(planName);
                        Plan plan;
                        
                        if (existingPlan.isPresent()) {
                            plan = existingPlan.get();
                            plan.setForDate(forDate);
                            plan.setDataId(dataId);
                            statusList.add("Success");
                            reasonList.add("Plan updated successfully");
                        } else {
                            plan = new Plan();
                            plan.setPlanName(planName);
                            plan.setForDate(forDate);
                            plan.setDataId(dataId);
                            statusList.add("Success");
                            reasonList.add("Plan created successfully");
                        }
                        
                        plansToSave.add(plan);
                        successfulPlans.add(planName);
                    }
                    
                } catch (Exception e) {
                    statusList.add("Error");
                    reasonList.add("Processing error: " + e.getMessage());
                }
            }
            
            if (!plansToSave.isEmpty()) {
                planRepository.saveAll(plansToSave);
                System.out.println("Phase 1: Batch saved " + plansToSave.size() + " plans");
            }
            
            if (!plansToDelete.isEmpty()) {
                planRepository.deleteAll(plansToDelete);
                System.out.println("Phase 1: Batch deleted " + plansToDelete.size() + " plans");
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("Phase 1: Completed plan processing in " + (endTime - startTime) + "ms");
        
        reasonMap.put("Plans", reasonList);
        return statusList;
    }
    
    private List<String> processOverrides(List<Map<String, Object>> overrideData, Map<String, List<String>> reasonMap, 
                                        Set<String> successfulPlans, Set<String> successfulOverrides) {
        List<String> statusList = new ArrayList<>();
        List<String> reasonList = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        System.out.println("Phase 1: Starting batch processing of " + overrideData.size() + " overrides");
        
        List<List<Map<String, Object>>> batches = partitionDataIntoBatches(overrideData, 1000);
        
        for (List<Map<String, Object>> batch : batches) {
            List<Override> overridesToSave = new ArrayList<>();
            
            for (Map<String, Object> rowData : batch) {
                try {
                    String planName = getString(rowData, "Plan_Name");
                    String overrideName = getString(rowData, "Override_Name");
                    
                    if (!successfulPlans.contains(planName)) {
                        statusList.add("Error");
                        reasonList.add("Parent Plan failed or not found: " + planName);
                        continue;
                    }
                    
                    String validationError = validateOverrideData(rowData);
                    if (validationError != null) {
                        statusList.add("Error");
                        reasonList.add(validationError);
                        continue;
                    }
                    
                    Optional<Plan> parentPlan = planRepository.findByPlanName(planName);
                    if (!parentPlan.isPresent()) {
                        statusList.add("Error");
                        reasonList.add("Parent Plan not found: " + planName);
                        continue;
                    }
                    
                    Double totalExecTime = getDouble(rowData, "Total_Execution_Time");
                    Double onHoldTime = getDouble(rowData, "On_Hold_Time");
                    Double coreExecTime = getDouble(rowData, "Core_Execution_Time");
                    String requestType = getString(rowData, "Request_Type");
                    
                    Optional<Override> existingOverride = overrideRepository.findByOverrideName(overrideName);
                    Override override;
                    
                    if (existingOverride.isPresent()) {
                        override = existingOverride.get();
                        override.setPlan(parentPlan.get());
                        override.setTotalExecutionTime(totalExecTime);
                        override.setOnHoldTime(onHoldTime);
                        override.setCoreExecutionTime(coreExecTime);
                        override.setRequestType(requestType);
                        statusList.add("Success");
                        reasonList.add("Override updated successfully");
                    } else {
                        if (parentPlan.get() == null || overrideName == null || totalExecTime == null || 
                            onHoldTime == null || coreExecTime == null || requestType == null) {
                            statusList.add("Error");
                            reasonList.add("Required Override fields cannot be null");
                            continue;
                        }
                        override = new Override(parentPlan.get(), overrideName, totalExecTime, onHoldTime, coreExecTime, requestType);
                        statusList.add("Success");
                        reasonList.add("Override created successfully");
                    }
                    
                    String entityValidationError = validateOverrideEntity(override);
                    if (entityValidationError != null) {
                        statusList.add("Error");
                        reasonList.add(entityValidationError);
                        continue;
                    }
                    
                    overridesToSave.add(override);
                    successfulOverrides.add(overrideName);
                    
                } catch (Exception e) {
                    statusList.add("Error");
                    reasonList.add("Processing error: " + e.getMessage());
                }
            }
            
            if (!overridesToSave.isEmpty()) {
                overrideRepository.saveAll(overridesToSave);
                System.out.println("Phase 1: Batch saved " + overridesToSave.size() + " overrides");
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("Phase 1: Completed override processing in " + (endTime - startTime) + "ms");
        
        reasonMap.put("Overrides", reasonList);
        return statusList;
    }
    
    private List<String> processItems(List<Map<String, Object>> itemData, Map<String, List<String>> reasonMap, Set<String> successfulOverrides) {
        List<String> statusList = new ArrayList<>();
        List<String> reasonList = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        System.out.println("Phase 1: Starting batch processing of " + itemData.size() + " items");
        
        List<List<Map<String, Object>>> batches = partitionDataIntoBatches(itemData, 1000);
        
        for (List<Map<String, Object>> batch : batches) {
            List<Item> itemsToSave = new ArrayList<>();
            
            for (Map<String, Object> rowData : batch) {
                try {
                    String overrideName = getString(rowData, "Override_Name");
                    String itemName = getString(rowData, "Item_Name");
                    
                    if (!successfulOverrides.contains(overrideName)) {
                        statusList.add("Error");
                        reasonList.add("Parent Override failed or not found: " + overrideName);
                        continue;
                    }
                    
                    String validationError = validateItemData(rowData);
                    if (validationError != null) {
                        statusList.add("Error");
                        reasonList.add(validationError);
                        continue;
                    }
                    
                    Optional<Override> parentOverride = overrideRepository.findByOverrideName(overrideName);
                    if (!parentOverride.isPresent()) {
                        statusList.add("Error");
                        reasonList.add("Parent Override not found: " + overrideName);
                        continue;
                    }
                    
                    String currencyCode = getString(rowData, "Currency_Code");
                    String createdBy = getString(rowData, "Created_By");
                    String updatedBy = getString(rowData, "Updated_By");
                    
                    Optional<Item> existingItem = itemRepository.findByItemName(itemName);
                    Item item;
                    
                    if (existingItem.isPresent()) {
                        item = existingItem.get();
                        item.setOverride(parentOverride.get());
                        item.setCurrencyCode(currencyCode);
                        item.setUpdatedBy(updatedBy);
                        item.setUpdatedTimestamp(LocalDateTime.now());
                        statusList.add("Success");
                        reasonList.add("Item updated successfully");
                    } else {
                        String itemId = "ITM_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
                        item = new Item(itemId, parentOverride.get(), currencyCode, createdBy, updatedBy, itemName);
                        statusList.add("Success");
                        reasonList.add("Item created successfully");
                    }
                    
                    setItemValues(item, rowData);
                    itemsToSave.add(item);
                    
                } catch (Exception e) {
                    statusList.add("Error");
                    reasonList.add("Processing error: " + e.getMessage());
                }
            }
            
            if (!itemsToSave.isEmpty()) {
                itemRepository.saveAll(itemsToSave);
                System.out.println("Phase 1: Batch saved " + itemsToSave.size() + " items");
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("Phase 1: Completed item processing in " + (endTime - startTime) + "ms");
        
        reasonMap.put("Items", reasonList);
        return statusList;
    }
    
    public String validatePlanData(String planName, String forDateStr, String dataIdStr) {
        if (planName == null || planName.trim().isEmpty()) {
            return "Plan_Name is required";
        }
        
        String specialCharError = SpecialCharacterValidator.validateCellValue(planName);
        if (specialCharError != null) {
            return "Plan_Name: " + specialCharError;
        }
        
        if (forDateStr == null || forDateStr.trim().isEmpty()) {
            return "For_Date is required";
        }
        
        try {
            LocalDate forDate = LocalDate.parse(forDateStr, DATE_FORMATTER);
            if (!isQuarterEndDate(forDate)) {
                return "For_Date must be a quarter-end date (Mar 31, Jun 30, Sep 30, Dec 31)";
            }
        } catch (DateTimeParseException e) {
            return "Invalid For_Date format. Expected: dd-MMM-yyyy";
        }
        
        if (dataIdStr == null || dataIdStr.trim().isEmpty()) {
            return "Data_ID is required";
        }
        
        try {
            Integer.parseInt(dataIdStr);
        } catch (NumberFormatException e) {
            return "Data_ID must be a valid number";
        }
        
        return null;
    }
    
    public String validateOverrideData(Map<String, Object> rowData) {
        String overrideName = getString(rowData, "Override_Name");
        if (overrideName == null || overrideName.trim().isEmpty()) {
            return "Override_Name is required";
        }
        
        String specialCharError = SpecialCharacterValidator.validateCellValue(overrideName);
        if (specialCharError != null) {
            return "Override_Name: " + specialCharError;
        }
        
        try {
            Double totalExecTime = getDouble(rowData, "Total_Execution_Time");
            Double onHoldTime = getDouble(rowData, "On_Hold_Time");
            Double coreExecTime = getDouble(rowData, "Core_Execution_Time");
            
            if (totalExecTime == null || totalExecTime < 0) {
                return "Total_Execution_Time must be a positive number";
            }
            if (onHoldTime == null || onHoldTime < 0) {
                return "On_Hold_Time must be a positive number";
            }
            if (coreExecTime == null || coreExecTime < 0) {
                return "Core_Execution_Time must be a positive number";
            }
        } catch (Exception e) {
            return "Invalid execution time values";
        }
        
        String requestType = getString(rowData, "Request_Type");
        if (requestType == null || requestType.trim().isEmpty()) {
            return "Request_Type is required";
        }
        
        return null;
    }
    
    public String validateItemData(Map<String, Object> rowData) {
        String itemName = getString(rowData, "Item_Name");
        if (itemName == null || itemName.trim().isEmpty()) {
            return "Item_Name is required";
        }
        
        String specialCharError = SpecialCharacterValidator.validateCellValue(itemName);
        if (specialCharError != null) {
            return "Item_Name: " + specialCharError;
        }
        
        String currencyCode = getString(rowData, "Currency_Code");
        if (currencyCode == null || currencyCode.length() != 3) {
            return "Currency_Code must be exactly 3 characters";
        }
        
        return null;
    }
    
    public void setItemValues(Item item, Map<String, Object> rowData) {
        for (int i = 1; i <= 20; i++) {
            String columnName = "Item_Value_Month_" + i;
            Double value = getDouble(rowData, columnName);
            
            switch (i) {
                case 1: item.setItemValueMonth1(value); break;
                case 2: item.setItemValueMonth2(value); break;
                case 3: item.setItemValueMonth3(value); break;
                case 4: item.setItemValueMonth4(value); break;
                case 5: item.setItemValueMonth5(value); break;
                case 6: item.setItemValueMonth6(value); break;
                case 7: item.setItemValueMonth7(value); break;
                case 8: item.setItemValueMonth8(value); break;
                case 9: item.setItemValueMonth9(value); break;
                case 10: item.setItemValueMonth10(value); break;
                case 11: item.setItemValueMonth11(value); break;
                case 12: item.setItemValueMonth12(value); break;
                case 13: item.setItemValueMonth13(value); break;
                case 14: item.setItemValueMonth14(value); break;
                case 15: item.setItemValueMonth15(value); break;
                case 16: item.setItemValueMonth16(value); break;
                case 17: item.setItemValueMonth17(value); break;
                case 18: item.setItemValueMonth18(value); break;
                case 19: item.setItemValueMonth19(value); break;
                case 20: item.setItemValueMonth20(value); break;
            }
        }
    }
    
    private boolean isQuarterEndDate(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        return (month == 3 && day == 31) ||
               (month == 6 && day == 30) ||
               (month == 9 && day == 30) ||
               (month == 12 && day == 31);
    }
    
    public String getString(Map<String, Object> rowData, String key) {
        Object value = rowData.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            Date date = (Date) value;
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.format(DATE_FORMATTER);
        }
        if (value instanceof Number) {
            return String.valueOf(((Number) value).longValue());
        }
        return value.toString().trim();
    }
    
    public Double getDouble(Map<String, Object> rowData, String key) {
        Object value = rowData.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public String validateOverrideEntity(Override override) {
        if (override.getPlan() == null) {
            return "Plan relationship is required";
        }
        if (override.getOverrideName() == null || override.getOverrideName().trim().isEmpty()) {
            return "Override_Name is required";
        }
        if (override.getTotalExecutionTime() == null) {
            return "Total_Execution_Time is required";
        }
        if (override.getOnHoldTime() == null) {
            return "On_Hold_Time is required";
        }
        if (override.getCoreExecutionTime() == null) {
            return "Core_Execution_Time is required";
        }
        if (override.getRequestType() == null || override.getRequestType().trim().isEmpty()) {
            return "Request_Type is required";
        }
        return null;
    }
    
    private List<List<Map<String, Object>>> partitionDataIntoBatches(List<Map<String, Object>> data, int batchSize) {
        List<List<Map<String, Object>>> batches = new ArrayList<>();
        
        for (int i = 0; i < data.size(); i += batchSize) {
            int end = Math.min(i + batchSize, data.size());
            batches.add(new ArrayList<>(data.subList(i, end)));
        }
        
        return batches;
    }
}
