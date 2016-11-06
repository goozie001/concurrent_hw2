import java.util.concurrent.locks.*;
import static java.lang.Thread.yield;


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
    private Condition insert;
    private Condition remove;
    private volatile int searchers;
    private boolean inserting;

    private static class Node<T>{
        final T item;
        Node<T> next;

        Node(T item, Node<T> next){
            this.item = item;
            this.next = next;
        }
    }

    private Node<T> first;

    public ConcurrentSearcherList() {
        first = null;
        searchLock = new ReentrantLock();
        insert = searchLock.newCondition();
        remove = searchLock.newCondition();
        searchers = 0;
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
        assert item != null: "Error in ConcurrentSearcherList insert:  Attempt to remove null";
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
            while (inserting)
                insert.await();
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
            insert.signal();
            if (searchers == 0)
                remove.signal();
        }
        finally {
            searchLock.unlock();
        }
    }

    private void start_search() throws InterruptedException{
        searchLock.lock();
        try {
            searchers += 1;
            if (!inserting)
                remove.signal();
        }
        finally {
            searchLock.unlock();
        }
    }

    private void end_search(){
        searchLock.lock();
        try {
            searchers -= 1;
            if (!inserting)
        }
    }

    private void start_remove() throws InterruptedException{
        // TTAS implementation to only ever have one remover
        while (nRemovers.get() == 1) {
            yield();
            if (nRemovers.compareAndSet(0, 1))
                break;
        }
        while
    }

    private void end_remove() {

    }
}