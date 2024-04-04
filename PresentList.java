// List of presents to be accessed concurrently by the minotaur's servants.

import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet; // todo delete
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

public class PresentList
{
    // Tracker nodes.
    private PNode head; // Points to sentinel.
    private PNode tail;
    private int size;

    // Amount of presents.
    public static final int PRESENTS = 500000;

    private ArrayList<Integer> presentBag; // Bag of presents.
    private PrintWriter printer; // Prints output.
    private boolean usingPrinter;
    public final long NANO_TO_SEC = 1000000000;

    // todo delete
    private final boolean DEBUGGING = false;
    private ReentrantLock printListLock;
    public HashSet<Integer> processed;

    // Task for servants to repeat.
    class ServantsTask implements Runnable
    {
        public void run()
        {
            while (presentBag.size() > 0) {
                int pres = getPresentToAdd();
                if (add(pres)) {
                    if (usingPrinter) {
                        printer.println("Added present " + pres + " to the list.");
                    }
                    else {
                        System.out.println("Added present " + pres + " to the list.");
                    }

                    if (remove(pres)) {
                        if (usingPrinter) {
                            printer.println("Wrote thank you note for present " + pres + ".");
                        }
                        else {
                            System.out.println("Wrote thank you note for present " + pres + ".");
                        }
                        addGuy(pres);
                    }
                }
                else {
                    System.out.println("bad");
                    presentBag.add(pres);
                }

                if (presentBag.size() < 1000 && usingPrinter) printer.flush();
            }

            if (DEBUGGING) System.out.println("Thread " + Thread.currentThread().getId() + " signing out");
        }
    }

    public PresentList() 
    {
        // Generate first sentinel node (does not represent a present in the list).
        tail = new PNode(PRESENTS + 2, null);
        head = new PNode(PRESENTS + 1, tail);
        size = 2;

        // Fill the present bag with all the presents.
        presentBag = new ArrayList<>();
        for (int i = 1; i <= PRESENTS; i++)
        {
            presentBag.add(i);
        }

        usingPrinter = true;
        try {
            printer = new PrintWriter(new File("./out1.txt"));
        }
        catch (Exception e) {
            usingPrinter = false;
        }
        printListLock = new ReentrantLock();
        processed = new HashSet<>();
    }

    // Begin thread processes.
    public void beginServants(ExecutorService servants, int numServants) {
        long start = System.nanoTime();
        for (int i = 1; i <= numServants; i++) {
            servants.submit(new ServantsTask());
        }
        long end = System.nanoTime();
        System.out.println("Problem 1 finished in " + ((end - start)) + " nanoseconds.");
    }

    // todo delete
    private synchronized void addGuy(int guy) {
        processed.add(guy);
    }

    // Get a present id from the bag.
    public synchronized int getPresentToAdd() {
        Random rand = new Random();
        int idx = rand.nextInt(presentBag.size());
        int retVal = presentBag.remove(idx);
        return retVal;
    }

    // Ensure that pred and cur exist, and that pred in fact points to cur.
    private boolean validate(PNode pred, PNode cur) {
        return !pred.isDeleted && !cur.isDeleted && pred.next == cur;
    }

    // Add a present to the list of presents to be processed.
    public boolean add(int presId) {
        long tId = Thread.currentThread().getId();
        if (DEBUGGING) System.out.println("Thread " + tId + " trying to add " + presId);
        head.lock();
        // if (DEBUGGING)
            // System.out.println("Thread " + tId + " locked " + head.id);
        PNode pred = head;
        try {
            PNode cur = pred.next;
            cur.lock();
            // if (DEBUGGING)
                // System.out.println("Thread " + tId + " locked " + cur.id);
            try {
                while (cur.id < presId) {
                    pred.unlock();
                    // if (DEBUGGING)
                        // System.out.println("Thread " + tId + " unlocked " + pred.id);
                    pred = cur;
                    cur = cur.next;
                    cur.lock();
                    // if (DEBUGGING)
                        // System.out.println("Thread " + tId + " locked " + cur.id);
                }
                if (cur.id == presId) {
                    if (DEBUGGING) System.out.println("\tAttempt to add " + presId + " failed");
                    return false;
                }
                PNode newNode = new PNode(presId, cur);
                pred.next = newNode;
                if (DEBUGGING) System.out.println("\tAttempt to add " + presId + " succeeded");
                return true;
            }
            finally {
                cur.unlock();
                // if (DEBUGGING)
                    // System.out.println("Thread " + tId + " unlocked " + cur.id);
            }
        }
        finally {
            pred.unlock();
            // if (DEBUGGING)
                // System.out.println("Thread " + tId + " unlocked " + pred.id);
        }
    }

    // Add a present to the list of presents to be processed.
    // public boolean add(int presId) {
    //     if (DEBUGGING) System.out.println("Attempting to add " + presId);
    //     while (true) {
    //         PNode pred = head;
    //         PNode cur = head.next;
    //         while (cur.id < presId) {
    //             pred = cur; 
    //             cur = cur.next;
    //         }
    //         pred.lock();
    //         try {
    //             cur.lock();
    //             try {
    //                 if (validate(pred, cur)) {
    //                     if (DEBUGGING) System.out.println("\tAttempt to add " + presId + " failed");
    //                     return false;
    //                 }
    //                 else {
    //                     PNode node = new PNode(presId, null);
    //                     node.next = cur;
    //                     pred.next = node;
    //                     if (DEBUGGING) System.out.println("\tAttempt to add " + presId + " succeeded");
    //                     return true;
    //                 }
    //             }
    //             finally {
    //                 cur.unlock();
    //             }
    //         }
    //         finally {
    //             pred.unlock();
    //         }
    //     }
    // }

    // Removes the present from the list. Effectively the same as writing
    // the thank you note.
    public synchronized boolean remove(int presId) {
        long tId = Thread.currentThread().getId();
        if (DEBUGGING) System.out.println("Thread " + tId + " trying to remove " + presId);
        PNode pred = null;
        PNode cur = null;
        head.lock();
        // if (DEBUGGING) System.out.println("Thread " + tId + " locked " + head.id);
        try {
            pred = head;
            cur = pred.next;
            cur.lock();
            // if (DEBUGGING) System.out.println("Thread " + tId + " locked " + cur.id);
            try {
                while (cur.id < presId) {
                    pred.unlock();
                    // if (DEBUGGING)
                        // System.out.println("Thread " + tId + " unlocked " + pred.id);
                    pred = cur;
                    cur = cur.next;
                    cur.lock();
                    // if (DEBUGGING) 
                        // System.out.println("Thread " + tId + " locked " + cur.id);
                }
                if (cur.id == presId) {
                    pred.next = cur.next;
                    if (DEBUGGING) System.out.println("\tAttempt to remove " + presId + " succeeded");
                    return true;
                }
                if (DEBUGGING) System.out.println("\tAttempt to remove " + presId + " failed");
                return false;
            }
            finally {
                cur.unlock();
                // if (DEBUGGING)
                    // System.out.println("Thread " + tId + " unlocked " + cur.id);
            }
        }
        finally {
            pred.unlock();
            // if (DEBUGGING)
                // System.out.println("Thread " + tId + " unlocked " + pred.id);
        }
    }

    // public boolean remove(int presId) {
    //     if (DEBUGGING) System.out.println("Attempting to remove " + presId);
    //     while (true) {
    //         PNode pred = head;
    //         PNode cur = head.next;
    //         while (cur.id < presId) {
    //             pred = cur;
    //             cur = cur.next;
    //         }
    //         pred.lock();
    //         try {
    //             cur.lock();
    //             try {
    //                 if (validate(pred, cur)) {
    //                     if (cur.id != presId) {
    //                         if (DEBUGGING) System.out.println("\tAttempt to remove " + presId + " failed");
    //                         return false;
    //                     }
    //                     else {
    //                         cur.isDeleted = true;
    //                         pred.next = cur.next;
    //                         if (DEBUGGING) System.out.println("\tAttempt to remove " + presId + " succeeded");
    //                         return true;
    //                     }
    //                 }
    //             }
    //             finally {
    //                 cur.unlock();
    //             }
    //         }
    //         finally {
    //             pred.unlock();
    //         }
    //     }
    // }

    // For debugging.
    private void printList() {
        printListLock.lock();
        PNode cur = head;
        System.out.println("Present List:");
        while (cur != null) {
            System.out.print(cur.id + " --> ");
            cur = cur.next;
        }
        System.out.println();
        printListLock.unlock();
    }

    // Node representing a single present.
    private class PNode
    {
        // Fields:
        public int id;
        public PNode next;
        public boolean isDeleted;
        public ReentrantLock lock;
        // note: high amount of add() and remove(), lots of contention
        // note: no contains() because thread can remove present right after adding it

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
}
