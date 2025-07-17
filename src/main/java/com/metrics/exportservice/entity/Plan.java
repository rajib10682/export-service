package com.metrics.exportservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import com.metrics.exportservice.validation.QuarterEndDate;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;
    
    @Column(name = "plan_name", nullable = false, unique = true)
    @NotNull
    private String planName;
    
    @Column(name = "for_date", nullable = false)
    @NotNull
    @QuarterEndDate
    private LocalDate forDate;
    
    @Column(name = "data_id", nullable = false)
    @NotNull
    private Integer dataId;
    
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Override> overrides;
    
    public Plan() {}
    
    public Plan(String planName, LocalDate forDate, Integer dataId) {
        this.planName = planName;
        this.forDate = forDate;
        this.dataId = dataId;
    }
    
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    
    public LocalDate getForDate() { return forDate; }
    public void setForDate(LocalDate forDate) { this.forDate = forDate; }
    
    public Integer getDataId() { return dataId; }
    public void setDataId(Integer dataId) { this.dataId = dataId; }
    
    public List<Override> getOverrides() { return overrides; }
    public void setOverrides(List<Override> overrides) { this.overrides = overrides; }
}
