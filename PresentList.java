// List of presents to be accessed concurrently by the minotaur's servants.

public class PresentList
{
    // Fields:

    // Tracker nodes.
    private PNode head; // Points to sentinel.
    private PNode tail;

    // Amount of presents.
    public static final int PRESENTS = 500000;

    public PresentList() 
    {
        // Generate first sentinel node (does not represent a present in the list).
        head = new PNode(PRESENTS + 1, null);
        tail = head;
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
