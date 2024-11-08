package org.sylvia;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class LiftRideConsumer {

    private static final Logger logger = Logger.getLogger(LiftRideConsumer.class.getName());
    private static final Integer NUM_WORKERS = 25;

    public static void main(String[] args) throws IOException, TimeoutException {
        Gson gson = new Gson();
        ConcurrentHashMap<Integer, List<JsonObject>> skierLiftRidesMap = new ConcurrentHashMap<>();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Constant.RABBITMQ_ELASTIC_IP);
        factory.setPort(Constant.RABBITMQ_PORT);
        factory.setUsername(Constant.RABBITMQ_USERNAME);
        factory.setPassword(Constant.RABBITMQ_PASSWORD);
        Connection connection = factory.newConnection();

        ExecutorService pool = Executors.newCachedThreadPool();
        for (int i =0; i < NUM_WORKERS; i++) {
            pool.execute(new WorkerRunnable(connection, gson, logger, skierLiftRidesMap));
        }
    }
}