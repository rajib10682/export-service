package com.metrics.exportservice.service;

import com.metrics.exportservice.entity.Plan;
import com.metrics.exportservice.entity.Override;
import com.metrics.exportservice.repository.PlanRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

@Service
public class ExportService {
    
    @Autowired
    private PlanRepository planRepository;
    
    public byte[] exportToCsv(Integer dataId) throws IOException {
        List<Plan> plans;
        if (dataId != null) {
            plans = planRepository.findByDataId(dataId);
        } else {
            plans = planRepository.findAll();
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out), CSVFormat.DEFAULT)) {
            printer.printRecord("Plan ID", "Plan Name", "For Date", "Data ID", "Avg Core Execution Time");
            
            for (Plan plan : plans) {
                Double avgCoreExecutionTime = calculateAvgCoreExecutionTime(plan);
                printer.printRecord(
                    plan.getPlanId(),
                    plan.getPlanName(),
                    plan.getForDate(),
                    plan.getDataId(),
                    avgCoreExecutionTime
                );
            }
        }
        
        return out.toByteArray();
    }
    
    private Double calculateAvgCoreExecutionTime(Plan plan) {
        if (plan.getOverrides() == null || plan.getOverrides().isEmpty()) {
            return 0.0;
        }
        
        return plan.getOverrides().stream()
            .mapToDouble(Override::getCoreExecutionTime)
            .average()
            .orElse(0.0);
    }
}
