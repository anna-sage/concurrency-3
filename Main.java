// Author: Anna MacInnis, last updated on 3/12/2024
// Main driver class

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main 
{
    public static final int SERVANTS = 4;

    public static void main(String [] args)
    {
        System.out.println("Hello world");

        // Problem 1: The Birthday Presents Party

        // Instantiate present list and present bag.
        PresentList pList = new PresentList();
        HashSet<Integer> pBag = new HashSet<>();
        for (int i = 1; i <= PresentList.PRESENTS; i++)
        {
            pBag.add(i);
        }

        // Create thread pool of size 4 (minotaur's 4 servants).
        ExecutorService servants = Executors.newFixedThreadPool(SERVANTS);
        
        // Create tasks to randomly select.
        // todo: task that involves adding then immediately removing
        // Randomly adds some present from the bag and removes some present from the list.
        Runnable addAndProcess = () -> {
            // todo: select a random present to process.
            int toAdd = pList.getPresentToAdd();
        };
    }
}
