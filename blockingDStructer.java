import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class blockingDStructer {

    public BlockingQueue BlockingQueue = null;
    public BlockingArr BlockingArr = null;

    public blockingDStructer(int s, String type) throws unknownDStructerException { //call the queue

        if (type.equals("Queue")) {
            this.BlockingQueue = new BlockingQueue(s);
        } else if (type.equals("Array")) {
            this.BlockingArr = new BlockingArr(s);
        } else {
            throw new unknownDStructerException("EROR");
        }
    }

    class unknownDStructerException extends Exception {

        public unknownDStructerException(String str) {
            super(str);
        }

    }

    class BlockingQueue {//for the tasks

    	private Semaphore mutex;
        private Semaphore empty;
        private Semaphore full;
        private Lock lock;

        private int count;
        private final int size;
        private ArrayList<Task> list;

        public BlockingQueue(int S) {
            this.size = S;
            this.count = 0;
            this.list = new ArrayList<Task>(size);

            //Semaphores:
            this.mutex = new Semaphore(1, true);
            this.empty = new Semaphore(size, true);
            this.full = new Semaphore(0, true);

            this.lock = new ReentrantLock(true);
        }

        public void put(Task item) throws InterruptedException {
            empty.acquire();
            mutex.acquire();
            insertItem(item);
            mutex.release();
            full.release();
        }

        public Task take() throws InterruptedException {
            full.acquire();
            mutex.acquire();
            Task item = getItem();
            mutex.release();
            empty.release();
            return item;
        }

        public Task peek() {
            if (list.isEmpty()) {
                return null;
            } else {
                return list.get(0);
            }
        }

        public int getSize() {
            return size;
        }

        private void insertItem(Task item) {
            list.add(item);
            count++;
        }

        private Task getItem() {
            count--;
            return list.remove(0);
        }

        public int getCount() {
            return count;
        }

        public boolean isFull() {
            return count == size;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        public int getY(int x) {
            for (int i = 0; i < count; i++) {
                if (list.get(i).getX() == x) {
                    return list.get(i).getY();
                }
            }
            return -1;
        }

        public int getIndex(int x) {
            for (int i = 0; i < count; i++) {
                if (list.get(i).getX() == x) {
                    return i;
                }
            }
            return -1;
        }
    }

    class BlockingArr {

        private int count;
        private int size;
        private Task taskArr[];

        private Semaphore mutexArr[];
        private Semaphore takeArr[];
        private Semaphore putArr[];

        public BlockingArr(int size) {
            this.taskArr = new Task[size];
            this.size = size;
            this.count = 0;

            this.mutexArr = new Semaphore[size];
            for (int i = 0; i < size; i++) {
                mutexArr[i] = new Semaphore(1, true);
            }

            this.takeArr = new Semaphore[size];
            for (int i = 0; i < size; i++) {
                takeArr[i] = new Semaphore(1, true);
            }

            this.putArr = new Semaphore[size];
            for (int i = 0; i < size; i++) {
                putArr[i] = new Semaphore(0, true);
            }
        }

        public void put(Task item, int i) throws InterruptedException {
            takeArr[i].acquire();
            mutexArr[i].acquire();
            insertItem(item, i);
            mutexArr[i].release();
            putArr[i].release();
        }

        public Task take(int i) throws InterruptedException {
            putArr[i].acquire();
            mutexArr[i].acquire();
            Task item = getItem(i);
            mutexArr[i].release();
            takeArr[i].release();
            return item;
        }

        public Task peek(int i) {
            if (taskArr[i] == null) {
                return null;
            }
            return taskArr[i];
        }

        private void insertItem(Task item, int i) {
            taskArr[i] = item;
            count++;
        }

        private Task getItem(int i) {
            count--;
            Task ans = taskArr[i];
            taskArr[i] = null;
            return ans;
        }

        Task[] getArr() {
            return taskArr;
        }

        public int getCount() {
            return count;
        }

        public int getSize() {
            return size;
        }

    }
}
