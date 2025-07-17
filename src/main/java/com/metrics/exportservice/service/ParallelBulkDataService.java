package com.metrics.exportservice.service;

import com.metrics.exportservice.entity.Item;
import com.metrics.exportservice.entity.Override;
import com.metrics.exportservice.entity.Plan;
import com.metrics.exportservice.repository.ItemRepository;
import com.metrics.exportservice.repository.OverrideRepository;
import com.metrics.exportservice.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParallelBulkDataService {
    
    @Autowired
    private PlanRepository planRepository;
    
    @Autowired
    private OverrideRepository overrideRepository;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private BulkDataService bulkDataService;
    
    public Map<String, List<String>> processParallelBulkData(Map<String, List<Map<String, Object>>> excelData) {
        List<Map<String, Object>> planData = excelData.get("Plans");
        List<Map<String, Object>> overrideData = excelData.get("Overrides");
        List<Map<String, Object>> itemData = excelData.get("Items");
        
        if (planData.size() < 50) {
            return bulkDataService.processBulkData(excelData);
        }
        
        List<List<Map<String, Object>>> planGroups = partitionData(planData, 5);
        
        Map<String, List<String>> finalResults = new ConcurrentHashMap<>();
        finalResults.put("status", Collections.synchronizedList(new ArrayList<>()));
        finalResults.put("reason", Collections.synchronizedList(new ArrayList<>()));
        
        Set<String> successfulPlans = ConcurrentHashMap.newKeySet();
        
        try {
            List<CompletableFuture<Map<String, Object>>> planFutures = new ArrayList<>();
            
            for (int i = 0; i < planGroups.size(); i++) {
                List<Map<String, Object>> planGroup = planGroups.get(i);
                CompletableFuture<Map<String, Object>> future = processPlanGroupAsync(planGroup, i);
                planFutures.add(future);
            }
            
            CompletableFuture.allOf(planFutures.toArray(new CompletableFuture[0])).join();
            
            List<String> allStatuses = new ArrayList<>();
            List<String> allReasons = new ArrayList<>();
            
            for (CompletableFuture<Map<String, Object>> future : planFutures) {
                Map<String, Object> result = future.get();
                allStatuses.addAll((List<String>) result.get("status"));
                allReasons.addAll((List<String>) result.get("reason"));
                successfulPlans.addAll((Set<String>) result.get("successfulPlans"));
            }
            
            finalResults.get("status").addAll(allStatuses);
            finalResults.get("reason").addAll(allReasons);
            
            Map<String, List<String>> overrideResults = processOverridesWithDependencies(overrideData, successfulPlans);
            finalResults.get("status").addAll(overrideResults.get("status"));
            finalResults.get("reason").addAll(overrideResults.get("reason"));
            
            Set<String> successfulOverrides = new HashSet<>();
            for (int i = 0; i < overrideData.size(); i++) {
                if ("Success".equals(overrideResults.get("status").get(i))) {
                    successfulOverrides.add((String) overrideData.get(i).get("Override_Name"));
                }
            }
            
            Map<String, List<String>> itemResults = processItemsWithDependencies(itemData, successfulOverrides);
            finalResults.get("status").addAll(itemResults.get("status"));
            finalResults.get("reason").addAll(itemResults.get("reason"));
            
        } catch (Exception e) {
            return bulkDataService.processBulkData(excelData);
        }
        
        return finalResults;
    }
    
    @Async("bulkProcessingExecutor")
    @Transactional
    public CompletableFuture<Map<String, Object>> processPlanGroupAsync(List<Map<String, Object>> planGroup, int groupId) {
        List<String> statusList = new ArrayList<>();
        List<String> reasonList = new ArrayList<>();
        Set<String> successfulPlans = new HashSet<>();
        
        long startTime = System.currentTimeMillis();
        System.out.println("Phase 1: Thread " + groupId + " processing " + planGroup.size() + " plans with batch operations");
        
        List<List<Map<String, Object>>> batches = partitionData(planGroup, 500);
        
        for (List<Map<String, Object>> batch : batches) {
            List<Plan> plansToSave = new ArrayList<>();
            List<Plan> plansToDelete = new ArrayList<>();
            
            for (Map<String, Object> rowData : batch) {
                try {
                    String planName = bulkDataService.getString(rowData, "Plan_Name");
                    String forDateStr = bulkDataService.getString(rowData, "For_Date");
                    String dataIdStr = bulkDataService.getString(rowData, "Data_ID");
                    String deleteFlag = bulkDataService.getString(rowData, "Delete");
                    
                    String validationError = bulkDataService.validatePlanData(planName, forDateStr, dataIdStr);
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
                        LocalDate forDate = LocalDate.parse(forDateStr, bulkDataService.DATE_FORMATTER);
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
                System.out.println("Phase 1: Thread " + groupId + " batch saved " + plansToSave.size() + " plans");
            }
            
            if (!plansToDelete.isEmpty()) {
                planRepository.deleteAll(plansToDelete);
                System.out.println("Phase 1: Thread " + groupId + " batch deleted " + plansToDelete.size() + " plans");
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("Phase 1: Thread " + groupId + " completed in " + (endTime - startTime) + "ms");
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", statusList);
        result.put("reason", reasonList);
        result.put("successfulPlans", successfulPlans);
        
        return CompletableFuture.completedFuture(result);
    }
    
    @Transactional
    public Map<String, List<String>> processOverridesWithDependencies(List<Map<String, Object>> overrideData, Set<String> successfulPlans) {
        List<String> statusList = new ArrayList<>();
        List<String> reasonList = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        System.out.println("Phase 1: Processing " + overrideData.size() + " overrides with batch operations");
        
        List<List<Map<String, Object>>> batches = partitionData(overrideData, 1000);
        
        for (List<Map<String, Object>> batch : batches) {
            List<Override> overridesToSave = new ArrayList<>();
            
            for (Map<String, Object> rowData : batch) {
                try {
                    String planName = bulkDataService.getString(rowData, "Plan_Name");
                    String overrideName = bulkDataService.getString(rowData, "Override_Name");
                    
                    if (!successfulPlans.contains(planName)) {
                        statusList.add("Error");
                        reasonList.add("Parent plan not found or failed to process");
                        continue;
                    }
                    
                    String validationError = bulkDataService.validateOverrideData(rowData);
                    if (validationError != null) {
                        statusList.add("Error");
                        reasonList.add(validationError);
                        continue;
                    }
                    
                    Optional<Plan> parentPlan = planRepository.findByPlanName(planName);
                    if (!parentPlan.isPresent()) {
                        statusList.add("Error");
                        reasonList.add("Parent plan not found");
                        continue;
                    }
                    
                    Double totalExecTime = bulkDataService.getDouble(rowData, "Total_Execution_Time");
                    Double onHoldTime = bulkDataService.getDouble(rowData, "On_Hold_Time");
                    Double coreExecTime = bulkDataService.getDouble(rowData, "Core_Execution_Time");
                    String requestType = bulkDataService.getString(rowData, "Request_Type");
                    
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
                    
                    String entityValidationError = bulkDataService.validateOverrideEntity(override);
                    if (entityValidationError != null) {
                        statusList.add("Error");
                        reasonList.add(entityValidationError);
                        continue;
                    }
                    
                    overridesToSave.add(override);
                    
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
        
        Map<String, List<String>> result = new HashMap<>();
        result.put("status", statusList);
        result.put("reason", reasonList);
        return result;
    }
    
    @Transactional
    public Map<String, List<String>> processItemsWithDependencies(List<Map<String, Object>> itemData, Set<String> successfulOverrides) {
        List<String> statusList = new ArrayList<>();
        List<String> reasonList = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        System.out.println("Phase 1: Processing " + itemData.size() + " items with batch operations");
        
        List<List<Map<String, Object>>> batches = partitionData(itemData, 1000);
        
        for (List<Map<String, Object>> batch : batches) {
            List<Item> itemsToSave = new ArrayList<>();
            
            for (Map<String, Object> rowData : batch) {
                try {
                    String overrideName = bulkDataService.getString(rowData, "Override_Name");
                    String itemName = bulkDataService.getString(rowData, "Item_Name");
                    
                    if (!successfulOverrides.contains(overrideName)) {
                        statusList.add("Error");
                        reasonList.add("Parent override not found or failed to process");
                        continue;
                    }
                    
                    String validationError = bulkDataService.validateItemData(rowData);
                    if (validationError != null) {
                        statusList.add("Error");
                        reasonList.add(validationError);
                        continue;
                    }
                    
                    Optional<Override> parentOverride = overrideRepository.findByOverrideName(overrideName);
                    if (!parentOverride.isPresent()) {
                        statusList.add("Error");
                        reasonList.add("Parent override not found");
                        continue;
                    }
                    
                    String currencyCode = bulkDataService.getString(rowData, "Currency_Code");
                    String createdBy = bulkDataService.getString(rowData, "Created_By");
                    String updatedBy = bulkDataService.getString(rowData, "Updated_By");
                    
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
                    
                    bulkDataService.setItemValues(item, rowData);
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
        
        Map<String, List<String>> result = new HashMap<>();
        result.put("status", statusList);
        result.put("reason", reasonList);
        return result;
    }
    
    private List<List<Map<String, Object>>> partitionData(List<Map<String, Object>> data, int partitions) {
        List<List<Map<String, Object>>> result = new ArrayList<>();
        int partitionSize = Math.max(1, data.size() / partitions);
        
        for (int i = 0; i < data.size(); i += partitionSize) {
            int end = Math.min(i + partitionSize, data.size());
            result.add(new ArrayList<>(data.subList(i, end)));
        }
        
        return result;
    }
}
