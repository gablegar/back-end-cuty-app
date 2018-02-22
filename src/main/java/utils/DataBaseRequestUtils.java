package utils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public class DataBaseRequestUtils {

    private static final String AGG_DAY = "{day}";
    private static final String AGG_MONTH = "{month}";
    private static final String AGG_YEAR = "{year}";
    private static final String AGG_HOUR = "{hour}";
    private static final String AGG_SHOP_ID = "{shopId}";
    private static final String AGG_SERVICE_ID = "{serviceId}";

    private static final String AGG_BOOKING_DATE_NAME = "agg_bookings";
    private static final String PREFIX_AGG_BOOKING =
            "{\"size\":0, \"query\": { \"bool\": { \"filter\":[ "
                    + "{ \"term\": " + "{ \"year\": \"" + AGG_YEAR + "\" } },"
                    + "{ \"term\": { \"month\": \"" + AGG_MONTH + "\" } },"
                    + "{ \"term\": { \"day\": \"" + AGG_DAY + "\" } },"
                    + "{ \"term\": { \"serviceId\": \"" + AGG_SERVICE_ID + "\" } }, "
                    + "{ \"term\": { \"shopId\": \"" + AGG_SHOP_ID + "\" } } ";
    private static final String AGG_BOOKING_DATE =
            PREFIX_AGG_BOOKING
                    + "] } },\"aggs\":{\"agg_bookings\":{\"terms\":{\"field\":\"hour\",\"order\":{\"_term\":\"asc\"}}}}}";

    private static final String AGG_BOOKING_HOUR_CLIENT =
            PREFIX_AGG_BOOKING + "," + "{ \"term\": { \"hour\": \"" + AGG_HOUR + "\"} }"
                    + " ] } },\"aggs\":{\"agg_bookings\":{\"terms\":{\"field\":\"clientId\",\"order\":{\"_term\":\"asc\"}}}}}";

    private static final String ELASTIC_BOOKINGS_INDEX_SEARCH = "/cutyapp/bookings/_search";

    public static List<String> getHoursOfADateThatAreFull(String serviceId, String shopId, String numberOfSlots,
            LocalDate day) throws NumberFormatException, IOException, JsonSyntaxException {

        String aggToRequestBookedHours = AGG_BOOKING_DATE.replace(AGG_DAY, String.valueOf(day.getDayOfMonth()))
                .replace(AGG_MONTH, String.valueOf(day.getMonthValue()))
                .replace(AGG_YEAR, String.valueOf(day.getYear())).replace(AGG_SERVICE_ID, serviceId)
                .replace(AGG_SHOP_ID, shopId).replace(AGG_HOUR, shopId);
        JsonArray hoursTurnsBooked = AppUtils
                .postElasticAggBucketsResponse(aggToRequestBookedHours, ELASTIC_BOOKINGS_INDEX_SEARCH,
                        AGG_BOOKING_DATE_NAME);
        List<String> hoursFullBooked = new ArrayList<>();
        for (JsonElement element : hoursTurnsBooked) {
            if (element.getAsJsonObject().get("doc_count").getAsInt() >= Integer.valueOf(numberOfSlots)) {
                hoursFullBooked.add(element.getAsJsonObject().get("key").getAsString());
            }
        }
        return hoursFullBooked;
    }

    public static void validateBooking(String serviceId, String shopId, String clientId, String numberOfSlots, LocalDate day,
            String hour) throws NumberFormatException, IOException, JsonSyntaxException, BookingException {
        String aggToRequestBookedHours = AGG_BOOKING_HOUR_CLIENT.replace(AGG_DAY, String.valueOf(day.getDayOfMonth()))
                .replace(AGG_MONTH, String.valueOf(day.getMonthValue()))
                .replace(AGG_YEAR, String.valueOf(day.getYear())).replace(AGG_SERVICE_ID, serviceId)
                .replace(AGG_SHOP_ID, shopId)
                .replace(AGG_HOUR, hour);
        JsonArray hoursTurnsBooked = AppUtils
                .postElasticAggBucketsResponse(aggToRequestBookedHours, ELASTIC_BOOKINGS_INDEX_SEARCH,
                        AGG_BOOKING_DATE_NAME);
        List<String> hoursFullBooked = new ArrayList<String>();
        int count = 0;
        for (JsonElement element : hoursTurnsBooked) {
            count += element.getAsJsonObject().get("doc_count").getAsInt();
            if (count >= Integer.valueOf(numberOfSlots)) {
                throw new BookingException("Lo sentimos el horario se encuentra lleno");
            }
            if(clientId.equals(element.getAsJsonObject().get("key").getAsString())){
                throw new BookingException("Ya tienes una reserva para esta hora");
            }

        }
    }

}
