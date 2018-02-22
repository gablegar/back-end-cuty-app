package utils;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import model.ClientModel;
import model.SessionIdModel;
import model.ShopModel;
import model.ShopServicesModel;

public class ModelsFromDatabase {

    private static final String NUMBER_ID = "{numberID}";
    private static final String QUERY_FILTER_SHOP_OR_CLIENT =
            "{" + "\"size\": 1," + "\"filter\": {" + "\"bool\": {" + "\"must\": [{" + "\"term\": {"
                    + "\"id\": \"" + NUMBER_ID + "\"" + "}" + "}]" + "}" + "}" + "}";

    private static final String ELASTIC_SHOP_INDEX = "/shops/beautyshop/_search";
    private static final String ELASTIC_SESSION_SEARCH = "/cutyapp/session/" + NUMBER_ID;
    private static final String ELASTIC_CLIENT_SEARCH = "/cutyapp/users/" + NUMBER_ID;

    private static final String ELASTIC_ID = "_id";
    private static final String ELASTIC_SOURCE = "_source";
    private static Gson gson = new Gson();

    public static ShopModel getShopModel(String shopId) {
        String queryES = QUERY_FILTER_SHOP_OR_CLIENT.replace(NUMBER_ID, shopId);
        ShopModel shopModel = new ShopModel();
        try {
            JsonObject shopResponse = AppUtils.postElasticResponseSource(queryES, ELASTIC_SHOP_INDEX);
            shopModel = gson.fromJson(shopResponse, ShopModel.class);
        }
        catch (Exception e) {
            System.out.println("Error reading shop info " + e);
        }
        return shopModel;
    }

    public static String getShopsJson(Map queryParams) {
        String shopsJson = "";
        try {
            JsonArray shopsResponse = AppUtils.getDirectElasticResponseListHits(ELASTIC_SHOP_INDEX);
            JsonObject shopsExtracted = new JsonObject();
            for(JsonElement shop : shopsResponse){
                shopsExtracted.add(shop.getAsJsonObject().get(ELASTIC_ID).getAsString(),shop.getAsJsonObject().getAsJsonObject(
                        ELASTIC_SOURCE));
            }
            shopsJson = shopsExtracted.toString();
        }
        catch (Exception e) {
            System.out.println("Error reading shops info " + e);
        }
        return shopsJson;
    }

    public static ClientModel getClientFromSession(String sessionID) {
        ClientModel clientModel= null;
        try {
            String sessionPath = ELASTIC_SESSION_SEARCH.replace(NUMBER_ID, sessionID);
            JsonObject sessionRawJson = AppUtils.getDirectElasticResponseSource(sessionPath);
            SessionIdModel sessionIdModel = gson.fromJson(sessionRawJson, SessionIdModel.class);
            String clientPath = ELASTIC_CLIENT_SEARCH.replace(NUMBER_ID, sessionIdModel.getClientId());
            JsonObject clientRawJson = AppUtils.getDirectElasticResponseSource(clientPath);
            clientModel = gson.fromJson(clientRawJson,ClientModel.class);
        }
        catch (Exception e) {
            System.out.print("Error retrieving client from db " + e);
        }
        return clientModel;
    }

    public static ShopServicesModel getServiceModel(String serviceID, List<ShopServicesModel> services){
        for(ShopServicesModel service : services){
            if(service.getId().equals(serviceID)){
                return service;
            }
        }
        return null;
    }
}
