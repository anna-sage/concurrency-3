// List of presents to be accessed concurrently by the minotaur's servants.

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;

public class PresentList
{
    // Tracker nodes.
    private PNode head; // Points to sentinel.
    private PNode tail;
    private int size;

    // Amount of presents.
    public static final int PRESENTS = 500000;

    // Bag of presents.
    private ArrayList<Integer> presentBag;

    // todo delete
    private final boolean DEBUGGING = true;

    class ServantsTask implements Runnable
    {
        public void run()
        {
            int adding = getPresentToAdd();
        }
    }

    public PresentList() 
    {
        // Generate first sentinel node (does not represent a present in the list).
        head = new PNode(PRESENTS + 1, null);
        tail = head;
        size = 1;

        // Fill the present bag with all the presents.
        presentBag = new ArrayList<>();
        for (int i = 0; i < PRESENTS; i++)
        {
            presentBag.add(i);
        }
        if (DEBUGGING) System.out.println("bag size: " + presentBag.size());
    }

    // Begin thread processes.
    public void beginServants(ExecutorService servants) {
        if (DEBUGGING) System.out.println("made it here");
        while (presentBag.size() > 0) {
            if (DEBUGGING) System.out.println("about to submit a new task");
            servants.submit(new ServantsTask());
        }
    }

    // Get a present id from the bag.
    public synchronized int getPresentToAdd() {
        Random rand = new Random();
        int idx = rand.nextInt(presentBag.size());
        int retVal = presentBag.remove(idx);
        if (DEBUGGING) System.out.println("idx: " + idx + ", retVal: " + retVal);
        return retVal;
    }

    public boolean presentBagIsEmpty() {
        return presentBag.size() > 0;
    }

    // Node representing a single present.
    private class PNode
    {
        // Fields:
        public int id;
        public PNode next;
        public boolean isDeleted;
        // note: high amount of add() and remove(), lots of contention
        // note: no contains() because thread can remove present right after adding it

        public PNode(int presId, PNode nextNode)
        {
            id = presId;
            next = nextNode;
            isDeleted = false;
        }
    }
}
