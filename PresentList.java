// Author: Anna MacInnis

// List of presents to be accessed concurrently by the minotaur's servants.

import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.lang.Thread;

public class PresentList {
    // Constants.
    public static final int SERVANTS = 4;
    public static final int PRESENTS = 500000;
    public static final long NANO_TO_SEC = 1000000000;

    // Tracker nodes.
    private PNode head; // Points to head sentinel.
    private PNode tail; // Points to tail sentinel.

    private ExecutorService servants;
    private ArrayList<Integer> presentBag; // Bag of presents.
    private PrintWriter printer; // Prints output.
    private boolean usingPrinter;

    // Fields to assist in debugging.
    private static final boolean DEBUGGING = false;
    public ArrayList<Integer> processed;

    public PresentList() {
        // Generate sentinel nodes (do not represent presents in the list).
        tail = new PNode(Integer.MAX_VALUE, null);
        head = new PNode(Integer.MIN_VALUE, tail);

        // Fill the present bag with all the presents.
        presentBag = new ArrayList<>();
        for (int i = 1; i <= PRESENTS; i++)
            presentBag.add(i);

        usingPrinter = true;
        try {
            printer = new PrintWriter(new File("./present_list_log.txt"));
        }
        catch (Exception e) {
            usingPrinter = false;
        }

        // For debugging and proof of correctness.
        // Set keeps track of which presents have been added and deleted.
        processed = new ArrayList<>();

        servants = Executors.newFixedThreadPool(SERVANTS);
    }

    // Task for servants to repeat.
    class ServantsTask implements Runnable {
        public void run() {
            while (presentBag.size() > 0) {
                int pres = getPresentToAdd();
                if (add(pres)) {
                    if (usingPrinter)
                        printer.println("Added present " + pres + " to the list.");
                    else
                        System.out.println("Added present " + pres + " to the list.");

                    if (remove(pres)) {
                        if (usingPrinter)
                            printer.println("Wrote thank you note for present " + pres + ".");
                        else
                            System.out.println("Wrote thank you note for present " + pres + ".");

                        if (DEBUGGING) addProcessed(pres);
                    }
                }
                else {
                    // If adding fails, just toss the present back in the bag.
                    presentBag.add(pres);
                }

                // Flush the printer so that it can write all necessary outputs.
                if (presentBag.size() < 10000 && usingPrinter) printer.flush();
            }
        }
    }

    // Begin thread processes.
    public void beginServants() {
        for (int i = 1; i <= SERVANTS; i++) {
            servants.submit(new ServantsTask());
        }
    }

    // For debugging / proof of correctness.
    // To track whether presents have been processed.
    private synchronized void addProcessed(int pres) {
        processed.add(pres);
    }

    // Get a present id from the bag.
    public synchronized int getPresentToAdd() {
        Random rand = new Random();
        int idx = rand.nextInt(presentBag.size());
        int retVal = presentBag.remove(idx);
        return retVal;
    }

    // Add a present to the list of presents to be processed.
    public boolean add(int presId) {
        head.lock();
        PNode pred = head;
        try {
            PNode cur = pred.next;
            cur.lock();
            try {
                while (cur.id < presId) {
                    pred.unlock();
                    pred = cur;
                    cur = cur.next;
                    cur.lock();
                }
                if (cur.id == presId) {
                    return false;
                }
                PNode newNode = new PNode(presId, cur);
                pred.next = newNode;
                return true;
            }
            finally {
                cur.unlock();
            }
        }
        finally {
            pred.unlock();
        }
    }

    // Removes the present from the list. Effectively the same as writing
    // the thank you note.
    public synchronized boolean remove(int presId) {
        PNode pred = null;
        PNode cur = null;
        head.lock();
        try {
            pred = head;
            cur = pred.next;
            cur.lock();
            try {
                while (cur.id < presId) {
                    pred.unlock();
                    pred = cur;
                    cur = cur.next;
                    cur.lock();
                }
                if (cur.id == presId) {
                    pred.next = cur.next;
                    return true;
                }
                return false;
            }
            finally {
                cur.unlock();
            }
        }
        finally {
            pred.unlock();
        }
    }

    // Checks whether the given present is in the list.
    // Technically not necessary for this implementation.
    public boolean contains(int pres) {
        PNode cur = null;
        head.lock();
        try {
            cur = head;
            while (cur.id < pres) {
                cur.unlock();
                cur = cur.next;
                cur.lock();
            }
            if (cur.id == pres)
                return true;
            else
                return false;
        }
        finally {
            cur.unlock();
        }
    }

    // Node representing a single present.
    private class PNode {
        // Fields:
        public int id;
        public PNode next;
        public boolean isDeleted;
        public ReentrantLock lock;

        public PNode(int presId, PNode nextNode) {
            id = presId;
            next = nextNode;
            isDeleted = false;
            lock = new ReentrantLock();
        }

        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }
    }

    // Main driver function.
    public static void main(String [] args) {
        PresentList p = new PresentList();

        // Time how long it takes servants to write thank you notes.
        long start = System.nanoTime();
        p.beginServants();
        p.servants.shutdown();
        try {
            // Generous wait time in the very slim change the PrintWriter
            // throws an exception and outputs rely on the slower System.out.println operation.
            if (!p.servants.awaitTermination(360, TimeUnit.SECONDS))
                p.servants.shutdownNow();
        }
        catch (Exception e) {
            p.servants.shutdownNow();
        }
        long end = System.nanoTime();

        System.out.println("Problem 1 completed in " + ((end - start) / NANO_TO_SEC) + " seconds.");

        if (DEBUGGING) {
            // Output to verify that all presents were processed.
            int amtProcessed = p.processed.size();
            System.out.println("Amount of presents processed: " + amtProcessed);

            // Make sure there were no duplicates processed.
            HashSet<Integer> dupeChecker = new HashSet<>(p.processed);
            if (dupeChecker.size() < amtProcessed)
                System.out.println("Oh no... duplicates were processed");

            if (amtProcessed < PRESENTS) {
                for (int i = 1; i <= PRESENTS; i++) {
                    if (!dupeChecker.contains(i))
                        System.out.println(i + " was never processed.");
                }
            }

            // Another sanity check. Sentinel nodes should be the only ones remaining.
            System.out.println("Should be empty list with only sentinels:");
            PNode cur = p.head;
            while (cur != null) {
                System.out.print(cur.id + " --> ");
                cur = cur.next;
            }
            System.out.println();
        }
    }
}
