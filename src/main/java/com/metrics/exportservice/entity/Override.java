package com.metrics.exportservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.List;

@Entity
@Table(name = "overrides")
public class Override {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "override_id")
    private Long overrideId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @NotNull
    @JsonBackReference
    private Plan plan;
    
    @Column(name = "override_name", nullable = false)
    @NotNull
    private String overrideName;
    
    @Column(name = "total_execution_time", nullable = false)
    @NotNull
    private Double totalExecutionTime;
    
    @Column(name = "on_hold_time", nullable = false)
    @NotNull
    private Double onHoldTime;
    
    @Column(name = "core_execution_time", nullable = false)
    @NotNull
    private Double coreExecutionTime;
    
    @Column(name = "request_type", nullable = false)
    @NotNull
    private String requestType;
    
    @OneToMany(mappedBy = "override", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items;
    
    public Override() {}
    
    public Override(Plan plan, String overrideName, Double totalExecutionTime, Double onHoldTime, Double coreExecutionTime, String requestType) {
        this.plan = plan;
        this.overrideName = overrideName;
        this.totalExecutionTime = totalExecutionTime;
        this.onHoldTime = onHoldTime;
        this.coreExecutionTime = coreExecutionTime;
        this.requestType = requestType;
    }
    
    public Long getOverrideId() { return overrideId; }
    public void setOverrideId(Long overrideId) { this.overrideId = overrideId; }
    
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    
    public String getOverrideName() { return overrideName; }
    public void setOverrideName(String overrideName) { this.overrideName = overrideName; }
    
    public Double getTotalExecutionTime() { return totalExecutionTime; }
    public void setTotalExecutionTime(Double totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }
    
    public Double getOnHoldTime() { return onHoldTime; }
    public void setOnHoldTime(Double onHoldTime) { this.onHoldTime = onHoldTime; }
    
    public Double getCoreExecutionTime() { return coreExecutionTime; }
    public void setCoreExecutionTime(Double coreExecutionTime) { this.coreExecutionTime = coreExecutionTime; }
    
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
