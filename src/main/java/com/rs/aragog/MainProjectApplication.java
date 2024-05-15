package com.rs.aragog;

import com.rs.aragog.controller.FirstController;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication
public class MainProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainProjectApplication.class, args);
    }

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    FirstController tinkCallerController;

    private final double PURCHASE_PRICE = -1.0;
    private final String PURCHASE_MESSAGE = "Test7";

    private ArrayList<String> deviceWithPendingPurchaseList;
    private long timeOfLatestTransactionProcessed;
    private String idOfLatestTransactionProcessed;

    //@EventListener(ApplicationReadyEvent.class)
    public void doAfterStartUp(){

        setUpRefreshCredentialsTimerTask();
        getEpochTimeOfThisMorning();
        // TODO: sätt den dynamiskt
        timeOfLatestTransactionProcessed = 1664791200000L;
        idOfLatestTransactionProcessed = null;

        deviceWithPendingPurchaseList = new ArrayList<>();

        System.out.println("Application started...");

        // TODO: 1. Initialize or wait for Tink initialization if authorization requires manual input
        /* This is how I did the init with manual auth, but perhaps I'll just always do it through the controller methods
        try {
            tinkCallerController.generateUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }

        while(tinkCallerController.hasCallbackHappened() == false){

        }

        tinkCallerController.getUserAccessToken();
         */
        tinkCallerController.loadPreviousCredentials();
        tinkCallerController.refreshCredentials();


        while( true ) {
            //TODO: 2. Get transactions

            String queryResults = tinkCallerController.queryTransactions();

            // TODO: 3. Sort transactions and find any new and matching transactions

            sortThroughAllQueriedTransactions(queryResults);

            // TODO: 4. If transactions contain new payment then send message to corresponding device

            while( !deviceWithPendingPurchaseList.isEmpty() ) {
                tinkCallerController.writeToFile("Payment-recieved-" + UUID.randomUUID(), "Message in Swish: " + deviceWithPendingPurchaseList.get(0));
                System.out.println("Sending message to device " + deviceWithPendingPurchaseList.remove(0));

            }

            // TODO: 5. Then update transactions and poll for when it has updated

            String checkUpdateResponse = tinkCallerController.checkUpdateStatus();
            long latestStatusUpdate = getLatestStatusUpdate(checkUpdateResponse);
            tinkCallerController.updateTransactions();
            checkUpdateResponse = tinkCallerController.checkUpdateStatus();
            while ( !getUpdateStatus(checkUpdateResponse).equals("UPDATED") && getLatestStatusUpdate(checkUpdateResponse) > latestStatusUpdate) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    checkUpdateResponse = tinkCallerController.checkUpdateStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    tinkCallerController.writeToFile("Status update exception-" + UUID.randomUUID(), "Message in exceptions: " + e.getMessage() +
                            "\n\n" + "exception caught at: " +  LocalDateTime.now() );
                }
            }

            // TODO: 6. Go back to step 2
            System.out.println("Go back to step 2");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setUpRefreshCredentialsTimerTask() {
        // By setting the fixed rate at 6000000 ms/ 6000 s/ 100 min the refresh have 20 minutes time which is more than plenty.
        int refreshRate = 6000000;
        Timer T = new Timer();
        TimerTask refreshCredentials = new TimerTask(){
            @Override
            public void run(){
                // Refresh Credentials
                tinkCallerController.refreshCredentials();
                System.out.println("TimerTask run: refreshes credentials, will run again in " + refreshRate + " ms");
            }
        };
        T.scheduleAtFixedRate(refreshCredentials, 0, refreshRate);
    }

    public String getUpdateStatus(String checkUpdateResponse){


        System.out.println(checkUpdateResponse);

        Object obj = JSONValue.parse(checkUpdateResponse);
        JSONObject updateStatusAsJson = (JSONObject) obj;

        System.out.println("json convert");
        JSONArray providerConsentsJSONArray = (JSONArray) updateStatusAsJson.get("providerConsents");

        System.out.println(providerConsentsJSONArray);
        System.out.println(providerConsentsJSONArray.get(0));

        Object providerConsent = JSONValue.parse(providerConsentsJSONArray.get(0).toString());
        JSONObject providerConsentAsJson = (JSONObject) providerConsent;
        String status = (String) providerConsentAsJson.get("status");
        System.out.println("status: " +status);

        return status;
    }

    public long getLatestStatusUpdate(String checkUpdateResponse){

        System.out.println(checkUpdateResponse);

        Object obj = JSONValue.parse(checkUpdateResponse);
        JSONObject updateStatusAsJson = (JSONObject) obj;

        System.out.println("json convert");
        JSONArray providerConsentsJSONArray = (JSONArray) updateStatusAsJson.get("providerConsents");

        System.out.println(providerConsentsJSONArray);
        System.out.println(providerConsentsJSONArray.get(0));

        Object providerConsent = JSONValue.parse(providerConsentsJSONArray.get(0).toString());
        JSONObject providerConsentAsJson = (JSONObject) providerConsent;
        long statusUpdated = (long) providerConsentAsJson.get("statusUpdated");
        System.out.println("statusUpdated: " +statusUpdated);

        return statusUpdated;
    }

    public void sortThroughAllQueriedTransactions(String queryResponse) {


        Object obj = JSONValue.parse(queryResponse);
        JSONObject queryAsJson = (JSONObject) obj;

        System.out.println("json convert");
        JSONArray resultsJSONArray = (JSONArray) queryAsJson.get("results");
        System.out.println(resultsJSONArray);
        System.out.println();


        Iterator iterator = resultsJSONArray.iterator();
        System.out.println("timeOfLatestTransactionProcessed: " + timeOfLatestTransactionProcessed);
        long timeOfCurrentTransaction = 0L;
        long timeOfTransactionToBeSaved = -1L;
        String idOfTransactionToBeSaved = null;

        while( iterator.hasNext() ) {
            JSONObject next = (JSONObject) iterator.next();
            System.out.println(next);
            JSONObject transaction = (JSONObject) next.get("transaction");
            String idOfCurrentTransaction = (String) transaction.get("id");
            if( idOfCurrentTransaction.equals(idOfLatestTransactionProcessed) ) {
                break;
            }
            // TODO: Sätt den senaste tiden som en betalning tagits emot för att användas i nästa query.
            if( isValidTransaction(transaction) ) {

                if( timeOfCurrentTransaction == 0L && idOfTransactionToBeSaved == null ) {
                    timeOfTransactionToBeSaved = (Long) transaction.get("timestamp");
                    idOfTransactionToBeSaved = idOfCurrentTransaction;
                }
                timeOfCurrentTransaction = (Long) transaction.get("timestamp");
                System.out.println("timeOfTransaction: " + timeOfCurrentTransaction);

                if( (timeOfCurrentTransaction >= timeOfLatestTransactionProcessed) ) {
                    validPaymentReceivedAlert();
                    deviceWithPendingPurchaseList.add((String) transaction.get("formattedDescription"));
                } else {
                    timeOfLatestTransactionProcessed = timeOfTransactionToBeSaved;
                    break;
                }
            }
            System.out.println();
        }
        if( timeOfTransactionToBeSaved != -1L ) {
            timeOfLatestTransactionProcessed = timeOfTransactionToBeSaved;
            idOfLatestTransactionProcessed = idOfTransactionToBeSaved;
        }
    }



    public long getEpochTimeOfThisMorning() {


        //String str = "Jun 13 2003 23:11:52.454 GMT";
        //SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
        Date date = new Date(1664791200000L);
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
        String str = formatter.format(date);
        //System.out.println("Current date: "+str);
        System.out.println();
        //System.out.println(date.toInstant());
    /*    try {
            date = df.parse(str);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

     */
        long epoch = date.getTime();
        System.out.println(epoch); // 1055545912454
        return epoch;
    }


    // TODO: null-safea och lägg till lite fler fält för checkning som typ "userModifiedAmount" och "amount"
    private boolean isValidTransaction(JSONObject transaction) {
        return transaction.get("originalAmount") != null && transaction.get("formattedDescription") != null
                && (Double)transaction.get("originalAmount") == PURCHASE_PRICE && transaction.get("formattedDescription").equals(PURCHASE_MESSAGE);
    }

    private void validPaymentReceivedAlert(){
        System.out.println();
        System.out.println("¤¤¤¤¤¤¤¤¤¤$$$$$¤¤¤¤¤¤¤¤¤¤");
        System.out.println("Valid payment received, send message to embedded system.");
        System.out.println("¤¤¤¤¤¤¤¤¤¤$$$$$¤¤¤¤¤¤¤¤¤¤");
        System.out.println();
    }
/*
    public JSONArray getValueFromJSONObject(JSONObject obj, String fieldName) {
        (JSONArray) obj.get(fieldName);

        return ;
    }

 */
}
