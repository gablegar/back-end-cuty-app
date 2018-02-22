package utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class AppUtils {

    private static final String USER_AGENT = "Mozilla/5.0";

    private static final String ELASTIC_NODE = "http://181.50.81.211:9200";
    private static final String ELASTIC_HITS = "hits";
    private static final String ELASTIC_SOURCE = "_source";
    private static Gson gson = new Gson();
    private static final int ID_ARE_UNIQUE = 0;
    private static final int HITS_ARE_UNIQUE = 0;

    public static String getJsonDataFromRequest(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader reader = req.getReader();
            reader.mark(10000);

            String line;
            do {
                line = reader.readLine();
                if (line != null) {
                    sb.append(line).append("\n");
                }
            }
            while (line != null);
            reader.reset();
            // do NOT close the reader here, or you won't be able to get the post data twice
        }
        catch (IOException e) {
            e.printStackTrace();
            //logger.warn("getPostData couldn't.. get the post data", e);  // This has happened if the request's reader is closed
        }
        return sb.toString();
    }


    // HTTP POST request
    public static String sendPost(String path, String param) throws IOException {

        URL obj = new URL(ELASTIC_NODE + path);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setConnectTimeout(1000 * 5);
        //con.setRequestProperty("Accept-Charset", "UTF-8");

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(param);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + path);
        System.out.println("Post parameters : " + param);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }


    // HTTP GET request
    public static String sendGet(String testPath) throws IOException {

        URL obj = new URL(ELASTIC_NODE + testPath);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        con.setConnectTimeout(1000 * 5);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + testPath);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    // HTTP POST request
    public static String sendDelete(String path, String param) throws Exception {

        URL obj = new URL(ELASTIC_NODE + path);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add request header
        con.setRequestMethod("DELETE");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setConnectTimeout(1000 * 5);

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(param);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'DELETE' request to URL : " + path);
        System.out.println("Post parameters : " + param);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }


    // HTTP GET request
    public static int sendGetReturnResponseCode(String path) throws Exception {

        URL obj = new URL(ELASTIC_NODE + path);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setConnectTimeout(1000*5);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + path);
        return responseCode;
    }

    public static JsonObject postElasticResponseJsonObject(String indexPath, String queryES) throws IOException,
            JsonSyntaxException {
        String elasticResponse = AppUtils.sendPost(indexPath, queryES);
        elasticResponse = elasticResponse.replace("\t", "");
        JsonElement elasticResponseElement = gson.fromJson(elasticResponse, JsonElement.class);
        return elasticResponseElement.getAsJsonObject();
    }

    public static JsonObject postElasticResponseSource(String queryES, String indexPath) throws IOException,
            JsonSyntaxException {
        String elasticResponse = AppUtils.sendPost(indexPath, queryES);
        elasticResponse = elasticResponse.replace("\t", "");
        JsonElement elasticResponseElement = gson.fromJson(elasticResponse, JsonElement.class);
        JsonObject rawElasticResponse = elasticResponseElement.getAsJsonObject();
        return rawElasticResponse.getAsJsonObject(ELASTIC_HITS).getAsJsonArray(ELASTIC_HITS).get(ID_ARE_UNIQUE)
                .getAsJsonObject().getAsJsonObject(ELASTIC_SOURCE);
    }

    public static JsonArray postElasticAggBucketsResponse(String queryES, String indexPath, String agg) throws IOException,
            JsonSyntaxException {
        String elasticResponse = AppUtils.sendPost(indexPath, queryES);
        elasticResponse = elasticResponse.replace("\t", "");
        JsonElement elasticResponseElement = gson.fromJson(elasticResponse, JsonElement.class);
        JsonObject rawElasticResponse = elasticResponseElement.getAsJsonObject();
        return rawElasticResponse.getAsJsonObject("aggregations").getAsJsonObject(agg).getAsJsonArray("buckets");
    }

    public static JsonObject getDirectElasticResponseSource(String indexPath) throws IOException,
            JsonSyntaxException {
        JsonObject rawElasticResponse = getRawElasticResponseAsJsonObject(indexPath);
        return rawElasticResponse.getAsJsonObject(ELASTIC_SOURCE);
    }

    public static JsonArray getDirectElasticResponseListHits(String indexPath) throws IOException,
            JsonSyntaxException {
        JsonObject rawElasticResponse = getRawElasticResponseAsJsonObject(indexPath);
        return rawElasticResponse.getAsJsonObject(ELASTIC_HITS).getAsJsonArray(ELASTIC_HITS);
    }

    private static JsonObject getRawElasticResponseAsJsonObject(String indexPath) throws IOException {
        String elasticResponse = AppUtils.sendGet(indexPath);
        elasticResponse = elasticResponse.replace("\t", "");
        JsonElement elasticResponseElement = gson.fromJson(elasticResponse, JsonElement.class);
        return elasticResponseElement.getAsJsonObject();
    }
}
