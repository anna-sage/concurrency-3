// List of presents to be accessed concurrently by the minotaur's servants.

import java.util.ArrayList;
import java.util.Random;

public class PresentList
{
    // Tracker nodes.
    private PNode head; // Points to sentinel.
    private PNode tail;

    // Amount of presents.
    public static final int PRESENTS = 500000;

    // Bag of presents.
    private ArrayList<Integer> presentBag;
    private ArrayList<Integer> toRemove;

    // todo delete
    private final boolean DEBUGGING = true;

    public PresentList() 
    {
        // Generate first sentinel node (does not represent a present in the list).
        head = new PNode(PRESENTS + 1, null);
        tail = head;

        // Fill the present bag with all the presents.
        presentBag = new ArrayList<>();
        for (int i = 0; i < PRESENTS; i++)
        {
            presentBag.add(i);
        }

        toRemove = new ArrayList<>();
    }

    // Get a present id from the bag.
    public synchronized int getPresentToAdd()
    {
        Random rand = new Random();
        int idx = rand.nextInt(presentBag.size());
        int retVal = presentBag.remove(idx);
        if (DEBUGGING) System.out.println("idx: " + idx + ", retVal: " + retVal);
        return retVal;
    }

    // public synchronized int getPresentToRemove()
    // {

    // }

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
