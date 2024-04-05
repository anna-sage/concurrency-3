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

public class PresentList
{
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
    private static final boolean DEBUGGING = true;
    public HashSet<Integer> processed;

    // Task for servants to repeat.
    class ServantsTask implements Runnable
    {
        public void run()
        {
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
                    System.out.println("bad");
                    presentBag.add(pres);
                }

                if (presentBag.size() < 10000 && usingPrinter) printer.flush();
            }
        }
    }

    public PresentList() 
    {
        // Generate first sentinel node (does not represent a present in the list).
        tail = new PNode(PRESENTS + 2, null);
        head = new PNode(PRESENTS + 1, tail);

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
        // Set keeps track of which presents have been added and deleted.
        if (DEBUGGING) processed = new HashSet<>();
        servants = Executors.newFixedThreadPool(SERVANTS);
    }

    // Begin thread processes.
    public void beginServants() {
        for (int i = 1; i <= SERVANTS; i++) {
            servants.submit(new ServantsTask());
        }
    }

    // todo delete
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

    // Ensure that pred and cur exist, and that pred in fact points to cur.
    private boolean validate(PNode pred, PNode cur) {
        return !pred.isDeleted && !cur.isDeleted && pred.next == cur;
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

    // Node representing a single present.
    private class PNode
    {
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
            if (!p.servants.awaitTermination(60, TimeUnit.SECONDS))
                p.servants.shutdownNow();
        }
        catch (Exception e) {
            p.servants.shutdownNow();
        }
        long end = System.nanoTime();

        if (DEBUGGING) {
            // Verify that all presents were processed.
            System.out.println("Amount of presents processed: " + p.processed.size());
            if (p.processed.size() < PRESENTS) {
                for (int i = 1; i <= PRESENTS; i++) {
                    if (!p.processed.contains(i))
                        System.out.println(i + " was never processed.");
                }
            }
        }
    }
}
