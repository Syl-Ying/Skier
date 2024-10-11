package org.sylvia;

import java.util.Random;
import java.util.concurrent.BlockingDeque;

public class LiftRideGenerator implements Runnable {

    private int totalEvents;
    private BlockingDeque<LiftRideEvent> eventQueue;
    private Random random = new Random();

    public LiftRideGenerator(BlockingDeque<LiftRideEvent> eventQueue, int totalEvents) {
        this.eventQueue = eventQueue;
        this.totalEvents = totalEvents;
    }

    @Override
    public void run() {
        System.out.println("GeneratorThread: starts to run!");
        for (int i = 0; i < totalEvents; i++) {
            // Generate a random lift ride event
            LiftRideEvent liftRide = new LiftRideEvent(
                    random.nextInt(100000) + 1, // skierID [1, 100000]
                    random.nextInt(10) + 1, // resortID [1, 10]
                    "2024", // seasonID 2024
                    "1", // dayID 1
                    random.nextInt(40) + 1, // liftID [1, 40]
                    random.nextInt(360) + 1// time [1, 360]
            );

            try {
                eventQueue.put(liftRide);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
