package com.rs.aragog;

import com.microsoft.azure.sdk.iot.service.*;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A class that represents a device and can be interacted on a high level to send
 * messages to the corresponding physical device.
 */
public class Device {

    String deviceID;
    String connectionString;
    private final IotHubServiceClientProtocol protocol = IotHubServiceClientProtocol.AMQPS;
    ServiceClient serviceClient;
    FeedbackReceiver feedbackReceiver;


    public Device(String deviceID, String connectionString) {
        this.deviceID = deviceID;
        this.connectionString = connectionString;

        serviceClient = new ServiceClient(connectionString, protocol);
        if (serviceClient != null) {
            try {
                serviceClient.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
            feedbackReceiver = serviceClient.getFeedbackReceiver();
        }
    }

    public FeedbackBatch sendMessage(String message) {

        // Preparing message
        Message messageToSend = null;
        try {
            messageToSend = new Message(message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        messageToSend.setDeliveryAcknowledgementFinal(DeliveryAcknowledgement.Full);

        // Sending message
        try {
            serviceClient.send(deviceID, messageToSend);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IotHubException e) {
            e.printStackTrace();
        }
        System.out.println("Message sent to device");

        // Preparing feedbackReceiver
        if (feedbackReceiver != null) {
            try {
                feedbackReceiver.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Receiving the feedback
        FeedbackBatch feedbackBatch = null;
        try {
            feedbackBatch = feedbackReceiver.receive(10000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String feedbackStatus = null;
        if (feedbackBatch != null) {
            feedbackStatus = "Message feedback received, feedback time: "
                    + feedbackBatch.getEnqueuedTimeUtc().toString();
        }

        // Closing the feedbackReceiver and the serviceClient
        if (feedbackReceiver != null) {
            try {
                feedbackReceiver.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            serviceClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return feedbackBatch;
    }


}
