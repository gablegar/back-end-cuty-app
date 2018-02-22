import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import model.TimeTableModel;
import model.TimeTableRequestModel;
import utils.AppUtils;

public class CalendarTimetableGenerator extends HttpServlet {

    Gson gson = new Gson();
    private final String NUMBER_ID = "{numberID}";
    private final String ELASTIC_CALENDAR_INDEX = "/calendar/timetable/" + NUMBER_ID;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String document;
        String jsonRequest = AppUtils.getJsonDataFromRequest(request);
        TimeTableRequestModel timeTableRequestModel = gson.fromJson(jsonRequest, TimeTableRequestModel.class);

        if (timeTableRequestModel != null && !timeTableRequestModel.getStart().isEmpty() && !timeTableRequestModel
                .getEnd().isEmpty() && !timeTableRequestModel.getStep().isEmpty()) {
            String[] stepSplit = timeTableRequestModel.getStep().split(":");
            long serviceDuration = Long
                    .valueOf((Integer.valueOf(stepSplit[0]) * 60 + Integer.valueOf(stepSplit[1])) * 60 * 1000);
            String[] firstHour = timeTableRequestModel.getStart().split(":");
            int firstHourDigit = Integer.valueOf(firstHour[0]);
            int firstHourMinutes = Integer.valueOf(firstHour[1]);
            boolean hour = false;
            if (firstHour[1].equals("00")) {
                hour = true;
                document = ELASTIC_CALENDAR_INDEX.replace(NUMBER_ID, "hour-" + timeTableRequestModel.getStep());
            } else {
                document = ELASTIC_CALENDAR_INDEX.replace(NUMBER_ID, "halfhour-" + timeTableRequestModel.getStep());
            }
            long firsttime = (firstHourDigit * 60 + firstHourMinutes) * 60 * 1000;

            String[] lastMorningHour = timeTableRequestModel.getEnd().split(":");
            int lastMorningHourDigit = Integer.valueOf(lastMorningHour[0]);
            int lastMorningHourMinutes = Integer.valueOf(lastMorningHour[1]);
            long lastMorningTime = (lastMorningHourDigit * 60 + lastMorningHourMinutes) * 60 * 1000;

            long morningTimeVar = firsttime;
            List<Long> morningTimes = new ArrayList<Long>();
            int counter = 0;
            if (firsttime < lastMorningTime) {
                while (lastMorningTime >= morningTimeVar && counter < 1000) {
                    morningTimes.add(counter, morningTimeVar);
                    counter++;
                    morningTimeVar += serviceDuration;
                }
                List<String> slotsHorarios = new ArrayList<String>();
                for (Long horario : morningTimes) {
                    slotsHorarios.add(combinationFormatter(horario));
                }
                TimeTableModel timeTable = new TimeTableModel();
                timeTable.setHours(slotsHorarios);
                timeTable.setStep(timeTableRequestModel.getStep());
                timeTable.setBeginsHour(hour);
                try {
                    AppUtils.sendPost(document, timeTable.toString());
                }
                catch (Exception e) {
                    System.out.print("Error creating time table " + e);
                }
                response.getWriter().write(slotsHorarios.toString());
            } else {
                response.getWriter().write("Hora de fin menor a hora de comienzo");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.getWriter().write("Parameters start-end-step empty");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private static String combinationFormatter(final long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS
                .toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);

        StringBuilder b = new StringBuilder();
        b.append(hours == 0 ? "00" : hours < 10 ? String.valueOf("0" + hours) : String.valueOf(hours));
        b.append(":");
        b.append(minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) : String.valueOf(minutes));
        return "\"" + b.toString() + "\"";
    }
}
