import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;


class rThreadPool {

    private ArrayList<R_Thread> threads = null;
    private blockingDStructer searchInDB = null;
    private DataBase database = null;
    private blockingDStructer ansFromDB = null;
    private int taskNum;
    private W_Thread writer = null;
    private HashMap<Integer, Task> updateList = null;

    private Semaphore db = null;
    private Semaphore delay = null;
    private Semaphore mutex = null;
    private Semaphore priority = null;

    private Semaphore lock = null;
    private Semaphore updateLock = null;

    /**
     * The readers ThreadPool, makes Y rThreads
     * @param size the Y
     * @param database 
     * @param ansFromDB blockingArr for the answers to sThread
     * @param updateList the updates to the DB
     * @param lock synchronized between the chooseAns and the write to the DB
     * @param updatelock synchronized between the putInUpdate and the write to the DB
     * @throws blockingDStructer.unknownDStructerException 
     */
    public rThreadPool(int size, DataBase database, blockingDStructer ansFromDB, HashMap updateList, Semaphore lock, Semaphore updateLock) throws blockingDStructer.unknownDStructerException {
        this.searchInDB = new blockingDStructer(size, "Queue");
        this.database = database;
        this.ansFromDB = ansFromDB;
        this.threads = new ArrayList<R_Thread>();
        this.taskNum = 0;
        this.updateList = updateList;

        this.mutex = new Semaphore(1, true);
        this.priority = new Semaphore(1, true);
        this.db = new Semaphore(1, true);
        this.delay = new Semaphore(1, true);

        this.lock = lock;
        this.updateLock = updateLock;

        this.writer = new W_Thread(updateList, database);
        writer.start();

        for (int i = 0; i < size; i++) {
            threads.add(new R_Thread(i, "rThread" + i));
        }

        for (R_Thread thread : threads) {
            thread.start();
        }
    }

    /**
     * put the Task request from the sThreads to the rThreads
     * @param item 
     */
    public void execute(Task item) {
        try {

            priority.acquire();
            delay.acquire();
            mutex.acquire();
            taskNum++;
            if (taskNum == 1) {
                db.acquire();
            }
            mutex.release();
            delay.release();
            priority.release();

            this.searchInDB.BlockingQueue.put(item);

            mutex.acquire();
            taskNum--;
            if (taskNum == 0) {
                db.release();
            }
            mutex.release();

        } catch (InterruptedException ex) {
            Logger.getLogger(sThreadPool.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<R_Thread> getThreads() {
        return threads;
    }

    /**
     * the Threads that allow to search in DB
     */
    class R_Thread extends Thread {

        int id;

        public R_Thread(int id, String name) {
            super(name);
            this.id = id;
        }

        @Override
        public void run() {
            Task item, ans;
            while (true) {
                try {
                    item = searchInDB.BlockingQueue.take();//take req from db manager
                    ans = database.readTask(item);//run the search on DB
                    ansFromDB.BlockingArr.put(item, item.getSid());
                } catch (InterruptedException ex) {
                    Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(rThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
	/**
	 * the thread that allow to write to DB
	 */
	class W_Thread implements Runnable {

		private DataBase database = null;
		private HashMap<Integer, Task> updateList = null;
		private int tasksInDB;
		private final Thread w;

		/**
		 * 
		 * @param update the Tasks from the sThreads that need to write or to update
		 * @param database 
		 */
		public W_Thread(HashMap update, DataBase database) {
			this.w = new Thread(this, "wThread");

			this.updateList = update;
			this.database = database;
			this.tasksInDB = 0;

		}

		public void start() {
			this.w.start();
		}

		/**
		 * the algorithm that control the writing times
		 */
		public void run() {
			while (true) {
				int needToWrite;
				System.out.print("");
				while (tasksInDB < 1000) {
					needToWrite = updateList.size();
					System.out.print("");
					if (needToWrite >= 100 && needToWrite <= 150) {
						System.out.print("");
						wantToWrite();
					}
				}
				while (tasksInDB >= 1000 && tasksInDB <= 5000) {
					needToWrite = updateList.size();
					System.out.print("");
					if (needToWrite >= 500 && needToWrite <= 700) {
						System.out.print("");
						wantToWrite();
					}
				}
				while (tasksInDB > 5000) {
					needToWrite = updateList.size();
					System.out.print("");
					if (needToWrite > 1000) {
						System.out.print("");
						wantToWrite();
					}
				}
			}
		}

		private void wantToWrite() {
			try {
				System.out.print("");
				delay.acquire();
				db.acquire();
				lock.acquire();
				updateLock.acquire();

				Iterator<Task> iter = updateList.values().iterator();
				while (iter.hasNext()) {
					tasksInDB++;
					Task item = iter.next();
					database.writeTask(item);
				}
				updateList.clear();

				updateLock.release();
				lock.release();
				db.release();
				delay.release();

			} catch (InterruptedException | IOException ex) {
				Logger.getLogger(W_Thread.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

}