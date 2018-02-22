package model;

public class CalendarDayHoursModel {
    String hour;
    String status;

    public CalendarDayHoursModel(String hour, String status) {
        this.hour = hour;
        this.status = status;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
