import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import model.SessionIdModel;
import utils.AppUtils;

public class Logout extends HttpServlet {

    Gson gson = new Gson();
    private final String NUMBER_ID = "{numberID}";
    private final String ELASTIC_SESSION_INDEX_CREATE = "/cutyapp/session/" + NUMBER_ID;
    private final String NO_PARAM_FOR_DELETING_DOCUMENT = "";

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String jsonRequest = AppUtils.getJsonDataFromRequest(request);
        SessionIdModel sessionIdModel = gson.fromJson(jsonRequest, SessionIdModel.class);
        String deleteSessionEndpoint = ELASTIC_SESSION_INDEX_CREATE.replace(NUMBER_ID, sessionIdModel.getSessionId());
        try{
            AppUtils.sendDelete(deleteSessionEndpoint, NO_PARAM_FOR_DELETING_DOCUMENT);
            response.getWriter().write("{\"message\":\"Logout ok\"}");
        } catch (IOException e){
            System.out.print("Error deleting session " + e);
            response.getWriter().write("{\"message\":\"Session no encontrada pero logout ok\"}");
        } catch (Exception e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            System.out.print("Error deleting session " + e);
        }
    }
}
