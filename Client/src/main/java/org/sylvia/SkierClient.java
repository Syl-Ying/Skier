package org.sylvia;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SkierClient {

    private static final Integer INITIAL_THREADS = 32;
    private static final Integer REQUESTS_PER_THREAD = 1000;
    private static final Integer TOTAL_REQUESTS = 200000;

    public static void main(String[] args) throws InterruptedException, IOException {
        BlockingDeque<LiftRideEvent> queue = new LinkedBlockingDeque<>(TOTAL_REQUESTS / 4);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        AtomicInteger remainingRequests = new AtomicInteger(TOTAL_REQUESTS);
        LinkedBlockingDeque<Record> records = new LinkedBlockingDeque<>();

        // Use a single thread to generate random lift events
        Thread generatorThread = new Thread(new LiftRideGenerator(queue, TOTAL_REQUESTS));
        generatorThread.start();
        while (queue.size() < INITIAL_THREADS * REQUESTS_PER_THREAD) {
            Thread.sleep(100); // Wait till the queue is filled with 32000 events
        }

        // Test latency for a single request
        Long latencyOneRequest = testLatency(queue, 1, successfulRequests, failedRequests,
                remainingRequests, records);
        System.out.println("Testing latency for 1 request and 1 thread: " + latencyOneRequest + " ms");
        Long latencyMultipleRequests = testLatency(queue, 10000, successfulRequests,
                failedRequests, remainingRequests, records);
        System.out.println("Testing latency for 10000 requests and 1 thread: " + latencyMultipleRequests + " ms");
        System.out.println("Expected throughput (requests per second): " + (TOTAL_REQUESTS / (latencyOneRequest / 1000.0)));

        long startTime = System.currentTimeMillis();

        // PhaseI: Start initial 32 threads, each sending 1k POST requests
        CountDownLatch initialLatch = new CountDownLatch(1);
        sendPostRequests(queue, INITIAL_THREADS, REQUESTS_PER_THREAD, initialLatch, successfulRequests,
                failedRequests, remainingRequests, records);
        initialLatch.await();
        System.out.println("One of the initial 32 threads has completed. Starting ThreadPoolExecutor...");

        // PhaseII: Once any of the 32 threads finishes, use thread pool
        ThreadPoolExecutor executor = new ThreadPoolExecutor(32,
                128,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>() // unbounded
        );
        while (remainingRequests.get() > 0) {
            executor.submit(new HttpPostThread(initialLatch, queue, 2000, successfulRequests,
                    failedRequests, remainingRequests, records));
        }

        generatorThread.join(); // Wait until generatorThread finishes generating all 200k events
        executor.shutdown(); // Stop accepting new tasks and only executing remaining tasks after generating all 200k events
        executor.awaitTermination(1, TimeUnit.HOURS); // Wait until all tasks are finished
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // System.out.println("Latency for a single request: " + durationTest);
        Integer success = successfulRequests.get();
        Integer failed = failedRequests.get();
        System.out.println("---------------------- Report Client1 ----------------------");
        System.out.println("Number of successful requests: " + success);
        System.out.println("Number of failed requests: " + failed);
        System.out.println("Wall time for all 200k requests: " + duration + " ms");
        System.out.println("Throughput (requests per second): " + ((success + failed) / (duration / 1000.0)));

        System.out.println("Testing latency for 1 request and 1 thread: " + latencyOneRequest + " ms");
        System.out.println("Testing latency for 10000 requests and 1 thread: " + latencyOneRequest + " ms");
        System.out.println("Expected throughput (requests per second): " + (TOTAL_REQUESTS / (latencyOneRequest / 1000.0)));
        System.out.println("----------------------  End  ----------------------");

        new RecordProcessor("./output.csv", records);
    }

    /**
     * Create numberOfThreads threads, each thread sending numberOfRequests POST requests
     * @param queue The queue to store LiftRideEvent produced by LiftRideGenerator thread
     * @param numberOfThreads
     * @param numberOfRequestsPerThread
     * @param latch
     */
    private static void sendPostRequests(BlockingDeque<LiftRideEvent> queue, Integer numberOfThreads,
                                         Integer numberOfRequestsPerThread, CountDownLatch latch,
                                         AtomicInteger successfulRequests, AtomicInteger failedRequests,
                                         AtomicInteger remainingRequests, LinkedBlockingDeque<Record> records) throws InterruptedException {
        for (int i = 0; i < numberOfThreads; i++) {
            Thread httpPostThread = new Thread(new HttpPostThread(latch, queue, numberOfRequestsPerThread,
                    successfulRequests, failedRequests, remainingRequests, records));
            httpPostThread.start();
        }
        System.out.println("Created " + numberOfThreads + " threads.");
    }

    private static Long testLatency(BlockingDeque<LiftRideEvent> queue, Integer numberOfRequestsPerThread,
                                    AtomicInteger successfulRequests, AtomicInteger failedRequests,
                                    AtomicInteger remainingRequests, LinkedBlockingDeque<Record> records) throws InterruptedException {
        System.out.println("Start testing latency.");
        long startTestingTime = System.currentTimeMillis();
        CountDownLatch latchTest = new CountDownLatch(1);
        sendPostRequests(queue, 1, numberOfRequestsPerThread, latchTest, successfulRequests,
                failedRequests, remainingRequests, records);
        latchTest.await();
        long endTestingTime = System.currentTimeMillis();
        long testDuration = endTestingTime - startTestingTime;
        System.out.println("Finish testing latency" );

        // Reset queue, successfulRequests, failedRequests, remainingRequests after testing
        queue.clear();
        successfulRequests.set(0);
        failedRequests.set(0);
        remainingRequests.set(TOTAL_REQUESTS);

        return testDuration;
    }
}
