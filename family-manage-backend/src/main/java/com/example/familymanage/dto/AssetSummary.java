package com.example.familymanage.dto;

import java.math.BigDecimal;

public class AssetSummary {

    private long totalCategories;
    private long totalAssets;
    private long expiredAssets;
    private long expiringSoonAssets;
    private long lowStockAssets;
    private BigDecimal totalQuantity;

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public long getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(long totalAssets) {
        this.totalAssets = totalAssets;
    }

    public long getExpiredAssets() {
        return expiredAssets;
    }

    public void setExpiredAssets(long expiredAssets) {
        this.expiredAssets = expiredAssets;
    }

    public long getExpiringSoonAssets() {
        return expiringSoonAssets;
    }

    public void setExpiringSoonAssets(long expiringSoonAssets) {
        this.expiringSoonAssets = expiringSoonAssets;
    }

    public long getLowStockAssets() {
        return lowStockAssets;
    }

    public void setLowStockAssets(long lowStockAssets) {
        this.lowStockAssets = lowStockAssets;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
}
