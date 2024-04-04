// Author: Anna MacInnis, last updated on 3/12/2024
// Main driver class

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main 
{
    public static final int SERVANTS = 4;
    public static final long NANO_TO_SEC = 1000000000;

    public static void main(String [] args)
    {
        System.out.println("Hello world");

        // Problem 1: The Birthday Presents Party

        // Instantiate present list and present bag.
        PresentList pList = new PresentList();
        HashSet<Integer> pBag = new HashSet<>();
        for (int i = 1; i <= PresentList.PRESENTS; i++) {
            pBag.add(i);
        }

        // Create thread pool of size 4 (minotaur's 4 servants).
        ExecutorService servants = Executors.newFixedThreadPool(SERVANTS);
        long start = System.nanoTime();
        pList.beginServants(servants, SERVANTS);
        servants.shutdown();
        try {
            if (!servants.awaitTermination(60, TimeUnit.SECONDS))
                servants.shutdownNow();
        }
        catch (Exception e) {
            servants.shutdownNow();
        }
        long end = System.nanoTime();

        System.out.println("Problem 1 completed in " + ((end - start) / NANO_TO_SEC) + " seconds");

        System.out.println("HashSet size: " + pList.processed.size());
        for (int i = 1; i <= pList.PRESENTS; i++) {
            if (!(pList.processed.contains(i))) {
                System.out.println(i + " never processed");
            }
        }
    }
}

// Didn't work
// class AddAndProcess implements Runnable {
//     public PresentList listRef;

//     public AddAndProcess(PresentList ref) {
//         listRef = ref;
//     }

//     public void run() {
//         System.out.println("meee");
//         int toAdd = listRef.getPresentToAdd();
//     }
// }
