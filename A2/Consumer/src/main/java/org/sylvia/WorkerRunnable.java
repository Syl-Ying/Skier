package org.sylvia;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class WorkerRunnable implements Runnable {

    private Connection connection;
    private Gson gson;
    private Logger logger;
    private ConcurrentHashMap<Integer, List<JsonObject>> skierLiftRidesMap;

    public WorkerRunnable(Connection connection, Gson gson, Logger logger,
                          ConcurrentHashMap<Integer, List<JsonObject>> skierLiftRidesMap) {
        this.connection = connection;
        this.gson = gson;
        this.logger = logger;
        this.skierLiftRidesMap = skierLiftRidesMap;
    }

    @Override
    public void run() {
        try {
            Channel channel = connection.createChannel();
            channel.queueDeclare(Constant.RABBITMQ_NAME, false, false, false, null);
            channel.basicQos(10); // accept only certain unacked message at a time
            // logger.info("Waiting for messages from queue: " + Constant.RABBITMQ_NAME);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info("Received message: " + msg);

                try {
                    doWork(msg);
                } finally {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false); // batch ack
                }
            };

            channel.basicConsume(Constant.RABBITMQ_NAME, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            logger.severe("Failed to consume messages: "+ e.getMessage());
        }
    }

    private void doWork(String msg) {

        JsonObject jsonObject = gson.fromJson(msg, JsonObject.class);
        Integer skierID = Integer.valueOf(String.valueOf(jsonObject.get("skierID")));
        // logger.info("Parsed message into JsonObject: " + jsonObject);

        skierLiftRidesMap.putIfAbsent(skierID, new ArrayList<>());
        synchronized (skierLiftRidesMap.get(skierID)) {
            skierLiftRidesMap.get(skierID).add(jsonObject);
        }

        logger.info("Lift ride recorded for skierID: " + skierID);
    }
}
