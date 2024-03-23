// Author: Anna MacInnis, last updated on 3/12/2024
// Main driver class

import java.util.HashSet;

public class Main 
{
    public static final int PRESENTS = 500000;

    public static void main(String [] args)
    {
        System.out.println("Hello world");

        // Problem 1: The Birthday Presents Party

        // Initialize the presents and the linked list to store them.
        int [] bagOfPresents = new int [PRESENTS];
        for (int i = 0; i < PRESENTS; i++)
            bagOfPresents[i] = i;

        // Instantiate present list and present bag.
        PresentList pList = new PresentList();
        HashSet<Integer> pBag = new HashSet<>();
        for (int i = 1; i <= PresentList.PRESENTS; i++)
        {
            pBag.add(i);
        }

        // Create thread pool of size 4 (minotaur's 4 servants).
        // todo: make the thread pool
        
        // Create tasks to randomly select.
        // todo: task that involves adding then immediately removing
    }
}
