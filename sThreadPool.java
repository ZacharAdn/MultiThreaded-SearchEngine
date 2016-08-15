import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

class sThreadPool {//thread pool for the S_Threds

	private blockingDStructer taskQueue = null;//for clients tasks
	private ArrayList<S_Thread> threads = null;
	private requestThread requests = null;//thread to check if client request to download

	private blockingDStructer reqCache = null;//queue for req from thread
	private blockingDStructer ansFromDB = null;
	private rThreadPool readersPool = null;
	private final int rangeToChoose;//range to answer if no ans in DB and in cache

	private HashMap<Integer, Task> updateList = null;
	private Semaphore executeMutex = null;
	private Semaphore lock = null;
	private Semaphore updatelock = null;

	/**
	 * make S sThreads.
	 *
	 * @param size the S
	 * @param clientList ArrayList of clients
	 * @param reqCache BlockingArr
	 * @param ansFromDB BlockingArr
	 * @param readersPool the readers ThreadPool
	 * @param rangeToChoose the L
	 * @param update hashMap for the updates to DB
	 * @param lock synchronized between the chooseAns and the write to the DB
	 * @param updatelock synchronized between the putInUpdate and the write to the DB
	 * @throws blockingDStructer.unknownDStructerException
	 */
	public sThreadPool(int size, ArrayList<clientInfo> clientList, blockingDStructer reqCache, blockingDStructer ansFromDB,
			rThreadPool readersPool, int rangeToChoose, HashMap update, Semaphore lock, Semaphore updatelock) throws blockingDStructer.unknownDStructerException {//, int maxNoOfTasks) {

		//clients connect and request:
		this.taskQueue = new blockingDStructer(size, "Queue");
		this.threads = new ArrayList<S_Thread>();
		this.requests = new requestThread(clientList);
		this.requests.start();

		this.reqCache = reqCache;   //requests and answers from cache
		this.ansFromDB = ansFromDB;        //requests and answers from DB
		this.readersPool = readersPool;

		this.executeMutex = new Semaphore(1, true);
		this.lock = lock;
		this.updatelock = updatelock;

		for (int i = 0; i < size; i++) {
			threads.add(new S_Thread(i, clientList, "sThread " + i));//taskQueue, reqCacheQueue, ansCacheQueue, rangeToChoose));
		}
		for (S_Thread thread : threads) {
			thread.start();
		}

		this.rangeToChoose = rangeToChoose;
		this.updateList = update;

	}

	/**
	 * call to sTread and give him mission to find.
	 *
	 * @param item Task
	 * @throws InterruptedException
	 */
	public void execute(Task item) throws InterruptedException {
		executeMutex.acquire();

		this.taskQueue.BlockingQueue.put(item);

		executeMutex.release();
	}

	public ArrayList<S_Thread> getThreads() {
		return threads;
	}

	/**
	 * Thread that iterate the clientList and check if it is request from him.
	 */
	class requestThread implements Runnable {

		private final Thread request;
		private ArrayList<clientInfo> clientList = null;

		public requestThread(ArrayList<clientInfo> clientList) {
			request = new Thread(this, "reqThread");
			this.clientList = clientList;
		}

		public void start() {
			request.start();
		}

		public void run() {
			Task item;
			System.out.print("");
			while (true) {
				System.out.print("");
				if (clientList.size() > 0) {
					System.out.print("");
					for (int i = 0; i < clientList.size(); i++) {
						try {
							if (clientList.get(i) != null) {
								if ((item = clientList.get(i).checkRequest()) != null) {// if its request in the i client-> execute
									execute(item);//put item in the blockingQueue from the threadPool
								}
							}
						} catch (IOException ex) {
							Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
							Logger.getLogger(sThreadPool.class.getName()).log(Level.SEVERE, null, ex);
						} catch (InterruptedException ex) {
							Logger.getLogger(sThreadPool.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * search Thread, started on the S ThreadPool
	 */
	class S_Thread extends Thread {

		private ArrayList<clientInfo> clientList = null;
		private final int Sid;

		/**
		 *
		 * @param threadNum the S
		 * @param clientList ArrayList of clients
		 * @param name
		 */
		public S_Thread(int threadNum, ArrayList<clientInfo> clientList, String name) {
			super(name);
			this.Sid = threadNum;
			this.clientList = clientList;
		}

		/**
		 * start the search mission put the answer in update list to DB and
		 * return to the client
		 */
		@Override
		public void run() {
			Task item, ans;
			while (true) {
				try {
					item = taskQueue.BlockingQueue.take();
					item.setsId(Sid);

					ans = searchInCache(item);
					if (ans == null) {
						ans = searchInDB(item);
						if (ans == null) {
							ans = chooseAns(item);
						}
					}

					putInUpdateList(ans);

					for (int i = 0; i < clientList.size(); i++) {//sends answer to the client
						if (clientList.get(i).getClientId() == ans.getClientId()) {
							clientList.get(i).ansToClient(ans);
						}
					}

				} catch (InterruptedException ex) {
					Logger.getLogger(S_Thread.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		public int getIdThread() {
			return Sid;
		}

		private Task searchInCache(Task item) throws InterruptedException {
			reqCache.BlockingArr.put(item, Sid);

			Task ans = reqCache.BlockingArr.take(Sid);

			if (ans.getY() == 0) {
				return null;
			} else {
				item.setY(ans.getY());
				item.setZ(1);
				return item;
			}
		}

		private Task searchInDB(Task item) throws InterruptedException {
			readersPool.execute(item);
			Task ans = ansFromDB.BlockingArr.take(Sid);
			if (ans.getY() == 0) {
				return null;
			}
			item.setY(ans.getY());
			item.setZ(1);
			return item;
		}

		private Task chooseAns(Task item) throws InterruptedException {
			Task tmp;
			lock.acquire();
			tmp = updateList.get(item.getX());
			if (tmp != null) {
				item.setY(tmp.getY());
				item.setZ(1);
				lock.release();
				return item;
			} else {
				int rand = (int) (Math.random() * rangeToChoose) + 1;
				item.setY(rand);
				item.setZ(1);
				lock.release();
				return item;
			}
		}

		private void putInUpdateList(Task item) throws InterruptedException {
			updatelock.acquire();
			Task tmp = null;
			tmp = updateList.remove(item.getX());
			if (tmp == null) {
				updateList.put(item.getX(), item);
			} else {
				tmp.setZ(tmp.getZ() + 1);
				updateList.put(tmp.getX(), tmp);
			}
			updatelock.release();
		}
	}
}