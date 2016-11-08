/**
 * Created by Dylan Richardson on 11/6/16.
 *
 * This is a thread safety testing environment for the ConcurrentSearcherList class
 */
public class ConcurrentSearcherListTester {

    public ConcurrentSearcherListTester() {}

    class Searcher implements Runnable {

        private final ConcurrentSearcherList<Integer> list;
        private final long rate;
        private final int val;

        public Searcher(ConcurrentSearcherList<Integer> list, int val, long rate) {
            this.list = list;
            this.val = val;
            this.rate = rate;
        }
        @Override
        public void run() {
            for (int i = 0; i < 10; ++i)
            {
                try {
                    Thread.sleep(rate);
                    list.search(val);
                }
                catch (InterruptedException ie) {}
            }
        }
    }

    class Inserter implements Runnable {

        private final ConcurrentSearcherList<Integer> list;
        private final long rate;
        private final int val;

        public Inserter(ConcurrentSearcherList<Integer> list, int val, long rate) {
            this.list = list;
            this.val = val;
            this.rate = rate;
        }
        @Override
        public void run() {
            for (int i = 0; i < 10; ++i)
            {
                try {
                    Thread.sleep(rate);
                    list.insert(val);
                }
                catch (InterruptedException ie) {}
            }
        }
    }

    class Remover implements Runnable {

        private final ConcurrentSearcherList<Integer> list;
        private final long rate;
        private final int val;

        public Remover(ConcurrentSearcherList<Integer> list, int val, long rate) {
            this.list = list;
            this.val = val;
            this.rate = rate;
        }
        @Override
        public void run() {
            for (int i = 0; i < 10; ++i)
            {
                try {
                    Thread.sleep(rate);
                    list.remove(val);
                }
                catch (InterruptedException ie) {}
            }
        }
    }

    public void test() {

        ConcurrentSearcherList<Integer> list = new ConcurrentSearcherList<>();
        for (int i = 0; i < 100000; ++i)
            try {
                list.insert(i);
            }
            catch (InterruptedException ie) {}


        System.out.println("Flag");

        Thread[] listOps = new Thread[14];

        for (int i = 0; i < 10; ++i) {
            listOps[i] = new Thread(new Searcher(list, i + 90000, 100));
        }
        listOps[10] = new Thread(new Inserter(list, 0, 100));
        listOps[11] = new Thread(new Inserter(list, 1, 100));
        listOps[12] = new Thread(new Remover(list, 100000, 100));
        listOps[13] = new Thread(new Remover(list, 99990, 100));

        for (int i = 0; i < listOps.length; ++i)
            listOps[i].start();

        for (int i = 0; i < listOps.length; ++i) {
            try {
                listOps[i].join();
            }
            catch (InterruptedException ie) {}
        }
    }

    public static void main(String[] args) {

        ConcurrentSearcherListTester test = new ConcurrentSearcherListTester();
        test.test();
        System.out.println("Finished!");
    }

}
