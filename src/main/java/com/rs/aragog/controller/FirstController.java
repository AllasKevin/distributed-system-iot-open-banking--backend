package com.rs.aragog.controller;

import com.microsoft.azure.sdk.iot.service.FeedbackBatch;
import com.rs.aragog.Device;
import com.rs.aragog.TinkRequestHelper;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

/**
 * The main controller which can both interact with the the banking API via Tink and with the embedded system via Azure.
 */
@RestController
public class FirstController {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    TinkRequestHelper tinkHelper;

    private String userID;
    private String secondClientAccessToken;
    private String userAccessToken;
    private String credentialsID;
    private String accountID;


    @GetMapping("/generate-url")
    public String generateUrl() throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String firstClientAccessToken;
        String userAuthorizationCode;
        String url;
        String[] codeAndUrl;

        Random random = new Random();


        System.out.println("first-method called.");

        firstClientAccessToken = tinkHelper.authorizeAppAndGetClientAccessToken(restTemplate);
        writeToFile("firstClientAccessToken", firstClientAccessToken);

        userID = tinkHelper.createUserAndGetUserID(restTemplate, firstClientAccessToken, "new_java_" + random.nextInt(10000) + "_user_not_used_by_tink");
        System.out.println("userID: " + userID);
        writeToFile("userID", userID);

        secondClientAccessToken = tinkHelper.generateUserAuthorizationCodeAndGetClientAccessToken(restTemplate);
        writeToFile("secondClientAccessToken", secondClientAccessToken);

        codeAndUrl = tinkHelper.grantUserAccessAndGetUserAuthorizationCodeAndUrl(restTemplate, secondClientAccessToken, userID);

        userAuthorizationCode = codeAndUrl[0];
        writeToFile("userAuthorizationCode", userAuthorizationCode);
        System.out.println("first userAuthorizationCode:" + userAuthorizationCode);
        url = codeAndUrl[1];
        writeToFile("url", url);

        return url;
    }

    @GetMapping("/get-user-access-token")
    public String getUserAccessToken() {
        String userAuthorizationCode;

        userAuthorizationCode = tinkHelper.getUserAuthorizationCode(restTemplate, secondClientAccessToken, userID);
        writeToFile("userAuthorizationCode2", userAuthorizationCode);
        System.out.println("second userAuthorizationCode:" + userAuthorizationCode);

        userAccessToken = tinkHelper.exchangeUserAuthorizationCodeForUserAccessToken(restTemplate, secondClientAccessToken, userAuthorizationCode);
        writeToFile("userAccessToken", userAccessToken);
        // This method is called so that the accountID will be saved to current context since it is needed when doing a query.
        listAccounts();
        return userAccessToken;
    }

    @GetMapping("/list-transactions")
    public String listTransactions() {
        String transacationsList;

        transacationsList = tinkHelper.listTransactions(restTemplate, userAccessToken);
        return transacationsList;
    }

    @GetMapping("/check-update-status")
    public String checkUpdateStatus() {
        String updateInfo;

        updateInfo = tinkHelper.checkTransactionsUpdateStatus(restTemplate, userAccessToken);
        return updateInfo;
    }

    @GetMapping("/update-transactions")
    public String updateTransactions() {
        String updateInfo;

        updateInfo = tinkHelper.updateTransactions(restTemplate, userAccessToken, credentialsID);
        return updateInfo;
    }

    @GetMapping("/query-transactions")
    public String queryTransactions() {
        String queryResults;

        queryResults = tinkHelper.queryTransactions(restTemplate, userAccessToken, accountID);
        return queryResults;
    }

    /**
     * Refreshes the credentials
     * @return The new user access token
     */
    @GetMapping("/refresh-credentials")
    public String refreshCredentials() {

        secondClientAccessToken = tinkHelper.generateUserAuthorizationCodeAndGetClientAccessToken(restTemplate);
        writeToFile("secondClientAccessToken", secondClientAccessToken);
        System.out.println("secondClientAccessToken:" + secondClientAccessToken);

        getUserAccessToken();

        return userAccessToken;
    }

    // TODO: sätt konto nummer mer dynamiskt, kanske.
    /**
     * Listing the accounts and saving the internal tink account id of the
     * account that corresponds with kontonummer 90601960408 (mitt privatkonto)
     * @return
     */
    @GetMapping("/list-accounts")
    public String listAccounts() {
        String accounts;

        accounts = tinkHelper.listAccounts(restTemplate, userAccessToken);
        System.out.println(accounts);

        Object obj = JSONValue.parse(accounts);
        JSONObject accountsAsJson = (JSONObject) obj;

        String jsonString = accountsAsJson.toJSONString();
        System.out.println("json convert");
        System.out.println(jsonString);

        JSONArray accountsJSONArray = (JSONArray) accountsAsJson.get("accounts");

        Object objTrans = JSONValue.parse(accountsJSONArray.toJSONString());

        Iterator iterator = accountsJSONArray.iterator();

        while(iterator.hasNext()) {
            JSONObject next = (JSONObject) iterator.next();
            System.out.println(next);
            if(next.get("bankId") != null && next.get("bankId").equals("90601960408")) {
                writeToFile("accountID", next.get("id").toString());
                accountID = next.get("id").toString();
            }
        }

        return accounts;
    }

    Device frodo;
    @GetMapping("/create-device")
    public String createDevice() {
        frodo = new Device("embedded-system-frodo", "HostName=iot-hub-gandalf.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=ioFAXuT+REhzg1qdaPjffFNyO1Hwps/l87HB8AHEIhw=");

        return "Device created.";
    }

    @GetMapping("/send-message-to-device")
    public String sendMessageToDevice() {
        FeedbackBatch feedback = frodo.sendMessage("There is always hope.");

        if (feedback != null) {
            return "Message feedback received, feedback time: " + feedback.getEnqueuedTimeUtc();
        } else {
            return "No feedback received from device probably because it is offline";
        }

    }


    @GetMapping("/callback")
    public String callback(@RequestParam String credentialsId) {
        synchronized(this) {
            System.out.println("credentialsId received in callback was: " + credentialsId);
            this.credentialsID = credentialsId;
            writeToFile("credentialsID", this.credentialsID);
            return credentialsId;
        }
    }

    @GetMapping("/load-previous-credentials")
    public void loadPreviousCredentials() {
        userID = readFromFile("userID");
        secondClientAccessToken = readFromFile("secondClientAccessToken");
        userAccessToken = readFromFile("userAccessToken");
        credentialsID = readFromFile("credentialsID");
        accountID = readFromFile("accountID");
    }

    public boolean hasCallbackHappened(){
        synchronized(this) {
            if(this.credentialsID == null) {
                return false;
            } else {
                return true;
            }
        }

    }


    public void writeToFile(String filename, String msg) {
        try {
            FileWriter myWriter = new FileWriter(filename + ".txt");
            myWriter.write(msg);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public String readFromFile(String filename){
        String data = null;
        try {
            File myObj = new File(filename + ".txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                data = myReader.nextLine();
                System.out.println(data);
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return data;
    }
}
