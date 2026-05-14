package com.example.familymanage.dto;

import java.math.BigDecimal;

public class StockChangeRequest {

    private BigDecimal delta;

    public BigDecimal getDelta() {
        return delta;
    }

    public void setDelta(BigDecimal delta) {
        this.delta = delta;
    }
}
