import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import model.CalendarDayHoursModel;
import model.CalendarDayResponseModel;
import model.CalendarRequestModel;
import model.CalendarResponseModel;
import model.ShopModel;
import model.ShopServicesModel;
import model.TimeTableModel;
import utils.AppUtils;
import utils.DataBaseRequestUtils;
import utils.ModelsFromDatabase;

public class Calendar extends HttpServlet {

    private static final String BEGINS_IN_HOUR = "hour";
    private static final String BEGINS_IN_HALF_HOUR = "halfHour";
    Gson gson = new Gson();
    private final String NUMBER_ID = "{numberID}";
    private final String ELASTIC_CALENDAR_INDEX = "/calendar/timetable/" + NUMBER_ID;

    private final int HOUR_MORNING = 0;
    private final int FIRST_HOUR_MORNING = 0;
    private final int LAST_HOUR_AFTERNOON = 1;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String jsonRequest = AppUtils.getJsonDataFromRequest(request);
        CalendarRequestModel calendarRequestModel = gson.fromJson(jsonRequest, CalendarRequestModel.class);

        List<LocalDate> daysToCalculate = getDaysToCalculateFromCurrentDate(calendarRequestModel);

        ShopModel shop = ModelsFromDatabase.getShopModel(calendarRequestModel.getShopId());
        TimeTableModel timeTable = null;
        String hour;
        if (shop != null) {
            ShopServicesModel serviceModel = ModelsFromDatabase
                    .getServiceModel(calendarRequestModel.getServiceId(), shop.getServices());

            if (!shop.getOpeningHours24().isEmpty()) {
                String[] hourMorning = shop.getOpeningHours24().get(HOUR_MORNING).split("-");
                String[] firstHour = hourMorning[FIRST_HOUR_MORNING].split(":");
                if (firstHour[1].equals("00")) {
                    hour = BEGINS_IN_HOUR;
                } else {
                    hour = BEGINS_IN_HALF_HOUR;
                }

                timeTable = getTimeTableForServiceHourAndLength(response, timeTable, hour, serviceModel);
                if(timeTable != null && timeTable.getHours() != null) {
                    CalendarResponseModel requestedDays = getCalendarResponse(response, daysToCalculate, shop,
                            timeTable, serviceModel, hourMorning);
                    response.getWriter().write(gson.toJson(requestedDays, CalendarResponseModel.class).toString());
                } else {
                    response.getWriter().write("la tabla del horario no ha sido creada");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                response.getWriter().write("el establecimiento no tiene horario definido");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            response.getWriter().write("error consultando calendario");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private CalendarResponseModel getCalendarResponse(HttpServletResponse response, List<LocalDate> daysToCalculate,
            ShopModel shop, TimeTableModel timeTable, ShopServicesModel serviceModel, String[] hourMorning) {
        int beginHourPosition = timeTable.getHours().indexOf(hourMorning[FIRST_HOUR_MORNING]);
        timeTable.getHours().subList(0, beginHourPosition).clear();
        int endHourPosition = timeTable.getHours().indexOf(hourMorning[LAST_HOUR_AFTERNOON]);
        timeTable.getHours().subList(endHourPosition, timeTable.getHours().size()).clear();

        CalendarResponseModel requestedDays = new CalendarResponseModel();
        List<String> normalDay = timeTable.getHours();
        for (LocalDate day : daysToCalculate) {
            List<CalendarDayHoursModel> calendarDayHoursModels = constructCalendarDayResponse(response,
                    shop, serviceModel, normalDay, day);

            requestedDays.getDays()
                    .add(new CalendarDayResponseModel(day.format(DateTimeFormatter.ofPattern("dd MMMM")),
                            day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), calendarDayHoursModels));
        }
        return requestedDays;
    }

    private TimeTableModel getTimeTableForServiceHourAndLength(HttpServletResponse response, TimeTableModel timeTable,
            String hour, ShopServicesModel serviceModel) {
        String timeTablePath = ELASTIC_CALENDAR_INDEX
                .replace(NUMBER_ID, hour + "-" + serviceModel.getTimeLength());
        try {
            JsonObject timeTableJson = AppUtils.getDirectElasticResponseSource(timeTablePath);
            timeTable = gson.fromJson(timeTableJson, TimeTableModel.class);
        }
        catch (Exception e) {
            System.out.print("Error retrieving time table" + e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return timeTable;
    }

    private List<CalendarDayHoursModel> constructCalendarDayResponse(HttpServletResponse response, ShopModel shop,
            ShopServicesModel serviceModel, List<String> normalDay, LocalDate day) {
        List<CalendarDayHoursModel> calendarDayHoursModels = new ArrayList<CalendarDayHoursModel>();
        if (!shop.getClosedDays().contains(day.format(DateTimeFormatter.ofPattern("EEEE")))) {
            List<String> hoursFullBooked = new ArrayList<String>();
            try {
                hoursFullBooked = DataBaseRequestUtils
                        .getHoursOfADateThatAreFull(serviceModel.getId(), shop.getId(), serviceModel.getNumberOfSlots(),
                                day);
            } catch (Exception e) {
                System.out.print("Error reading already booked turns " + e);
            }
            for(String hourDay : normalDay){
                if(hoursFullBooked.contains(hourDay)) {
                    calendarDayHoursModels.add(new CalendarDayHoursModel(hourDay, "full"));
                } else {
                    calendarDayHoursModels.add(new CalendarDayHoursModel(hourDay, "free"));
                }
            }
        } else {
            calendarDayHoursModels.add(new CalendarDayHoursModel("cerrado","Not Bookable"));
        }
        return calendarDayHoursModels;
    }

    private List<LocalDate> getDaysToCalculateFromCurrentDate(CalendarRequestModel calendarRequestModel) {
        LocalDate currentDate = LocalDate.parse(calendarRequestModel.getDate(),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE");
        currentDate.format(formatter);
        List<LocalDate> daysToCalculate = new ArrayList<LocalDate>();
        if (calendarRequestModel.getNumberOfDays() != null) {
            for (int counter = 0; counter < Integer.valueOf(calendarRequestModel.getNumberOfDays()); counter++) {
                daysToCalculate.add(currentDate.plusDays(counter));
            }
        }
        return daysToCalculate;
    }


}
