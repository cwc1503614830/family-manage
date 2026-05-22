package com.example.familymanage.dto;

public class TravelWuhanSpotRequest {

    private String spotName;
    private String spotType;
    private Integer checkedIn;
    private String remark;

    public String getSpotName() {
        return spotName;
    }

    public void setSpotName(String spotName) {
        this.spotName = spotName;
    }

    public String getSpotType() {
        return spotType;
    }

    public void setSpotType(String spotType) {
        this.spotType = spotType;
    }

    public Integer getCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(Integer checkedIn) {
        this.checkedIn = checkedIn;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
