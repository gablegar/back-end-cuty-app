package model;

import java.util.List;

public class ShopModel {

    String id;
    String name;
    String group;
    List<ShopServicesModel> services;
    GeolocationModel geolocation;
    String address;
    List<String> openingHours;
    List<String> openingHours24;
    List<String> closedDays;
    String email;
    String phone;
    String city;
    String manager;
    String score;
    String summary;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<ShopServicesModel> getServices() {
        return services;
    }

    public void setServices(List<ShopServicesModel> services) {
        this.services = services;
    }

    public GeolocationModel getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(GeolocationModel geolocation) {
        this.geolocation = geolocation;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(List<String> openingHours) {
        this.openingHours = openingHours;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getOpeningHours24() {
        return openingHours24;
    }

    public void setOpeningHours24(List<String> openingHours24) {
        this.openingHours24 = openingHours24;
    }

    public List<String> getClosedDays() {
        return closedDays;
    }

    public void setClosedDays(List<String> closedDays) {
        this.closedDays = closedDays;
    }
}
