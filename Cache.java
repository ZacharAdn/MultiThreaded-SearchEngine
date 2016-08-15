import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

class Cache {

    private HashMap<Integer, Task> hashCache = null;
    private ArrayList<cThread> cThreads = null;
    private final int cSize;
    private Semaphore mutex = null;
    private Semaphore priority = null;
    private Semaphore csh = null;
    private Semaphore delay = null;

    private int countInCache, updated;
    private boolean firstIter = true;
    private blockingDStructer reqCache = null;

    /**
     * make S cThread.
     *
     * @param cSize the C
     * @param sSize the S
     * @param reqCache blockingArr to requests and answers to the S thread
     */
    public Cache(int cSize, int sSize, blockingDStructer reqCache) {
        this.hashCache = new HashMap<Integer, Task>(cSize);
        this.reqCache = reqCache;
        this.cSize = cSize;

        this.mutex = new Semaphore(1, true);
        this.priority = new Semaphore(1, true);
        this.csh = new Semaphore(1, true);
        this.delay = new Semaphore(1, true);

        this.countInCache = 0;
        this.updated = 0;

        this.cThreads = new ArrayList<cThread>();

        for (int i = 0; i < sSize; i++) {
            cThreads.add(new cThread(i));
        }
        for (int i = 0; i < sSize; i++) {
            cThreads.get(i).start();
        }
    }

    /**
     * call to the writerToCache
     *
     * @param updateCache
     * @param minInCache the minimum Z in Cache
     * @param maxInUpdate
     * @return the minimum z in the cache
     * @throws InterruptedException
     */
    public Task add(HashMap updateCache, Task minInCache, Task maxInUpdate) throws InterruptedException {
        delay.acquire();
        csh.acquire();

        minInCache = writerToCache(updateCache, minInCache, maxInUpdate);

        updated = 0;
        updateCache.clear();

        csh.release();
        delay.release();
        return minInCache;
    }

    /**
     * swap between the minimum in cache and maximum in update cache makes 2
     * praiorityQueues that keeps in the head the minimum/maximum
     *
     * @param updateCache
     * @param minInCache
     * @param maxInUpdate
     * @return the minimum z in the cache
     */
    public Task writerToCache(HashMap updateCache, Task minInCache, Task maxInUpdate) {
        Task min = new Task(minInCache), max = new Task(maxInUpdate);
        if (firstIter) {
            firstIter = false;
            updated = cSize;
            hashCache.putAll(updateCache);
            return min;
        } else if (min.getZ() < max.getZ()) {
            if (hashCache.containsKey(max.getX())) {
                hashCache.remove(max.getX());
                hashCache.put(max.getX(), max);
                updateCache.remove(max.getX(), max);
            } else {
                hashCache.remove(min.getX(), min);
                hashCache.put(max.getX(), max);
                updateCache.remove(max.getX(), max);
                updated++;

                PriorityQueue<Task> cacheQueue = new PriorityQueue(hashCache.values());
                PriorityQueue<Task> updateQueue = new PriorityQueue(updateCache.size(), Collections.reverseOrder());
                updateQueue.addAll(updateCache.values());

                while (!cacheQueue.isEmpty()) {
                    min = cacheQueue.poll();
                    max = updateQueue.poll();
                    if (max == null) {
                        break;
                    }
                    if (min.getZ() < max.getZ()) {
                        hashCache.remove(min.getX(), min);
                        hashCache.put(max.getX(), max);
                        updateCache.remove(max.getX(), max);
                        updated++;
                    } else {
                        break;
                    }
                }
            }
        }
        return min;
    }

    private void swap(Task min1, Task min2) {
        Task tmp = min1;
        min1 = min2;
        min2 = tmp;
    }

    /**
     *
     * @param x
     * @return the answer from cache
     * @throws InterruptedException
     */
    public Task takeAns(Task x) throws InterruptedException {
        Task ans = new Task();

        priority.acquire();
        delay.acquire();
        mutex.acquire();
        countInCache++;
        if (countInCache == 1) {
            csh.acquire();
        }
        mutex.release();
        delay.release();
        priority.release();

        ans = hashCache.get(x.getX());

        mutex.acquire();
        countInCache--;
        if (countInCache == 0) {
            csh.release();
        }
        mutex.release();
        return ans;
    }

    public void remove(Task x) {
        hashCache.remove(x.getX());
    }

    public int getCount() {
        return hashCache.size();
    }

    public int getSize() {
        return cSize;
    }
    
    
    class cThread extends Thread {

    	private int id;

    	public cThread(int i) {
    		super("cThread " + i);

    		this.id = i;
    	}

    	/**
    	 * every c thread wait until its a Task in his cell search the Task in
    	 * cache and return answer to the sThread through the blockingArr
    	 */
    	@Override
    	public void run() {
    		while (true) {
    			try {
    				Task item = reqCache.BlockingArr.take(id);
    				Task ans = takeAns(item);

    				if (ans != null) {
    					reqCache.BlockingArr.put(ans, id);
    				} else {
    					reqCache.BlockingArr.put(item, id);
    				}
    			} catch (InterruptedException ex) {
    				Logger.getLogger(Cache.class.getName()).log(Level.SEVERE, null, ex);
    			}
    		}
    	}
    }

}