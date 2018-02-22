package model;

public class ShopServicesModel {
    String id;
    String serviceType;
    String numberOfSlots;
    String timeLength;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getNumberOfSlots() {
        return numberOfSlots;
    }

    public void setNumberOfSlots(String numberOfSlots) {
        this.numberOfSlots = numberOfSlots;
    }

    public String getTimeLength() {
        return timeLength;
    }

    public void setTimeLength(String timeLength) {
        this.timeLength = timeLength;
    }
}
