import java.util.concurrent.locks.*;


public class ConcurrentSearcherList<T> {

	/*
	 * Three kinds of threads share access to a singly-linked list:
	 * searchers, inserters and deleters. Searchers merely examine the list;
	 * hence they can execute concurrently with each other. Inserters add
	 * new items to the front of the list; insertions must be mutually exclusive
	 * to preclude two inserters from inserting new items at about
	 * the same time. However, one insert can proceed in parallel with
	 * any number of searches. Finally, deleters remove items from anywhere
	 * in the list. At most one deleter process can access the list at
	 * a time, and deletion must also be mutually exclusive with searches
	 * and insertions.
	 *
	 * Make sure that there are no data races between concurrent inserters and searchers!
	 */

    private Lock searchLock;
    private Condition insertCondition;
    private Condition removeCondition;
    private Condition searchCondition;
    private int searchers;
    private int removers;
    private boolean inserting;
    private boolean removing;

    private static class Node<T>{
        final T item;
        Node<T> next;

        Node(T item, Node<T> next){
            this.item = item;
            this.next = next;
        }
    }

    // Make volatile to remove race condition between insert and search
    private volatile Node<T> first;

    public ConcurrentSearcherList() {
        first = null;
        searchLock = new ReentrantLock();
        insertCondition = searchLock.newCondition();
        removeCondition = searchLock.newCondition();
        searchCondition = searchLock.newCondition();
        searchers = 0;
        removers = 0;
    }

    /**
     * Inserts the given item into the list.
     *
     * Precondition:  item != null
     *
     * @param item
     * @throws InterruptedException
     */
    public void insert(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert:  Attempt to insert null";
        start_insert();
        try{
            first = new Node<T>(item, first);
        }
        finally{
            end_insert();
        }
    }

    /**
     * Determines whether or not the given item is in the list
     *
     * Precondition:  item != null
     *
     * @param item
     * @return  true if item is in the list, false otherwise.
     * @throws InterruptedException
     */
    public boolean search(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert:  Attempt to search for null";
        start_search();
        try{
            for(Node<T> curr = first;  curr != null ; curr = curr.next){
                if (item.equals(curr.item)) return true;
            }
            return false;
        }
        finally{
            end_search();
        }
    }

    /**
     * Removes the given item from the list if it exists.  Otherwise the list is not modified.
     * The return value indicates whether or not the item was removed.
     *
     * Precondition:  item != null.
     *
     * @param item
     * @return  whether or not item was removed from the list.
     * @throws InterruptedException
     */
    public boolean remove(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert:  Attempt to removeCondition null";
        start_remove();
        try{
            if(first == null) return false;
            if (item.equals(first.item)){first = first.next; return true;}
            for(Node<T> curr = first;  curr.next != null ; curr = curr.next){
                if (item.equals(curr.next.item)) {
                    curr.next = curr.next.next;
                    return true;
                }
            }
            return false;
        }
        finally{
            end_remove();
        }
    }

    private void start_insert() throws InterruptedException{
        searchLock.lock();
        try {
            // Wait until there are no inserts or removes currently happening
            while (inserting || removing) {
                insertCondition.await();
            }
            // Now we set inserting to true for this thread
            inserting = true;
        }
        finally {
            searchLock.unlock();
        }
    }

    private void end_insert(){
        searchLock.lock();
        try {
            inserting = false;
            // Try to hand baton to removers waiting first, then to (potential) inserters waiting
            if (searchers == 0 && removers > 0) {
                removeCondition.signal();
            }
            else {
                insertCondition.signal();
            }
        }
        finally {
            searchLock.unlock();
        }
    }

    private void start_search() throws InterruptedException{
        searchLock.lock();
        try {
            searchers += 1;
            // wait until removing is done, then we can enter the search method
            while (removing)
                searchCondition.await();
        }
        finally {
            searchLock.unlock();
        }
    }

    private void end_search(){
        searchLock.lock();
        try {
            searchers -= 1;
            // If we are the last searcher, there is not an insert happening, and there is at least one
            // remover waiting, signal that remover to start
            if (searchers == 0 && !inserting && removers > 0) {
                removeCondition.signal();
            }
        }
        finally {
            searchLock.unlock();
        }
    }

    private void start_remove() throws InterruptedException{
        searchLock.lock();
        try {
            removers += 1;
            // Wait until there are no other operations to continue
            while (searchers > 0 || inserting || removing)
                removeCondition.await();

            removing = true;
        }
        finally {
            searchLock.unlock();
        }
    }

    private void end_remove() {
        searchLock.lock();
        try {
            removing = false;
            removers -= 1;
            // If we have other removes waiting, prioritize those before the search method get multiple threads
            // performing it again
            if (removers > 0) {
                removeCondition.signal();
            }
            else {
                // Signal all searches at once. They will all pass their conditions and search concurrently
                searchCondition.signalAll();
                insertCondition.signal(); // Only one insert that is potentially waiting should be signalled
            }
        }
        finally {
            searchLock.unlock();
        }
    }
}
