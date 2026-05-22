package com.example.familymanage.dto;

import java.math.BigDecimal;

public class TravelDomesticRequest {

    private Long cityId;
    private BigDecimal cost;

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
}
