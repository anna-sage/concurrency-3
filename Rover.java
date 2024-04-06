// Responsible for taking temperature readings.

import java.io.PrintWriter;
import java.io.File;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

import java.lang.Thread;

public class Rover {

    // Constants.
    public static final int SENSORS = 8;
    public final int LOWTEMP = -100;
    public final int HIGHTEMP = 70;

    public final int MINS_PER_INTERVAL = 10;
    public final int MINS_PER_HOUR = 60;
    public final int MINS_PER_DAY = 1440;

    // For the purposes of this simulation, a "minute" is 60 ms.
    public final int MINUTE = 60;
    public final boolean SIMULATING = true;

    // Fields for making and processing readings.
    private ExecutorService [] sensors;
    private Random randTemp; // Generates random temperatures.
    private int [] temps; // Temperature reading storage.
    ThreadLocal<Integer> myId;

    private int minsPassed;
    private int intervalsPassed;
    private int maxDiffThisInterval;
    private int maxDiffThisHour;
    private int intervalWithMaxDiff;

    private TreeSet<Integer> maxFive;
    private TreeSet<Integer> minFive;
    private int largestMin; // Largest value in min set.
    private int smallestMax; // Smallest value in max set.

    // Output generators.
    private PrintWriter reportPrinter; // Hourly reports.
    private PrintWriter logPrinter; // Debug log: readings/info.
    private PrintWriter timePrinter; // Debug log: actual time passed.
    private boolean printerError;

    // Enables or disables more detailed logs.
    public final boolean DEBUGGING = true;

    public Rover() {
        // Make an array of sensors.
        sensors = new ExecutorService [SENSORS];
        for (int i = 0; i < SENSORS; i++)
            sensors[i] = Executors.newSingleThreadExecutor();

        randTemp = new Random(0);
        temps = new int [SENSORS];
        myId = new ThreadLocal<>();

        minsPassed = 0;
        intervalsPassed = 0;
        maxDiffThisInterval = Integer.MIN_VALUE;
        maxDiffThisHour = Integer.MIN_VALUE;
        intervalWithMaxDiff = Integer.MIN_VALUE; // Dummy value for now.

        maxFive = new TreeSet<>();
        minFive = new TreeSet<>();
        largestMin = Integer.MAX_VALUE;
        smallestMax = Integer.MIN_VALUE;

        printerError = false;
        try {
            reportPrinter = new PrintWriter(new File("./report.txt"));
            logPrinter = new PrintWriter(new File("./logs/rover_debug_log.txt"));
            timePrinter = new PrintWriter(new File("./logs/rover_time_elapsed.txt"));   
        }
        catch (Exception e) {
            printerError = true;
        }
    }

    // Assign unique IDs to sensors.
    private void assignSensorIds() {
        AtomicInteger idGiver = new AtomicInteger(0);

        CountDownLatch needsId = new CountDownLatch(SENSORS);
        for (int i = 0; i < SENSORS; i++) {
            sensors[i].submit(() -> {
                myId.set(idGiver.getAndIncrement());
                needsId.countDown();
            });
        }

        // Ensure all threads finish obtaining IDs.
        try {
            needsId.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    // Mars rover takes readings over a 24 hour period.
    public void takeReadings() {
        while (minsPassed < MINS_PER_DAY) {
            long start = System.currentTimeMillis();
            tasksForCurrentMinute();
            long end = System.currentTimeMillis();

            if (DEBUGGING)
                timePrinter.println("Tasks for current minute completed in " + 
                                    (end - start) + " milliseconds.");

            // If we are simulating taking readings, wait for the current minute to end.
            if (SIMULATING) {
                // Ensure we don't wait for a negative amount of time.
                long sleepTime = (MINUTE - (end - start)) < 1 ? 1 : (MINUTE - (end - start));

                try {
                    Thread.sleep(sleepTime);
                }
                catch (InterruptedException e) {
                    System.out.println("Thread.sleep threw an exception " + e);
                }
            }
        }
    }

    // Take readings and adjust various trackers for the current minute.
    private void tasksForCurrentMinute() {
        if (DEBUGGING) logPrinter.println("\nMinute " + (minsPassed + 1));

        // Take readings and wait for sensors to finish recording them.
        CountDownLatch sensorsReading = new CountDownLatch(SENSORS);
        for (int i = 0; i < SENSORS; i++) {
            sensors[i].submit(() -> {
                int reading = randTemp.nextInt(HIGHTEMP - LOWTEMP + 1) + LOWTEMP;
                if (DEBUGGING) logPrinter.println("\t\t\tSensor " + myId.get() + " reads: " + reading);
                temps[myId.get()] = reading;

                // Sensor signifies that it's done.
                sensorsReading.countDown();
            });
        }

        // Wait for threads to finish reading.
        try {
            sensorsReading.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        // Find the max and min temperatures for this minute.
        int maxTemp = Integer.MIN_VALUE;
        int minTemp = Integer.MAX_VALUE;
        for (int i = 0; i < SENSORS; i++) {
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

        updateTop5Sets(minTemp, maxTemp); // Update the top 5 sets.

        // Is this temperature difference the largest we've seen this hour?
        if ((maxTemp - minTemp) > maxDiffThisInterval)
            maxDiffThisInterval = maxTemp - minTemp;

        minsPassed++;

        // Checkpoints:
        if ((minsPassed % MINS_PER_INTERVAL) == 0)
            tasksForCurrentInterval();

        if ((minsPassed % MINS_PER_HOUR) == 0)
            tasksForCurrentHour();
    }

    // Considers the values for the top 5 largest/smallest temperatures sets.
    private void updateTop5Sets(int min, int max) {
        minFive.add(min);
        if (minFive.size() > 5)
            minFive.pollLast(); // Remove the largest.

        maxFive.add(max);
        if (maxFive.size() > 5)
            maxFive.pollFirst();
    }

    // Updates trackers pertaining to the current interval.
    private void tasksForCurrentInterval() {
        intervalsPassed++;

        if (maxDiffThisInterval > maxDiffThisHour) {
            maxDiffThisHour = maxDiffThisInterval;
            intervalWithMaxDiff = intervalsPassed;
        }

        // Debugging log.
        if (DEBUGGING) {
            logPrinter.println("\nInterval " + intervalsPassed + " passed:");
            logPrinter.println("\tLargest diff this interval: " + maxDiffThisInterval + "\n");
        }

        // Reset the max diff seen this interval.
        maxDiffThisInterval = Integer.MIN_VALUE;
    }

    // Generate the hourly report and update trackers for the current hour.
    private void tasksForCurrentHour() {
        // Generate the report.
        reportPrinter.println("Hour " + (minsPassed / MINS_PER_HOUR) + ":");
        reportPrinter.println("Top 5 highest temperatures:");
        reportPrinter.println("\t" + maxFive);
        reportPrinter.println("Top 5 lowest temperatures:");
        reportPrinter.println("\t" + minFive);
        reportPrinter.println("Interval " + intervalWithMaxDiff + " had the largest " + 
                                "temperature difference of " + maxDiffThisHour + ".\n");
        reportPrinter.flush();

        // Debug log and flush debug printers.
        if (DEBUGGING) {
            logPrinter.println("Hour " + (minsPassed / MINS_PER_HOUR) + " passed.");

            logPrinter.flush();
            timePrinter.flush();
        }

        // Reset the top and bottom 5 sets.
        minFive.clear();
        maxFive.clear();

        // Reset trackers for max diff this hour and amount of intervals.
        maxDiffThisHour = Integer.MIN_VALUE;
        intervalsPassed = 0;
    }

    // Main driver function.
    public static void main(String [] args) {
        Rover r = new Rover();

        if (!r.printerError) {
            long start = System.nanoTime();
            r.assignSensorIds();
            r.takeReadings();
            for (int i = 0; i < SENSORS; i++) {
                r.sensors[i].shutdown();

                try {
                    if (!r.sensors[i].awaitTermination(120, TimeUnit.SECONDS))
                        r.sensors[i].shutdownNow();
                }
                catch (Exception e) {
                    r.sensors[i].shutdownNow();
                } 
            }
        }
        else {
            System.out.println("Error generating one or more PrintWriters.\n" + 
                                "Please recompile and try again!");
        }
    }
}
