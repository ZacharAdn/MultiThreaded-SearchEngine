import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server Class
 *
 * @author Zahar Adiniaev, Yosi Gavriel, Aviya Pinhas
 */
public class Server implements Runnable {

	private ServerSocket serverSocket = null;
	private int serverPort;
	private Thread runningThread = null;

	private ArrayList<clientInfo> clientList = null;
	private int uniqeId;

	private rThreadPool readersPool = null;
	private sThreadPool searchPool = null;
	private Cache cache = null;
	private blockingDStructer reqCache = null;
	private DataBase database = null;
	private blockingDStructer arrAnsFromDB = null;
	private Semaphore lock = null;
	private Semaphore updatelock = null;
	private HashMap<Integer, Task> updateList = null;

	/**
	 *
	 * @param S number of allows search thread
	 * @param C size of the cache
	 * @param M the number of times to request before enter the cache
	 * @param L the range [1,L] is the not given replies
	 * @param Y number of reader threads
	 * @throws blockingDStructer.unknownDStructerException
	 */
	public Server(int S, int C, int M, int L, int Y) throws blockingDStructer.unknownDStructerException {

		this.serverPort = 55000;
		this.runningThread = new Thread(this, "ServerThread");//the Server thread
		this.runningThread.start();

		this.clientList = new ArrayList<clientInfo>();
		this.uniqeId = 0;
		this.updateList = new HashMap<Integer, Task>();

		this.reqCache = new blockingDStructer(S, "Array");

		this.cache = new Cache(C, S, reqCache);
		this.database = new DataBase(Y, M, cache);

		this.arrAnsFromDB = new blockingDStructer(S, "Array");
		this.lock = new Semaphore(1, true);
		this.updatelock = new Semaphore(1, true);
		this.readersPool = new rThreadPool(Y, database, arrAnsFromDB, updateList, lock, updatelock);
		this.searchPool = new sThreadPool(S, clientList, reqCache, arrAnsFromDB, readersPool, L, updateList, lock, updatelock);

	}

	/**
	 * make new ServerSocket that waits until client connected. establish the
	 * connection when they do so, and wait until became client ID. make new
	 * clientInfo and add hem to ArrayList of clients
	 */
	public void run() {
		this.runningThread = Thread.currentThread();
		System.out.println("Server Conected\nWaiting for clients to connect\n");
		try {
			this.serverSocket = new ServerSocket(this.serverPort);

			while (true) {
				Socket clientSocket = serverSocket.accept();//wait until client connect

				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String msg;
				while ((msg = in.readLine()) != null) {
					if (msg.contains("Connected:")) {
						System.out.println(msg);
						msg = msg.substring(17);
						uniqeId = Integer.parseInt(msg);
						break;
					}
				}
				clientInfo newClient = new clientInfo(clientSocket, in, out, uniqeId);
				clientList.add(newClient);
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void main(String[] args) throws blockingDStructer.unknownDStructerException {
		int S = Integer.parseInt(args[0]);
		int C = Integer.parseInt(args[1]);
		int M = Integer.parseInt(args[2]);
		int L = Integer.parseInt(args[3]);
		int Y = Integer.parseInt(args[4]);

		Server server = new Server(S, C, M, L, Y);
	}
}
