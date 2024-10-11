package org.sylvia;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpPostThread implements Runnable {

    private static final Integer MAX_RETRIES = 5;
    private static final String SERVER_URL = "http://localhost:8080/Server/"; // http://localhost:8080/Server/skiers/12/seasons/2019/day/1/skier/123

    private CountDownLatch latch;
    private BlockingDeque<LiftRideEvent> eventQueue;
    private LinkedBlockingDeque<Record> records;
    private Integer numOfRequestsPerThread;
    private AtomicInteger successfulRequests;
    private AtomicInteger failedRequests;
    private AtomicInteger remainingRequests;


    public HttpPostThread(CountDownLatch latch, BlockingDeque<LiftRideEvent> eventQueue, Integer numOfRequestsPerThread,
                          AtomicInteger successfulRequests, AtomicInteger failedRequests,
                          AtomicInteger remainingRequests, LinkedBlockingDeque<Record> records) {
        this.latch = latch;
        this.eventQueue = eventQueue;
        this.numOfRequestsPerThread = numOfRequestsPerThread;
        this.successfulRequests = successfulRequests;
        this.failedRequests = failedRequests;
        this.remainingRequests = remainingRequests;
        this.records = records;
    }

    /**
     * Make numOfRequestsPerThread times of POST requests.
     * If the POST request is successful, increase successfulRequests, log latency and output to csv.
     * If not, retry for at most 5 times.
     */
    @Override
    public void run() {
        // Create an instance for each thread
        SkiersApi apiInstance = new SkiersApi();
        ApiClient client = apiInstance.getApiClient();
        client.setBasePath(SERVER_URL);

        while (!eventQueue.isEmpty()) {
            Integer requestsToSend = Math.min(numOfRequestsPerThread, remainingRequests.getAndAdd(-numOfRequestsPerThread));
            // Post numOfRequestsPerThread requests
            for (int i = 0; i < requestsToSend; i++) {
                LiftRideEvent liftRideEvent = null;
                try {
                    liftRideEvent = eventQueue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // retry for at most 5 times
                Boolean success = false;
                Integer retries = 0;
                while (!success && retries < MAX_RETRIES) {
                    // Post a new skier to server
                    long startTime = System.currentTimeMillis();
                    try {
                        ApiResponse<Void> res = apiInstance.writeNewLiftRideWithHttpInfo(new LiftRide().liftID(liftRideEvent.getLiftID()).time(liftRideEvent.getTime()),
                                liftRideEvent.getResortID(), liftRideEvent.getSeasonID(),
                                liftRideEvent.getDayID(), liftRideEvent.getSkierID());
                        int statusCode = res.getStatusCode();
                        if (statusCode == 201) {
                            // If POST request is successful, increase successfulRequests and log latency
                            successfulRequests.incrementAndGet();
                            success = true;
                            long endTime = System.currentTimeMillis();
                            long duration = endTime - startTime;
                            // Output to CSV
                            records.add(new Record(startTime, "POST", endTime - startTime, res.getStatusCode()));
                        } else if (statusCode >= 400 && statusCode < 600) {
                            retries++;
                            if (retries == MAX_RETRIES) {
                                failedRequests.incrementAndGet();
                            }
                        }
                    } catch (ApiException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Count down after finishes sending POST requests
            latch.countDown();
        }
    }
}
