package model;

public class CalendarRequestModel {

    String date;
    String shopId;
    String serviceId;
    String numberOfDays;
    boolean monthRequest;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public boolean isMonthRequest() {
        return monthRequest;
    }

    public void setMonthRequest(boolean monthRequest) {
        this.monthRequest = monthRequest;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(String numberOfDays) {
        this.numberOfDays = numberOfDays;
    }
}
