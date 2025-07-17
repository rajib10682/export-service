package com.metrics.exportservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
public class Item {
    
    @Id
    @Column(name = "item_id", nullable = false)
    @NotNull
    private String itemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "override_id", nullable = false)
    @NotNull
    @JsonBackReference
    private Override override;
    
    @Column(name = "currency_code", length = 3, nullable = false)
    @Size(min = 3, max = 3)
    @NotNull
    private String currencyCode;
    
    @Column(name = "create_timestamp", nullable = false)
    @NotNull
    private LocalDateTime createTimestamp;
    
    @Column(name = "updated_timestamp", nullable = false)
    @NotNull
    private LocalDateTime updatedTimestamp;
    
    @Column(name = "created_by", nullable = false)
    @NotNull
    private String createdBy;
    
    @Column(name = "updated_by", nullable = false)
    @NotNull
    private String updatedBy;
    
    @Column(name = "item_name", nullable = false, unique = true)
    @NotNull
    private String itemName;
    
    @Column(name = "item_value_month_1") private Double itemValueMonth1;
    @Column(name = "item_value_month_2") private Double itemValueMonth2;
    @Column(name = "item_value_month_3") private Double itemValueMonth3;
    @Column(name = "item_value_month_4") private Double itemValueMonth4;
    @Column(name = "item_value_month_5") private Double itemValueMonth5;
    @Column(name = "item_value_month_6") private Double itemValueMonth6;
    @Column(name = "item_value_month_7") private Double itemValueMonth7;
    @Column(name = "item_value_month_8") private Double itemValueMonth8;
    @Column(name = "item_value_month_9") private Double itemValueMonth9;
    @Column(name = "item_value_month_10") private Double itemValueMonth10;
    @Column(name = "item_value_month_11") private Double itemValueMonth11;
    @Column(name = "item_value_month_12") private Double itemValueMonth12;
    @Column(name = "item_value_month_13") private Double itemValueMonth13;
    @Column(name = "item_value_month_14") private Double itemValueMonth14;
    @Column(name = "item_value_month_15") private Double itemValueMonth15;
    @Column(name = "item_value_month_16") private Double itemValueMonth16;
    @Column(name = "item_value_month_17") private Double itemValueMonth17;
    @Column(name = "item_value_month_18") private Double itemValueMonth18;
    @Column(name = "item_value_month_19") private Double itemValueMonth19;
    @Column(name = "item_value_month_20") private Double itemValueMonth20;
    
    public Item() {
        this.createTimestamp = LocalDateTime.now();
        this.updatedTimestamp = LocalDateTime.now();
    }
    
    public Item(String itemId, Override override, String currencyCode, String createdBy, String updatedBy, String itemName) {
        this();
        this.itemId = itemId;
        this.override = override;
        this.currencyCode = currencyCode;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.itemName = itemName;
    }
    
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public Override getOverride() { return override; }
    public void setOverride(Override override) { this.override = override; }
    
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    
    public LocalDateTime getCreateTimestamp() { return createTimestamp; }
    public void setCreateTimestamp(LocalDateTime createTimestamp) { this.createTimestamp = createTimestamp; }
    
    public LocalDateTime getUpdatedTimestamp() { return updatedTimestamp; }
    public void setUpdatedTimestamp(LocalDateTime updatedTimestamp) { this.updatedTimestamp = updatedTimestamp; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Double getItemValueMonth1() { return itemValueMonth1; }
    public void setItemValueMonth1(Double itemValueMonth1) { this.itemValueMonth1 = itemValueMonth1; }
    
    public Double getItemValueMonth2() { return itemValueMonth2; }
    public void setItemValueMonth2(Double itemValueMonth2) { this.itemValueMonth2 = itemValueMonth2; }
    
    public Double getItemValueMonth3() { return itemValueMonth3; }
    public void setItemValueMonth3(Double itemValueMonth3) { this.itemValueMonth3 = itemValueMonth3; }
    
    public Double getItemValueMonth4() { return itemValueMonth4; }
    public void setItemValueMonth4(Double itemValueMonth4) { this.itemValueMonth4 = itemValueMonth4; }
    
    public Double getItemValueMonth5() { return itemValueMonth5; }
    public void setItemValueMonth5(Double itemValueMonth5) { this.itemValueMonth5 = itemValueMonth5; }
    
    public Double getItemValueMonth6() { return itemValueMonth6; }
    public void setItemValueMonth6(Double itemValueMonth6) { this.itemValueMonth6 = itemValueMonth6; }
    
    public Double getItemValueMonth7() { return itemValueMonth7; }
    public void setItemValueMonth7(Double itemValueMonth7) { this.itemValueMonth7 = itemValueMonth7; }
    
    public Double getItemValueMonth8() { return itemValueMonth8; }
    public void setItemValueMonth8(Double itemValueMonth8) { this.itemValueMonth8 = itemValueMonth8; }
    
    public Double getItemValueMonth9() { return itemValueMonth9; }
    public void setItemValueMonth9(Double itemValueMonth9) { this.itemValueMonth9 = itemValueMonth9; }
    
    public Double getItemValueMonth10() { return itemValueMonth10; }
    public void setItemValueMonth10(Double itemValueMonth10) { this.itemValueMonth10 = itemValueMonth10; }
    
    public Double getItemValueMonth11() { return itemValueMonth11; }
    public void setItemValueMonth11(Double itemValueMonth11) { this.itemValueMonth11 = itemValueMonth11; }
    
    public Double getItemValueMonth12() { return itemValueMonth12; }
    public void setItemValueMonth12(Double itemValueMonth12) { this.itemValueMonth12 = itemValueMonth12; }
    
    public Double getItemValueMonth13() { return itemValueMonth13; }
    public void setItemValueMonth13(Double itemValueMonth13) { this.itemValueMonth13 = itemValueMonth13; }
    
    public Double getItemValueMonth14() { return itemValueMonth14; }
    public void setItemValueMonth14(Double itemValueMonth14) { this.itemValueMonth14 = itemValueMonth14; }
    
    public Double getItemValueMonth15() { return itemValueMonth15; }
    public void setItemValueMonth15(Double itemValueMonth15) { this.itemValueMonth15 = itemValueMonth15; }
    
    public Double getItemValueMonth16() { return itemValueMonth16; }
    public void setItemValueMonth16(Double itemValueMonth16) { this.itemValueMonth16 = itemValueMonth16; }
    
    public Double getItemValueMonth17() { return itemValueMonth17; }
    public void setItemValueMonth17(Double itemValueMonth17) { this.itemValueMonth17 = itemValueMonth17; }
    
    public Double getItemValueMonth18() { return itemValueMonth18; }
    public void setItemValueMonth18(Double itemValueMonth18) { this.itemValueMonth18 = itemValueMonth18; }
    
    public Double getItemValueMonth19() { return itemValueMonth19; }
    public void setItemValueMonth19(Double itemValueMonth19) { this.itemValueMonth19 = itemValueMonth19; }
    
    public Double getItemValueMonth20() { return itemValueMonth20; }
    public void setItemValueMonth20(Double itemValueMonth20) { this.itemValueMonth20 = itemValueMonth20; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
}
