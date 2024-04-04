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
        for (int i = 1; i <= PresentList.PRESENTS; i++) {
            pBag.add(i);
        }

        // Create thread pool of size 4 (minotaur's 4 servants).
        ExecutorService servants = Executors.newFixedThreadPool(SERVANTS);
        pList.beginServants(servants);
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
