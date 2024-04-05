// Responsible for taking temperature readings.

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Rover {

    // Constants.
    public static final int SENSORS = 8;
    public static final long NANO_TO_SEC = 1000000000;
    public final int LOWTEMP = -100;
    public final int HIGHTEMP = 70;

    public final int MINUTE = 60;
    public final int HOUR = 3600;
    public final int DAY = 86400;

    // Other fields.
    private ExecutorService sensors;
    private Random randTemp; // Generates random temperatures.
    private int [] temps; // Temperature reading storage.
    private AtomicInteger idGiver; // todo this thing is broken
    ThreadLocal<Integer> myId; // todo idk man...

    // Debugging assistance.
    public final boolean DEBUGGING = true;

    public Rover() {
        sensors = Executors.newFixedThreadPool(SENSORS);
        randTemp = new Random(0);
        temps = new int [SENSORS];
        idGiver = new AtomicInteger(0);
        myId = new ThreadLocal<>();
        if (DEBUGGING) System.out.println("idGiver is: " + idGiver.get());
    }

    // Assigns a sensor a unique ID.
    class SetMyId implements Runnable {
        public void run() {
            int id = idGiver.getAndIncrement();
            myId.set(id);
            if (DEBUGGING) System.out.println("Thread obtained ID " + myId.get());
        }
    }

    // Take a temperature reading for the current minute.
    class ReadTemperature implements Runnable {
        public void run() {
            int reading = randTemp.nextInt(HIGHTEMP - LOWTEMP + 1) + LOWTEMP;
            if (DEBUGGING) System.out.println("Thread " + Thread.currentThread().getId() + "'s reading: " + reading);
            temps[myId.get()] = reading;
        }
    }

    // Starts sensors on their tasks.
    public void beginSensors() {
        for (int i = 1; i <= SENSORS; i++) {
            sensors.submit(new SetMyId());
        }

        finish(60);

        // Take readings and then wait for the current "minute" to end.
        long start = System.currentTimeMillis();
        for (int i = 1; i <= SENSORS; i++) {
            sensors.submit(new ReadTemperature());
        }
        long breakPoint = System.currentTimeMillis();

        finish(MINUTE - (breakPoint - start));

        // Find max and min readings.
        // sensors.submit(new FindMaxesAndMins());

        long end = System.currentTimeMillis();

        if (DEBUGGING) System.out.println("took readings in " + (end - start) + " ms");
    }

    // Wait for threads to finish some task.
    public void finish(long millisToWait) {
        try {
            sensors.awaitTermination(millisToWait, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
            sensors.shutdownNow();
        }
    }

    // Main driver function.
    public static void main(String [] args) {
        Rover r = new Rover();

        long start = System.nanoTime();
        r.beginSensors();
        r.sensors.shutdown();
        try {
            if (!r.sensors.awaitTermination(120, TimeUnit.SECONDS))
                r.sensors.shutdownNow();
        }
        catch (Exception e) {
            r.sensors.shutdownNow();
        }
    }
}