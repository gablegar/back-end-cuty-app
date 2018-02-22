package model;

import java.util.List;

public class TimeTableModel {

    boolean beginsHour;
    String step;
    List<String> hours;

    public boolean isBeginsHour() {
        return beginsHour;
    }

    public void setBeginsHour(boolean beginsHour) {
        this.beginsHour = beginsHour;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public List<String> getHours() {
        return hours;
    }

    public void setHours(List<String> hours) {
        this.hours = hours;
    }

    @Override
    public String toString() {
        return "{" +
                "\"beginsHour\":\"" + beginsHour +
                "\", \"step\":\"" + step +
                "\", \"hours\":" + hours +
                "}";
    }
}
