package model;

import java.util.ArrayList;
import java.util.List;

public class CalendarResponseModel {
    List<CalendarDayResponseModel> days;

    public CalendarResponseModel(){
        setDays(new ArrayList<CalendarDayResponseModel>());
    }

    public List<CalendarDayResponseModel> getDays() {
        return days;
    }

    public void setDays(List<CalendarDayResponseModel> days) {
        this.days = days;
    }
}
