import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import model.BookingsModel;
import model.ClientModel;
import model.RequestForReservationModel;
import model.ShopModel;
import model.ShopServicesModel;
import utils.AppUtils;
import utils.BookingException;
import utils.DataBaseRequestUtils;
import utils.ModelsFromDatabase;

public class SendConfirmation extends HttpServlet {

    private static final int DAY = 0;
    private static final int MONTH = 1;
    private static final int YEAR = 2;
    private final String USER_AGENT = "Mozilla/5.0";
    private final String ELASTIC_BOOKING_INDEX_CREATE = "/cutyapp/bookings/";

    private Gson gson = new Gson();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String username = "gabriellegardatest@gmail.com";
        final String password = "prodigious";
        ClientModel clientModel = new ClientModel();
        RequestForReservationModel requestObject = getPostData(request);

        ShopModel shopModel = ModelsFromDatabase.getShopModel(requestObject.getShopId());
        clientModel = ModelsFromDatabase.getClientFromSession(requestObject.getSessionId());

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            if (allDataNeededIsPresent(shopModel, clientModel, requestObject)) {

                ShopServicesModel shopServicesModel=ModelsFromDatabase.getServiceModel(requestObject.getServiceId(),
                        shopModel.getServices());
                LocalDate dateOfBooking = LocalDate.parse(requestObject.getDate(),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                DataBaseRequestUtils.validateBooking(requestObject.getServiceId(), requestObject.getShopId(), clientModel.getId(),
                            shopServicesModel.getNumberOfSlots(), dateOfBooking, requestObject.getHour());

                boolean successCreatingBooking = createBooking(
                        createBookingModel(shopModel, clientModel, requestObject));

                if (successCreatingBooking) {
                    Message messageClient = new MimeMessage(session);
                    messageClient.setFrom(new InternetAddress("gabriellegardatest@gmail.com"));
                    messageClient
                            .setRecipients(Message.RecipientType.TO, InternetAddress.parse(clientModel.getEmail()));
                    messageClient.setSubject(
                            "Reserva en " + shopModel.getName() + " para la fecha " + requestObject.getDate());
                    messageClient.setText(
                            "Se realizo la reserva para el dia " + requestObject.getDate() + " a las " + requestObject
                                    .getHour() + "," + " en " + shopModel.getName() + ", para el servicio de : "
                                    + getServiceName(requestObject.getServiceId(), shopModel.getServices())
                                    + ", la dirección del establecimiento es: " + shopModel.getAddress());

                    Transport.send(messageClient);

                    System.out.println("Done sending email to client");

                    Message messageShop = new MimeMessage(session);
                    messageShop.setFrom(new InternetAddress("gabriellegardatest@gmail.com"));
                    messageShop.setRecipients(Message.RecipientType.TO, InternetAddress.parse(shopModel.getEmail()));
                    messageShop.setSubject(clientModel.getFirstName() + clientModel.getLastName() + " ha hecho reserva"
                            + " para la fecha " + requestObject.getDate());
                    messageShop.setText(
                            "Se realizo la reserva para el dia " + requestObject.getDate() + " a las " + requestObject
                                    .getHour() + "," + " para el servicio de " + getServiceName(
                                    requestObject.getServiceId(), shopModel.getServices()));

                    Transport.send(messageShop);

                    System.out.println("Done sending email to Shop");
                    response.getWriter().write("{\"message\":\"reserva exitosa!\"}");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.getWriter().write("{\"message\":\"Error al realizar la reserva, intenta de nuevo!\"}");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                response.getWriter().write("{\"message\":\"Error en reserva, faltan datos\"}");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);

        } catch (NumberFormatException | IOException | JsonSyntaxException e) {
            System.out.println("Error while reading existing bookings " + e + e.getMessage());
            response.getWriter().write("{\"message\":\"Error while reading existing bookings\"}");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (BookingException e) {
            System.out.println(e);
            response.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.out.println(e);
            response.getWriter().write("{\"message\":\"error interno lo sentimos estamos trabajando en solucionarlo\"}");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private BookingsModel createBookingModel(ShopModel shopModel, ClientModel clientModel,
            RequestForReservationModel requestObject) {
        BookingsModel booking = new BookingsModel();
        booking.setClientId(clientModel.getId());
        booking.setServiceId(requestObject.getServiceId());
        booking.setShopId(shopModel.getId());
        String[] dateParsed = requestObject.getDate().split("/");
        booking.setDay(dateParsed[DAY].replaceFirst("^0+(?!$)", ""));
        booking.setYear(dateParsed[YEAR]);
        booking.setMonth(dateParsed[MONTH].replaceFirst("^0+(?!$)", ""));
        booking.setHour(requestObject.getHour());
        return booking;
    }

    private boolean allDataNeededIsPresent(ShopModel shopModel, ClientModel clientModel,
            RequestForReservationModel requestObject) {
        return shopModel != null && shopModel.getName() != null && shopModel.getAddress() != null
                && clientModel.getFirstName() != null && clientModel.getLastName() != null && requestObject != null
                && requestObject.getDate() != null && requestObject.getDate().split("/").length == 3;
    }

    private String getServiceName(String serviceID, List<ShopServicesModel> services) {
        for (ShopServicesModel service : services) {
            if (service.getId().equals(serviceID)) {
                return service.getServiceType();
            }
        }
        return null;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String username = "gabriellegardatest@gmail.com";
        final String password = "prodigious";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("gabriellegardatest@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("myreservationapp@mailinator.com"));
            message.setSubject("Testing Subject");
            message.setText("Dear Mail Crawler," + "\n\n No spam to my email, please!");

            Transport.send(message);

            System.out.println("Done");

        }
        catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private RequestForReservationModel getPostData(HttpServletRequest req) {
        RequestForReservationModel request = gson
                .fromJson(AppUtils.getJsonDataFromRequest(req), RequestForReservationModel.class);

        return request;
    }

    private boolean createBooking(BookingsModel bookingsModel) {
        try {
            JsonObject rawElasticShopResponse = AppUtils.postElasticResponseJsonObject(ELASTIC_BOOKING_INDEX_CREATE,
                    gson.toJson(bookingsModel, BookingsModel.class));
            if (rawElasticShopResponse.get("created").getAsString().equals("true")) {
                return true;
            }
        }
        catch (Exception e) {
            System.out.print("Error while creating booking in db " + e);
            return false;
        }
        return false;
    }
}

