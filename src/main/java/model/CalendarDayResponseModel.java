package model;

import java.util.List;

public class CalendarDayResponseModel {
    String day;
    String dateDay;
    List<CalendarDayHoursModel> hoursModels;

    public CalendarDayResponseModel(String day, String dateDay, List<CalendarDayHoursModel> hours){
        setDay(day);
        setHoursModels(hours);
        setDateDay(dateDay);
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public List<CalendarDayHoursModel> getHoursModels() {
        return hoursModels;
    }

    public void setHoursModels(List<CalendarDayHoursModel> hours) {
        this.hoursModels = hours;
    }

    public String getDateDay() {
        return dateDay;
    }

    public void setDateDay(String dateDay) {
        this.dateDay = dateDay;
    }
}
