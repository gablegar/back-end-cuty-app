import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import model.ClientModel;
import model.SessionIdModel;
import utils.AppUtils;
import utils.LoginException;

public class Login extends HttpServlet {

    private static final String CLIENT_EMAIL = "{clientEmail}";
    private final String NUMBER_ID = "{numberID}";
    private final String SESSION_ID = "sessionId";
    private final String CLIENT_ID = "clientId";
    private final int ID_ARE_UNIQUE = 0;

    private final String ELASTIC_CLIENT_INDEX = "/cutyapp/users/_search";
    private final String ELASTIC_CLIENT_INDEX_CREATE = "/cutyapp/users/" + NUMBER_ID;
    private final String ELASTIC_SESSION_INDEX_CREATE = "/cutyapp/session/" + NUMBER_ID;

    private final String QUERY_FILTER_EMAIL_EXIST = "{\"filter\" :{\"term\":{\"email\": \"" + CLIENT_EMAIL + "\"}}}";

    private final String QUERY_FILTER_MAX_CLIENT = "{\"size\":0,\"aggs\":{\"agg_max_client_id\":{\"max\":{\"field\":\"id\"}}}}";

    Gson gson = new Gson();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String jsonRequest = AppUtils.getJsonDataFromRequest(request);
        SessionIdModel sessionIdModel = gson.fromJson(jsonRequest, SessionIdModel.class);
        ClientModel clientModel = gson.fromJson(jsonRequest, ClientModel.class);

        if (isAutomaticLogin(sessionIdModel)) {
            response = searchIfSessionExist(response, sessionIdModel);
        } else if (isRegistration(clientModel)) {
            try {

                Integer actualMaxId = getMaxUserId();
                JsonObject rawElasticShopResponse;

                if (actualMaxId > 0) {
                    if (!clientModel.getEmail().isEmpty()) {
                        String queryEmailExist = QUERY_FILTER_EMAIL_EXIST.replace(CLIENT_EMAIL, clientModel.getEmail());
                        rawElasticShopResponse = AppUtils
                                .postElasticResponseJsonObject(ELASTIC_CLIENT_INDEX, queryEmailExist);
                        if (((int) rawElasticShopResponse.getAsJsonObject("hits").get("total").getAsDouble()) > 0) {
                            throw new LoginException("Ya existe una cuenta con el email ingresado");
                        }
                    } else {
                        throw new LoginException("El email esta vacio");
                    }
                }

                response = createUserFromJsonRequest(response, jsonRequest, actualMaxId);
            }
            catch (LoginException e) {
                response.getWriter().write("" + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            catch (Exception e) {
                System.out.print("Error creating client " + e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else if (isFacebookRegistration(clientModel)){

            try {

                String queryClientByEmail = QUERY_FILTER_EMAIL_EXIST.replace(CLIENT_EMAIL, clientModel.getEmail());
                JsonObject rawElasticShopResponse = AppUtils
                        .postElasticResponseJsonObject(ELASTIC_CLIENT_INDEX, queryClientByEmail);
                ClientModel completeData = null;
                if (((int) rawElasticShopResponse.getAsJsonObject("hits").get("total").getAsDouble()) == 1) {
                    JsonObject elasticClientDetailResponse = rawElasticShopResponse.getAsJsonObject("hits")
                            .getAsJsonArray("hits").get(ID_ARE_UNIQUE).getAsJsonObject().getAsJsonObject("_source");
                    completeData = gson.fromJson(elasticClientDetailResponse, ClientModel.class);
                    String sessionId = getValidSession(completeData.getId());
                    if (sessionId == null) {
                        throw new LoginException("Error al iniciar sesión, por favor intente de nuevo");
                    }
                    response.setHeader("sessionId", sessionId);

                } else {
                    Integer actualMaxId = getMaxUserId();
                    response = createUserFromJsonRequest(response, jsonRequest, actualMaxId);
                }
            }
            catch (LoginException e) {
                response.getWriter().write("" + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            catch (Exception e) {
                System.out.print("Error creando usuario facebook " + e);
                response.getWriter().write("Error creando el usuario");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } else {
            try {
                String queryClientByEmail = QUERY_FILTER_EMAIL_EXIST.replace(CLIENT_EMAIL, clientModel.getEmail());
                JsonObject rawElasticShopResponse = AppUtils
                        .postElasticResponseJsonObject(ELASTIC_CLIENT_INDEX, queryClientByEmail);
                ClientModel completeData = null;
                if (((int) rawElasticShopResponse.getAsJsonObject("hits").get("total").getAsDouble()) == 1) {
                    JsonObject elasticClientDetailResponse = rawElasticShopResponse.getAsJsonObject("hits")
                            .getAsJsonArray("hits").get(ID_ARE_UNIQUE).getAsJsonObject().getAsJsonObject("_source");
                    completeData = gson.fromJson(elasticClientDetailResponse, ClientModel.class);
                } else {
                    response.getWriter().write("Usuario no existe, por favor cree el usuario");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }

                if (completeData != null) {
                    if (!clientModel.getPassword().equals(completeData.getPassword())) {
                        response.getWriter().write("Email o clave no validos");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        String sessionId = getValidSession(completeData.getId());
                        if (sessionId == null) {
                            throw new LoginException("Error al iniciar sesión, por favor intente de nuevo");
                        }
                        response.setHeader("sessionId", sessionId);
                    }
                }

            }
            catch (LoginException e) {
                response.getWriter().write("" + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            catch (Exception e) {
                System.out.print("Error creating client " + e);
                response.getWriter().write("Error creando el usuario");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private Integer getMaxUserId() throws Exception {
        Integer actualMaxId;
        JsonObject rawElasticShopResponse = AppUtils
                .postElasticResponseJsonObject(ELASTIC_CLIENT_INDEX, QUERY_FILTER_MAX_CLIENT);
        if (!rawElasticShopResponse.getAsJsonObject("aggregations").getAsJsonObject("agg_max_client_id").get("value")
                .isJsonNull()) {
            actualMaxId = (int) rawElasticShopResponse.getAsJsonObject("aggregations")
                    .getAsJsonObject("agg_max_client_id").get("value").getAsDouble();
        } else {
            actualMaxId = 0;
        }
        return actualMaxId;
    }

    private HttpServletResponse createUserFromJsonRequest(HttpServletResponse response, String jsonRequest,
            Integer actualMaxId) throws Exception {
        if (actualMaxId != null) {
            int newMaxClientId = actualMaxId + 1;
            JsonElement rawElementRequest = gson.fromJson(jsonRequest, JsonElement.class);
            JsonObject rawElasticRequest = rawElementRequest.getAsJsonObject();
            rawElasticRequest.addProperty("id", newMaxClientId);
            String createURL = ELASTIC_CLIENT_INDEX_CREATE.replace(NUMBER_ID, "" + newMaxClientId);
            AppUtils.postElasticResponseJsonObject(createURL, rawElasticRequest.toString());
            String sessionId = getValidSession("" + newMaxClientId);
            if (sessionId == null) {
                throw new LoginException("Usuario creado pero no se realizo login, intente realizar login");
            }
            response.setHeader("sessionId", sessionId);
            response.getWriter().write("registro ok");
        }
        return response;
    }

    private HttpServletResponse searchIfSessionExist(HttpServletResponse response, SessionIdModel sessionIdModel) {
        try {
            String testURL = ELASTIC_SESSION_INDEX_CREATE.replace(NUMBER_ID, sessionIdModel.getSessionId());
            int responseCode = AppUtils.sendGetReturnResponseCode(testURL);
            if (responseCode == HttpServletResponse.SC_NOT_FOUND) {
                response.getWriter().write("Sesión invalida por favor realizar login de nuevo");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            response.setHeader("authorized", "true");
        }
        catch (Exception e) {
            System.out.print("Error automatic login evaluation of sessionID " + e);
        }
        return response;
    }

    private boolean isFacebookRegistration(ClientModel clientModel) {
        boolean facebookMode = false;
        if (clientModel.getFirstName() != null && clientModel.getLastName() != null && clientModel.getEmail() != null) {
            facebookMode = true;
        }
        return facebookMode;
    }

    private boolean isRegistration(ClientModel clientModel) {
        boolean registrationMode = false;
        if (clientModel.getFirstName() != null && clientModel.getLastName() != null && clientModel.getPassword() != null) {
            registrationMode = true;
        }
        return registrationMode;
    }

    private boolean isAutomaticLogin(SessionIdModel sessionModel) {
        boolean automaticLoginMode = false;
        if (sessionModel.getSessionId() != null) {
            automaticLoginMode = true;
        }
        return automaticLoginMode;
    }

    private String getValidSession(String clientId) {
        long t = System.currentTimeMillis();
        long end = t + 5000;
        String sessionId = null;
        while (System.currentTimeMillis() < end && sessionId == null) {
            sessionId = createSession(clientId);
        }
        return sessionId;
    }

    private String createSession(String clientId) {
        boolean findSuitableNumber = false;
        String sessionId = null;
        String createURL = null;
        while (!findSuitableNumber) {
            String randomId = UUID.randomUUID().toString().replaceAll("-", "");
            String testURL = ELASTIC_SESSION_INDEX_CREATE.replace(NUMBER_ID, randomId);
            try {
                int responseCode = AppUtils.sendGetReturnResponseCode(testURL);
                if (responseCode == HttpServletResponse.SC_NOT_FOUND) {
                    sessionId = randomId;
                    createURL = testURL;
                    findSuitableNumber = true;
                }
            }
            catch (Exception e) {
                System.out.print("Error requesting if sessionId exist " + e);
                return null;
            }
        }

        try {
            JsonObject sessionDocument = new JsonObject();
            sessionDocument.addProperty(SESSION_ID, sessionId);
            sessionDocument.addProperty(CLIENT_ID, clientId);

            JsonObject rawElasticShopResponse = AppUtils
                    .postElasticResponseJsonObject(createURL, sessionDocument.toString());
            if (!rawElasticShopResponse.get("created").getAsString().equals("true")) {
                sessionId = null;
            }
        }
        catch (Exception e) {
            System.out.print("Error while creating session ID in db " + e);
        }
        return sessionId;
    }
}