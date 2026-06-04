package com.example.familymanage.dto;

import java.util.List;

public class KitchenCookRecordRequest {

    private String dishName;
    private String cookDate;
    private Integer isDone;
    private String remark;
    private List<String> dishNames;

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public String getCookDate() {
        return cookDate;
    }

    public void setCookDate(String cookDate) {
        this.cookDate = cookDate;
    }

    public Integer getIsDone() {
        return isDone;
    }

    public void setIsDone(Integer isDone) {
        this.isDone = isDone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<String> getDishNames() {
        return dishNames;
    }

    public void setDishNames(List<String> dishNames) {
        this.dishNames = dishNames;
    }
}
