package com.rs.aragog;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TinkRequestHelper {




    public String authorizeAppAndGetClientAccessToken(RestTemplate restTemplate) throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        System.out.println("Authorizing app and retrieving access token...");
        HttpHeaders headers = new HttpHeaders();
        //headers.set("X-TP-DeviceID", "testHeaderValue");

        String body = "client_id=ec7ed1b7ab5b4537a1b6b00828055d73&client_secret=e0796805a9e5401ead781979878ca2ba&grant_type=client_credentials&scope=user:create";

        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);

        ResponseEntity response = restTemplate.exchange("https://api.tink.se/api/v1/oauth/token", HttpMethod.POST, requestEntity, String.class);

        return getValueFromJsonField((String) response.getBody(), "access_token");
    }


    public String createUserAndGetUserID(RestTemplate restTemplate, String accessToken, String username) throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        System.out.println("Creating user and retrieving userID...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set("Authorization", "Bearer " + accessToken);

        Map<String, String> body = new HashMap<String, String>();
        body.put("external_user_id", username);
        body.put("market", "SE");
        body.put("locale", "en_US");

        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);

        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/user/create", HttpMethod.POST, requestEntity, String.class);
        return getValueFromJsonField((String) response.getBody(), "user_id");
    }


    public String generateUserAuthorizationCodeAndGetClientAccessToken(RestTemplate restTemplate){
        System.out.println("Generating a user authorization code and retrieving the corresponding client access token...");
        HttpHeaders headers = new HttpHeaders();
        //headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = "client_id=ec7ed1b7ab5b4537a1b6b00828055d73&client_secret=e0796805a9e5401ead781979878ca2ba&grant_type=client_credentials&scope=authorization:grant";

        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);

        ResponseEntity response = restTemplate.exchange("https://api.tink.se/api/v1/oauth/token", HttpMethod.POST, requestEntity, String.class);
        return getValueFromJsonField((String) response.getBody(), "access_token");
    }

    public String[] grantUserAccessAndGetUserAuthorizationCodeAndUrl(RestTemplate restTemplate, String clientAccessToken, String userID){
        System.out.println("Granting user access and retrieving user authorization code and URL...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        headers.set("Authorization", "Bearer " + clientAccessToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("actor_client_id", "df05e4b379934cd09963197cc855bfe9");
        body.add("user_id", userID);
        body.add("id_hint", "Myxxxampleflow111");
        body.add("scope", "authorization:read,authorization:grant,credentials:refresh,credentials:read,credentials:write,providers:read,user:read,provider-consents:read,provider-consents:write");
        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);


        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/oauth/authorization-grant/delegate", HttpMethod.POST, requestEntity, String.class);
        String code = getValueFromJsonField((String) response.getBody(), "code");

        String url = "https://link.tink.com/1.0/transactions/connect-accounts?client_id=ec7ed1b7ab5b4537a1b6b00828055d73&state=333&redirect_uri=http://localhost:8080/callback&authorization_code=" + code + "&market=SE&locale=en_US";
        System.out.println(url);

        return new String[]{code, url};
    }

    public String getUserAuthorizationCode(RestTemplate restTemplate, String clientAccessToken, String userID){
        System.out.println("Retrieving user authorization code...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        headers.set("Authorization", "Bearer " + clientAccessToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("user_id", userID);
        body.add("scope", "accounts:read,balances:read,transactions:read,provider-consents:read,credentials:refresh,credentials:write,credentials:read,provider-consents:write");
        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);


        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/oauth/authorization-grant", HttpMethod.POST, requestEntity, String.class);
        String code = getValueFromJsonField((String) response.getBody(), "code");


        return code;
    }

    public String exchangeUserAuthorizationCodeForUserAccessToken(RestTemplate restTemplate, String clientAccessToken, String code){
        System.out.println("Retrieving user access token...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        headers.set("Authorization", "Bearer " + clientAccessToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", "ec7ed1b7ab5b4537a1b6b00828055d73");
        body.add("client_secret", "e0796805a9e5401ead781979878ca2ba");
        body.add("grant_type", "authorization_code");

        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);


        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/oauth/token", HttpMethod.POST, requestEntity, String.class);
        String userAccessToken = getValueFromJsonField((String) response.getBody(), "access_token");


        return userAccessToken;
    }

    public String listTransactions(RestTemplate restTemplate, String userAccessToken){
        System.out.println("Retrieving a list of all transactions...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        headers.set("Authorization", "Bearer " + userAccessToken);


        HttpEntity<Map> requestEntity = new HttpEntity(null, headers);


        ResponseEntity response = restTemplate.exchange("https://api.tink.com/data/v2/transactions", HttpMethod.GET, requestEntity, String.class);
        String transactions = (String) response.getBody();


        return transactions;
    }

    public String checkTransactionsUpdateStatus(RestTemplate restTemplate, String userAccessToken){
        System.out.println("Retrieving update info...");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + userAccessToken);


        HttpEntity<Map> requestEntity = new HttpEntity(null, headers);


        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/provider-consents", HttpMethod.GET, requestEntity, String.class);
        String updateInfo = (String) response.getBody();


        return updateInfo;
    }

    public String updateTransactions(RestTemplate restTemplate, String userAccessToken, String credentialsID){
        System.out.println("Updating transactions...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set("Authorization", "Bearer " + userAccessToken);

        Map<String, Boolean> body = new HashMap<String, Boolean>();
        body.put("userAvailableForInteraction", false);
        body.put("userPresent", true);


        HttpEntity<Map> requestEntity = new HttpEntity(null, headers);


        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/credentials/" + credentialsID + "/refresh", HttpMethod.POST, requestEntity, String.class);
        String updateInfo = (String) response.getBody();


        return updateInfo;
    }

    public String queryTransactions(RestTemplate restTemplate, String userAccessToken, String accountId){
        System.out.println("Querying transactions...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set("Authorization", "Bearer " + userAccessToken);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("includeUpcoming", true);



        //body.put("startDate", Long.valueOf(1665255335217L));
        body.put("sort", "DATE");
        body.put("order", "DESC");
        //body.put("endDate", Long.valueOf(1664877600000L));

        if(accountId != null) {
            JSONArray accounts = new JSONArray();
            accounts.add(accountId);
            body.put("accounts", accounts);
        }

        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);

        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/search", HttpMethod.POST, requestEntity, String.class);
        String updateInfo = (String) response.getBody();

        return updateInfo;
    }

    public long getEpochTimeOfThisMorning() {


        //String str = "Jun 13 2003 23:11:52.454 GMT";
        //SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
        String str = formatter.format(date);
        System.out.print("Current date: "+str);
 /*       try {
            date = df.parse(str);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

  */
        long epoch = date.getTime();
        System.out.println(epoch); // 1055545912454
        return epoch;
    };


    public String updateConsent(RestTemplate restTemplate, String userAccessToken){
        System.out.println("Querying transactions...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set("Authorization", "Bearer " + userAccessToken);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("includeUpcoming", false);
        body.put("startDate", Long.valueOf(1664582400000L));
        body.put("endDate", Long.valueOf(1664640000000L));

        HttpEntity<Map> requestEntity = new HttpEntity(body, headers);


        ResponseEntity response = restTemplate.exchange("https://link.tink.com/1.0/transactions/update-consent?client_id={YOUR_CLIENT_ID}&redirect_uri={YOUR_REDIRECT_URL}&credentials_id={THE_CREDENTIALS_ID}&authorization_code={TINK_LINK_AUTHORIZATION_CODE}&market={YOUR_MARKET_CODE}", HttpMethod.GET, requestEntity, String.class);
        String updateInfo = (String) response.getBody();


        return updateInfo;
    }


    public String listAccounts(RestTemplate restTemplate, String userAccessToken){
        System.out.println("Retrieving a list of all accounts...");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        headers.set("Authorization", "Bearer " + userAccessToken);


        HttpEntity<Map> requestEntity = new HttpEntity(null, headers);


        ResponseEntity response = restTemplate.exchange("https://api.tink.com/api/v1/accounts/list", HttpMethod.GET, requestEntity, String.class);
        String transactions = (String) response.getBody();


        return transactions;
    }


    public String getValueFromJsonField(String json, String fieldName) {
        //System.out.println("trying to get value from: " + json);
        // parsing json
        Object obj = null;
        try {obj = new JSONParser().parse(json);} catch (ParseException e) {e.printStackTrace();}

        // typecasting obj to JSONObject
        JSONObject jo = (JSONObject) obj;

        // getting firstName and lastName
        String accessToken = (String) jo.get(fieldName);

        //System.out.println(accessToken);
        return accessToken;
    }

}
