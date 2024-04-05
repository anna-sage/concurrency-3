// Responsible for taking temperature readings.

import java.io.PrintWriter;
import java.io.File;
import java.util.Random;
import java.util.TreeSet;
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

    // Representing a minute as 60 milliseconds for the purpose of this simulation.
    public final int MINUTE = 60;
    public final int HOUR = 3600; // todo is this necessary?
    public final int DAY = 86400; // todo is this necessary?

    public final int TEN = 10;
    public final int SIXTY = 60; // todo is this necessary?
    public final int MINS_PER_HOUR = 60;
    public final int MINS_PER_DAY = 1440;

    // Other fields.
    private ExecutorService sensors;
    private Random randTemp; // Generates random temperatures.
    private int [] temps; // Temperature reading storage.
    // private int [] tempDiffs; // Temperature differences for 10 min interval.
    private AtomicInteger idGiver;
    ThreadLocal<Integer> myId;

    // Output generators.
    private PrintWriter reportPrinter; // Hourly reports.
    private PrintWriter logPrinter; // Debug log: readings/info.
    private PrintWriter timePrinter; // Debug log: actual time passed.
    private boolean printerError;

    // Debugging assistance.
    public final boolean DEBUGGING = true;

    public Rover() {
        sensors = Executors.newFixedThreadPool(SENSORS);
        randTemp = new Random(0);
        temps = new int [SENSORS];
        // tempDiffs = new int [MINS_PER_HOUR / TEN];
        idGiver = new AtomicInteger(0);
        myId = new ThreadLocal<>();

        printerError = false;
        try {
            reportPrinter = new PrintWriter(new File("./report.txt"));
            logPrinter = new PrintWriter(new File("./rover_debug_log.txt"));
            timePrinter = new PrintWriter(new File("./rover_time_elapsed.txt"));   
        }
        catch (Exception e) {
            printerError = true;
        }
    }

    // Assigns a sensor a unique ID.
    class SetMyId implements Runnable {
        public void run() {
            int id = idGiver.getAndIncrement();
            myId.set(id);
            if (DEBUGGING) logPrinter.println("Sensor obtained ID " + myId.get());
        }
    }

    // Take a temperature reading for the current minute.
    class ReadTemperatures implements Runnable {
        public void run() {
            int reading = randTemp.nextInt(HIGHTEMP - LOWTEMP + 1) + LOWTEMP;
            if (DEBUGGING) logPrinter.println("\t\t\tSensor " + myId.get() + " reads: " + reading);
            temps[myId.get()] = reading;
        }
    }

    // Starts sensors on their tasks.
    public void beginSensors() {
        for (int i = 1; i <= SENSORS; i++) {
            sensors.submit(new SetMyId());
        }

        finish(60);

        // Collect temperature reading tasks to assign.
        ReadTemperatures [] readingTasks = new ReadTemperatures [SENSORS];
        for (int i = 0; i < SENSORS; i++) {
            readingTasks[i] = new ReadTemperatures();
        }

        // Let the rover collect readings for 24 hours.
        int minsPassed = 0;
        int intervalsPassed = 0;
        int hoursPassed = 0;

        int tenMinInterval = 1; // What 10 min interval are we on?
        // int maxTemp = Integer.MIN_VALUE;
        // int minTemp = Integer.MAX_VALUE;
        int maxDiffThisInterval = Integer.MIN_VALUE;
        int maxDiffThisHour = Integer.MIN_VALUE;

        TreeSet<Integer> minFive = new TreeSet<>();
        TreeSet<Integer> maxFive = new TreeSet<>();
        int largestMin = Integer.MAX_VALUE; // Largest value in min set.
        int smallestMax = Integer.MIN_VALUE; // Smallest value in max set.

        while (minsPassed < MINS_PER_DAY) {
            // Debugging log.
            if (DEBUGGING) {
                if (minsPassed % 60 == 0) {
                    logPrinter.println("====== Hour " + hoursPassed + " ======\n");
                }

                if (minsPassed % 10 == 0) {
                    logPrinter.println("\t=== Interval " + (intervalsPassed % 6) + " ===\n");
                }

                logPrinter.println("\t\t< Minute " + minsPassed + " >\n");
            }
            // Take readings and wait for sensors to finish recording them.
            long start = System.currentTimeMillis();
            for (int i = 0; i < SENSORS; i++) {
                sensors.submit(readingTasks[i]);
            }
            long breakPoint = System.currentTimeMillis();
            finish(MINUTE - (breakPoint - start)); 
            if (DEBUGGING)
                timePrinter.println("Sensors finished reading in " + (breakPoint - start) + " ms.");

            // Find the max and min temps and the largest difference.
            int maxTemp = Integer.MIN_VALUE;
            int minTemp = Integer.MAX_VALUE;
            for (int i = 0; i < SENSORS; i++) {
                if (DEBUGGING) {
                    logPrinter.println("\t\t\tSensor[" + i + "]: " + temps[i]);
                }
                if (temps[i] > maxTemp) {
                    maxTemp = temps[i];
                }
                else if (temps[i] < minTemp) {
                    minTemp = temps[i];
                }
            }

            if (DEBUGGING) 
                logPrinter.println("\t\t\tLargest diff: " + maxTemp + 
                                    " - " + minTemp + " = " + (maxTemp - minTemp) + "\n");

            if (DEBUGGING) 
                logPrinter.println("\t\t\tChecking " + minTemp + " against " + largestMin);

            // Update top and bottom 5 if necessary.
            if (minTemp < largestMin) {
                if (minFive.size() == 5) {
                    minFive.remove(largestMin);
                    minFive.add(minTemp);
                    // Set largestMin to the largest in the set.
                    largestMin = minFive.last();
                    logPrinter.println("largestMin is now " + largestMin);
                }
            }

            if (maxTemp > smallestMax) {
                if (maxFive.size() == 5) {
                    maxFive.remove(smallestMax);
                    maxFive.add(maxTemp);
                    // Set smallestMax to the smallest in the set.
                    smallestMax = maxFive.first();
                    logPrinter.println("smallestMax is now " + smallestMax);
                }
            }

            // Is this temperature difference the largest we've seen this hour?
            if ((maxTemp - minTemp) > maxDiffThisInterval)
                maxDiffThisInterval = maxTemp - minTemp;

            minsPassed++;

            // 10 minute interval.
            if (minsPassed % 10 == 0) {
                if (maxDiffThisInterval > maxDiffThisHour)
                    maxDiffThisHour = maxDiffThisInterval;

                if (DEBUGGING) 
                    logPrinter.println("\t\tLargest diff this interval: " + maxDiffThisInterval + "\n");

                // Reset the max diff seen this interval.
                maxDiffThisInterval = Integer.MIN_VALUE;
                intervalsPassed++;
            }

            // 1 hour has passed.
            if (minsPassed % 60 == 0) {
                // Generate the report.

                if (DEBUGGING) {
                    logPrinter.println("\tInterval with max diff this hour: " + 999 + "\n");
                    logPrinter.flush();
                    timePrinter.flush();
                }

                // Reset the top and bottom 5 sets.
                minFive.clear();
                maxFive.clear();

                // Reset tracker for max diff this hour.
                maxDiffThisHour = Integer.MIN_VALUE;
                hoursPassed++;
            }

            long end = System.currentTimeMillis();
            if (DEBUGGING)
                timePrinter.println("Minute elapsed in " + (end - start) + " ms.");
        }

        // Take readings and then wait for the current "minute" to end.
        long start = System.currentTimeMillis();
        for (int i = 1; i <= SENSORS; i++) {
            sensors.submit(new ReadTemperatures());
        }
        long breakPoint = System.currentTimeMillis();

        finish(MINUTE - (breakPoint - start));

        // Find max and min readings.
        // sensors.submit(new FindMaxesAndMins());

        long end = System.currentTimeMillis();

        if (DEBUGGING) timePrinter.println("took readings in " + (end - start) + " ms");
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

        if (!r.printerError) {
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
        else {
            System.out.println("Error generating one or more PrintWriters.\n" + 
                                "Please recompile and try again!");
        }
    }
}