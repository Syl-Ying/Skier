package org.sylvia;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class LiftRideConsumer {

    private static final Map<Integer, List<JsonObject>> skierLiftRidesMap = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(LiftRideConsumer.class.getName());
    private static final Integer NUM_WORKERS = 15;

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Constant.RABBITMQ_ELASTIC_IP);
        factory.setPort(Constant.RABBITMQ_PORT);
        factory.setUsername(Constant.RABBITMQ_USERNAME);
        factory.setPassword(Constant.RABBITMQ_PASSWORD);
        Connection connection = null;
        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            logger.severe("RabbitMQ connection failed.");
        }

        // fair distribute between workers
        for (int i =0; i < NUM_WORKERS; i++) {
            Connection finalConnection = connection;
            new Thread(() -> new Worker(finalConnection).work()).start();
        }

    }

    static class Worker {
        private final Connection connection;

        Worker(Connection connection) {
            this.connection = connection;
        }

        public void work() {
            try {
                Channel channel = connection.createChannel();
                channel.queueDeclare(Constant.RABBITMQ_NAME, false, false, false, null);
                channel.basicQos(10); // accept only 10 unacked message at a time
                // logger.info("Waiting for messages from queue: " + Constant.RABBITMQ_NAME);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    logger.info("Received message: " + msg);

                    doWork(msg);
                    logger.info("Done.");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                };

                channel.basicConsume(Constant.RABBITMQ_NAME, false, deliverCallback, consumerTag -> {});
            } catch (IOException e) {
                logger.severe("Failed to consume messages: "+ e.getMessage());
            }
        }

        private void doWork(String msg) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(msg, JsonObject.class);
            Integer skierID = Integer.valueOf(String.valueOf(jsonObject.get("skierID")));
            logger.info("Parsed message into JsonObject: " + jsonObject + ". skierID" + skierID);

            if (jsonObject != null) {
                skierLiftRidesMap.putIfAbsent(skierID, Collections.synchronizedList(new ArrayList<>()));
                skierLiftRidesMap.get(skierID).add(jsonObject);
                logger.info("Lift ride recorded for skierID: " + skierID);
            } else {
                logger.warning("Failed to parse message: " + msg);
            }
        }
    }
}