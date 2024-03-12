// Author: Anna MacInnis, last updated on 3/12/2024
// Main driver class

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
        // todo: write list class and instantiate here

        // Create thread pool of size 4 (minotaur's 4 servants).
        // todo: make the thread pool
        
        // Create tasks to randomly select.
        // todo: task that involves adding then immediately removing
    }
}
